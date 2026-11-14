package com.example.bookinghelper;

import android.content.Context;
import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingDao;
import com.example.bookinghelper.data.BookingItem;
import com.example.bookinghelper.util.TemplateManager;
import java.util.concurrent.Executors;

public class DebugDataSeeder {

    private static final Object LOCK = new Object();

    public static void seedIfEmpty(Context context, Runnable onComplete) {
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            synchronized (LOCK) {
                AppDatabase db = AppDatabase.getInstance(appContext);
                BookingDao dao = db.bookingDao();

                if (dao.getPendingAndDuplicateList().isEmpty() && dao.getSentHistoryList().isEmpty()) {
                    String msg1 = TemplateManager.buildMessage(appContext, "김민지", "기타", "2026. 11. 15.(일) 오후 2:00");
                    dao.insert(new BookingItem(
                            "어썸기타학원 여의도점", "기타", "김민지", "01012345678",
                            "2026. 11. 15.(일) 오후 2:00", "통기타 무료 체험 레슨",
                            "입문자입니다. 악기 대여 가능할까요?", "PENDING", "수동대기", msg1
                    ));

                    String msg2 = TemplateManager.buildMessage(appContext, "박준형", "보컬", "2026. 11. 16.(월) 오후 7:30");
                    dao.insert(new BookingItem(
                            "어썸보컬드럼 여의도점", "보컬", "박준형", "01098765432",
                            "2026. 11. 16.(월) 오후 7:30", "보컬 원포인트 상담",
                            "", "PENDING", "수동대기", msg2
                    ));

                    String msg3 = TemplateManager.buildMessage(appContext, "이서연", "드럼", "2026. 11. 18.(수) 오후 5:00");
                    dao.insert(new BookingItem(
                            "어썸보컬드럼 여의도점", "드럼", "이서연", "01055554444",
                            "2026. 11. 18.(수) 오후 5:00", "드럼 취미반 상담",
                            "시간 변경해서 다시 예약했어요.", "DUPLICATE", "중복/변경", msg3
                    ));
                }
            }

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}