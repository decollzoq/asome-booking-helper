package com.example.bookinghelper.util;

public class BookingState {
    // 자동화 진행 중 여부
    public static volatile boolean isProcessing = false;

    // 알림에서 1차로 확보한 타깃 정보 (교차 검증용)
    public static volatile String targetCustomerName = "";
    public static volatile String detectedBranch = ""; // "어썸기타학원 여의도점" or "어썸보컬드럼 여의도점"

    // 동작 모드 상수 ("AUTO" = 자동 발송, "MANUAL" = 수동 발송)
    public static final String MODE_AUTO = "AUTO";
    public static final String MODE_MANUAL = "MANUAL";
    public static volatile String currentMode = MODE_AUTO; // 기본값
}