package com.example.bookinghelper;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingDao;
import com.example.bookinghelper.data.BookingItem;
import com.example.bookinghelper.util.TemplateManager;
import com.example.bookinghelper.util.TimeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BookingHelperTest {

    private Context context;
    private AppDatabase db;
    private BookingDao dao;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = AppDatabase.getInstance(context);
        dao = db.bookingDao();
    }

    /**
     * 1. 템플릿 치환 및 연락처 포맷 로직 자동 검증
     */
    @Test
    public void testTemplateAndContactFormatting() {
        String contact = TemplateManager.buildContactName(context, "김민지", "기타", "어썸기타학원 여의도점");
        assertTrue(contact.contains("김민지"));
        assertTrue(contact.contains("기타"));

        String message = TemplateManager.buildMessage(context, "김민지", "기타", "2026. 11. 15.(일) 오후 2:00");
        assertTrue(message.contains("김민지님!"));
        assertTrue(message.contains("2026. 11. 15.(일) 오후 2:00"));
    }

    /**
     * 2. 실제 기기(갤럭시 A50) DB에 테스트용 예약 3건 주입
     * 이 테스트만 단독 실행한 뒤 앱을 켜면 대기열/중복배너/발송기록 UI를 바로 눈으로 테스트할 수 있습니다.
     */
    @Test
    public void seedDummyDataToDevice() {
        // 기존 테스트 데이터가 있다면 초기화
        db.clearAllTables();

        // (1) 일반 대기 건 - 요청사항 있음
        String msg1 = TemplateManager.buildMessage(context, "김민지", "기타", "2026. 11. 15.(일) 오후 2:00");
        BookingItem item1 = new BookingItem(
                "어썸기타학원 여의도점", "기타", "김민지", "01012345678",
                "2026. 11. 15.(일) 오후 2:00", "통기타 무료 체험 레슨",
                "입문자입니다. 악기 대여 가능할까요?", "PENDING", "수동대기", msg1
        );

        // (2) 일반 대기 건 - 요청사항 없음 (요청사항 행 숨김 확인용)
        String msg2 = TemplateManager.buildMessage(context, "박준형", "보컬", "2026. 11. 16.(월) 오후 7:30");
        BookingItem item2 = new BookingItem(
                "어썸보컬드럼 여의도점", "보컬", "박준형", "01098765432",
                "2026. 11. 16.(월) 오후 7:30", "보컬 원포인트 상담",
                "", "PENDING", "수동대기", msg2
        );

        // (3) 당일 중복/변경 감지 건 (상단 빨간 배너 및 빨간 배지 확인용)
        String msg3 = TemplateManager.buildMessage(context, "이서연", "드럼", "2026. 11. 18.(수) 오후 5:00");
        BookingItem item3 = new BookingItem(
                "어썸보컬드럼 여의도점", "드럼", "이서연", "01055554444",
                "2026. 11. 18.(수) 오후 5:00", "드럼 취미반 상담",
                "시간 변경해서 다시 예약했어요.", "DUPLICATE", "중복/변경", msg3
        );

        dao.insert(item1);
        dao.insert(item2);
        dao.insert(item3);

        // DB 정상 삽입 및 쿼리 검증
        List<BookingItem> queueList = dao.getPendingAndDuplicateList();
        assertEquals(3, queueList.size());

        int todayDupCount = dao.countDuplicatesAfter(TimeUtils.getStartOfToday());
        assertEquals(1, todayDupCount);
    }

    /**
     * 3. 테스트 종료 후 기기 DB 깨끗하게 비우기
     */
    @Test
    public void clearAllDeviceData() {
        db.clearAllTables();
        assertTrue(dao.getPendingAndDuplicateList().isEmpty());
        assertTrue(dao.getSentHistoryList().isEmpty());
    }
}