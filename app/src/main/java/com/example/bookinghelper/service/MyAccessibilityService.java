package com.example.bookinghelper.service;

import android.accessibilityservice.AccessibilityService;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingDao;
import com.example.bookinghelper.data.BookingItem;
import com.example.bookinghelper.ui.MainActivity;
import com.example.bookinghelper.util.BookingState;
import com.example.bookinghelper.util.TemplateManager;
import com.example.bookinghelper.util.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "BookingPipeline";
    private final Pattern phonePattern = Pattern.compile("01[016789]-?\\d{3,4}-?\\d{4}");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isWaiting = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (!BookingState.isProcessing) return;

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!"com.naver.smartplace".equals(pkg)) return;

        if (isWaiting) return;
        isWaiting = true;

        Log.d(TAG, "[3-1] 스마트플레이스 화면 진입 확인 -> 1.5초 후 텍스트 스캔");
        handler.postDelayed(() -> tryParseAndSend(1), 1500);
    }

    private void tryParseAndSend(int attempt) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            retryOrFail(attempt);
            return;
        }

        List<String> texts = new ArrayList<>();
        collectTexts(rootNode, texts);

        Log.d(TAG, "[3-2] (" + attempt + "회차 스캔) 수집된 화면 텍스트 수: " + texts.size());

        // 뒷배경 예약 목록을 건너뛰고 "예약 상세정보" 모달 시작 위치 탐색
        int detailStartIndex = -1;
        for (int i = 0; i < texts.size(); i++) {
            if ("예약 상세정보".equals(texts.get(i))) {
                detailStartIndex = i;
                break;
            }
        }

        if (detailStartIndex == -1) {
            Log.d(TAG, "[대기] '예약 상세정보' 모달 렌더링 대기 중...");
            retryOrFail(attempt);
            return;
        }

        String targetName = BookingState.targetCustomerName;

        String customerName = null;
        String rawPhone = null;
        String itemName = null;
        String bookingDate = null;
        String userRequest = "";

        // "예약 상세정보" 모달 내부(detailStartIndex ~ 끝)만 순회
        for (int i = detailStartIndex; i < texts.size(); i++) {
            String current = texts.get(i);

            // 1. 예약자 (모달 내 '예약자관리' 아래 '예약자' 라벨 다음 텍스트)
            if (customerName == null && "예약자".equals(current) && i + 1 < texts.size()) {
                String candidateName = texts.get(i + 1).replace("님", "").trim();
                if (targetName == null || targetName.isEmpty() || candidateName.equals(targetName)) {
                    customerName = candidateName;
                }
            }

            // 2. 전화번호 (ContentDescription과 Text가 연속으로 오므로 패턴 일치하는 항목 추출)
            if (rawPhone == null && "전화번호".equals(current) && i + 1 < texts.size()) {
                String candidate = texts.get(i + 1).replace(" ", "");
                if (phonePattern.matcher(candidate).matches()) {
                    rawPhone = candidate.replace("-", "");
                }
            }

            // 3. 상품 (예약내역 -> 상품 다음 텍스트)
            if (itemName == null && "상품".equals(current) && i + 1 < texts.size()) {
                itemName = texts.get(i + 1).trim();
            }

            // 4. 이용일시 (이용일시 다음 텍스트)
            if (bookingDate == null && "이용일시".equals(current) && i + 1 < texts.size()) {
                bookingDate = texts.get(i + 1).trim();
            }

            // 5. 요청사항 (고객이 작성한 경우에만 모달에 라벨이 나타나며, 고정 라벨이 아닐 때만 저장)
            if (userRequest.isEmpty() && "요청사항".equals(current) && i + 1 < texts.size()) {
                String req = texts.get(i + 1).trim();
                if (!req.equals("유입경로") && !req.equals("결제예상정보") && !req.equals("직원메모") && !req.equals("-")) {
                    userRequest = req;
                }
            }
        }

        // 식별 완료 시
        if (rawPhone != null && customerName != null) {
            BookingState.isProcessing = false;
            isWaiting = false;

            if (itemName == null) itemName = "무료상담 및 이용안내";
            if (bookingDate == null) bookingDate = "";

            String branch = BookingState.detectedBranch;
            String subject = classifySubject(itemName);

            if ((subject.equals("보컬") || subject.equals("드럼")) && !branch.contains("보컬")) {
                branch = "어썸보컬드럼 여의도점";
            }

            Log.d(TAG, "===============================================");
            Log.d(TAG, "[성공] '예약 상세정보' 모달 정보 추출 완료!");
            Log.d(TAG, "지점: " + branch + " | 과목: " + subject);
            Log.d(TAG, "고객명: " + customerName + " | 번호: " + rawPhone);
            Log.d(TAG, "일시: " + bookingDate + " | 요청: " + (userRequest.isEmpty() ? "(없음)" : userRequest));
            Log.d(TAG, "===============================================");

            // 주소록 저장
            String contactName = TemplateManager.buildContactName(getApplicationContext(), customerName, subject, branch);
            saveContact(contactName, rawPhone);

            // DB 작업 및 발송 분기
            processBookingDecision(branch, subject, customerName, rawPhone, bookingDate, itemName, userRequest);

            // 모든 모드 공통: 스마트플레이스 상세 화면 닫고 어썸 예약 비서 앱 전면 복귀
            performGlobalAction(GLOBAL_ACTION_BACK);
            Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
            returnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(returnIntent);
            Log.d(TAG, "[5] 상세 화면 종료 및 어썸 예약 비서 앱 복귀");

        } else {
            Log.d(TAG, "[재시도] 상세 정보 하위 텍스트 로딩 대기 중...");
            retryOrFail(attempt);
        }
    }

    private String classifySubject(String itemName) {
        if (itemName.contains("베이스")) return "베이스";
        if (itemName.contains("기타")) return "기타";
        if (itemName.contains("보컬")) return "보컬";
        if (itemName.contains("드럼")) return "드럼";
        return "상담";
    }

    private void processBookingDecision(String branch, String subject, String name,
                                        String phone, String date, String itemName, String req) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            BookingDao dao = db.bookingDao();

            long startToday = TimeUtils.getStartOfToday();
            long endToday = TimeUtils.getEndOfToday();

            int todayCount = dao.countBookingsTodayByPhone(phone, startToday, endToday);
            String message = TemplateManager.buildMessage(getApplicationContext(), name, subject, date);

            BookingItem item = new BookingItem();
            item.branch = branch;
            item.subject = subject;
            item.customerName = name;
            item.phone = phone;
            item.bookingDate = date;
            item.itemName = itemName;
            item.userRequest = req;
            item.messageBody = message;
            item.createdAt = System.currentTimeMillis();

            if (todayCount > 0) {
                Log.w(TAG, "[당일 중복/변경 감지] 발송 차단 -> DUPLICATE 큐로 보관 (" + phone + ")");
                item.status = "DUPLICATE";
                item.modeTag = "[중복/변경]";
                item.sentAt = 0;
                dao.insert(item);

            } else if (BookingState.MODE_AUTO.equals(BookingState.currentMode)) {
                sendCustomSms(phone, message);
                item.status = "SENT";
                item.modeTag = "[자동]";
                item.sentAt = System.currentTimeMillis();
                dao.insert(item);
                Log.d(TAG, "[자동 모드] 문자 발송 및 SENT 이력 저장");

            } else {
                item.status = "PENDING";
                item.modeTag = "[수동]";
                item.sentAt = 0;
                dao.insert(item);
                Log.d(TAG, "[수동 모드] PENDING 대기 목록에 저장 (발송 대기)");
            }
        });
    }

    private void retryOrFail(int attempt) {
        if (attempt < 5 && BookingState.isProcessing) {
            Log.d(TAG, "[재시도] 1초 후 재스캔 (" + (attempt + 1) + "/5)");
            handler.postDelayed(() -> tryParseAndSend(attempt + 1), 1000);
        } else {
            Log.w(TAG, "[실패] 최종 탐색 실패: 예약 정보를 추출하지 못했습니다.");
            BookingState.isProcessing = false;
            isWaiting = false;
        }
    }

    private void collectTexts(AccessibilityNodeInfo node, List<String> list) {
        if (node == null) return;
        if (node.getText() != null) {
            String t = node.getText().toString().trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        if (node.getContentDescription() != null) {
            String cd = node.getContentDescription().toString().trim();
            if (!cd.isEmpty() && (node.getText() == null || !cd.equals(node.getText().toString().trim()))) {
                list.add(cd);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectTexts(node.getChild(i), list);
        }
    }

    private void saveContact(String displayName, String phoneNumber) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int rawIndex = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Log.d(TAG, "주소록 저장 완료: " + displayName);
        } catch (Exception e) {
            Log.e(TAG, "주소록 저장 에러", e);
        }
    }

    private void sendCustomSms(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
            Log.d(TAG, "안내 문자 발송 완료 -> " + phone);
        } catch (Exception e) {
            Log.e(TAG, "문자 전송 에러", e);
        }
    }

    @Override
    public void onInterrupt() {}
}