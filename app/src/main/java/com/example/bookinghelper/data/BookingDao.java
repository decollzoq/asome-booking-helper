package com.example.bookinghelper.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookingDao {
    @Insert
    long insert(BookingItem item);

    @Update
    void update(BookingItem item);

    // 발송 대기 목록 조회 (최신순)
    @Query("SELECT * FROM booking_items WHERE status = 'PENDING' ORDER BY createdAt DESC")
    List<BookingItem> getPendingList();

    // 홈 화면 대기열용: 대기(PENDING) 및 중복/변경(DUPLICATE) 건 전체 조회 (최신순)
    @Query("SELECT * FROM booking_items WHERE status IN ('PENDING', 'DUPLICATE') ORDER BY createdAt DESC")
    List<BookingItem> getPendingAndDuplicateList();

    // 특정 타임스탬프 이후 발생한 중복 건수 조회 (배너 재노출 방지용)
    @Query("SELECT COUNT(*) FROM booking_items WHERE status = 'DUPLICATE' AND createdAt > :sinceTime")
    int countDuplicatesAfter(long sinceTime);

    // 오늘 발생한 중복/변경 건수 조회
    @Query("SELECT COUNT(*) FROM booking_items WHERE status = 'DUPLICATE' AND createdAt >= :startOfDay AND createdAt <= :endOfDay")
    int countTodayDuplicates(long startOfDay, long endOfDay);

    // 발송 완료 이력 조회 (최신순)
    @Query("SELECT * FROM booking_items WHERE status = 'SENT' ORDER BY sentAt DESC")
    List<BookingItem> getSentHistoryList();

    // 중복/변경 감지 목록 조회
    @Query("SELECT * FROM booking_items WHERE status = 'DUPLICATE' ORDER BY createdAt DESC")
    List<BookingItem> getDuplicateList();

    // 당일 내 동일 번호 예약 접수 이력 확인
    @Query("SELECT COUNT(*) FROM booking_items WHERE phone = :phone AND createdAt >= :startOfDay AND createdAt <= :endOfDay")
    int countBookingsTodayByPhone(String phone, long startOfDay, long endOfDay);

    // 단일 건 조회
    @Query("SELECT * FROM booking_items WHERE id = :id LIMIT 1")
    BookingItem getById(long id);
}