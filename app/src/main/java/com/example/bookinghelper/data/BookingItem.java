package com.example.bookinghelper.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "booking_items")
public class BookingItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String branch;           // "어썸기타학원 여의도점" | "어썸보컬드럼 여의도점"
    public String subject;          // "기타" | "베이스" | "보컬" | "드럼"
    public String customerName;     // 예약자명
    public String phone;            // 연락처
    public String bookingDate;      // 이용일시
    public String itemName;         // 네이버 상품 원문
    public String userRequest;      // 요청사항 (없으면 "")

    public String status;           // "PENDING" (발송 대기) | "SENT" (발송 완료) | "DUPLICATE" (중복 / 변경 감지)
    public String modeTag;          // "자동" | "수동"

    public long createdAt;          // 알림 접수 타임스탬프
    public long sentAt;             // 문자 발송 타임스탬프

    public String messageBody;      // 발송할 (발송된) 메시지 본문

    public BookingItem(){}

    public BookingItem(String branch, String subject, String customerName, String phone, String bookingDate, String itemName, String userRequest, String status, String modeTag, String messageBody) {
        this.branch = branch;
        this.subject = subject;
        this.customerName = customerName;
        this.phone = phone;
        this.bookingDate = bookingDate;
        this.itemName = itemName;
        this.userRequest = userRequest;
        this.status = status;
        this.modeTag = modeTag;
        this.createdAt = System.currentTimeMillis();
        this.sentAt = 0;
        this.messageBody = messageBody;
    }
}
