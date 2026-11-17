package com.example.bookinghelper.service;

import android.app.PendingIntent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.example.bookinghelper.util.BookingState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyNotificationListener extends NotificationListenerService {

    private static final String TAG = "BookingPipeline";
    private final Pattern namePattern = Pattern.compile("^([가-힣a-zA-Z0-9]+)님");

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        String packageName = sbn.getPackageName();
        String title = extras.getString("android.title", "");
        CharSequence charSequenceText = extras.getCharSequence("android.text");
        String text = charSequenceText != null ? charSequenceText.toString() : "";

        boolean isSmartPlace = "com.naver.smartplace".equals(packageName) &&
                (text.contains("새로운 예약이 확정") || title.contains("예약확정"));

        if (isSmartPlace) {
            Log.d(TAG, "===============================================");
            Log.d(TAG, "[1] 스마트플레이스 예약 알림 수신");
            Log.d(TAG, "제목: " + title + " | 내용: " + text);
            Log.d(TAG, "===============================================");

            // 1. 지점 구분
            String branch = "어썸기타학원 여의도점";
            if (title.contains("보컬") || text.contains("보컬") || title.contains("드럼") || text.contains("드럼")) {
                branch = "어썸보컬드럼 여의도점";
            }
            BookingState.detectedBranch = branch;

            // 2. 예약자 이름 1차 추출
            String targetName = "";
            Matcher nameMatcher = namePattern.matcher(text.trim());
            if (nameMatcher.find()) {
                targetName = nameMatcher.group(1);
            }
            BookingState.targetCustomerName = targetName;

            Log.d(TAG, "추출 지점: " + branch + " | 타깃 고객명: " + targetName);
            Log.d(TAG, "===============================================");

            // 3. 처리 플래그 켜기
            BookingState.isProcessing = true;

            // 4. 상세 페이지 열기
            PendingIntent contentIntent = sbn.getNotification().contentIntent;
            if (contentIntent != null) {
                try {
                    contentIntent.send();
                    Log.d(TAG, "[2] 알림 클릭 완료 -> 스마트플레이스 상세 페이지 진입");
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "인텐트 실행 실패", e);
                    BookingState.isProcessing = false;
                }
            }
        }
    }
}