package com.example.bookinghelper.util;

import android.content.Context;
import android.content.SharedPreferences;

public class TemplateManager {

    private static final String PREF_NAME = "booking_template_pref";
    private static final String KEY_SMS_TEMPLATE = "sms_template";
    private static final String KEY_CONTACT_FORMAT = "contact_format";

    // 기본 연락터 저장 포맷
    public static final String DEFAULT_CONTACT_FORMAT = "어썸 {과목} {이름}";
    // 기본 템플릿 본문 ({이름}, {과목}, {일시} 치환 변수 사용)
    public static final String DEFAULT_TEMPLATE =
            "안녕하세요 {이름}님!\n" +
                    "상담 예약해 주신 어썸{과목} 여의도점 입니다.\n\n" +
                    "■ 예약 일정\n\n" +
                    "- {일시}\n\n" +
                    "■ 오시는 길\n\n" +
                    "1. 주소 : 서울 영등포구 국제금융로 108 2층\n" +
                    "2. 지하철 : 9호선 샛강역 도보 5분\n" +
                    "3. 주차 여부 : 당건물은 주차가 불가능합니다. 차량 이용 시 근처 공영 주차장 이용 바랍니다. (대중교통 권장)\n\n" +
                    "■ 예약 시간보다 늦어질 경우\n\n" +
                    "- 상담이 연달아 있는 상황이 있으니 꼭 미리 연락 부탁드립니다.\n\n" +
                    "추가 궁금하신 사항은 편하게 연락 주세요!\n\n" +
                    "NO PAIN, YES GAIN\n" +
                    "음악을 가르치며 즐거움을 드리는 일은\n" +
                    "저희가 가장 잘하는 일입니다.";

    // 저장된 연락처 포맷 가져오기
    public static String getContactFormat(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_CONTACT_FORMAT, DEFAULT_CONTACT_FORMAT);
    }

    // 연락처 포맷 저장하기
    public static void saveContactFormat(Context context, String newFormat) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_CONTACT_FORMAT, newFormat).apply();
    }

    // 변수 치환 후 최종 저장될 연락처 이름 생성
    public static String buildContactName(Context context, String name, String subject, String branch) {
        String format = getContactFormat(context);
        return format.replace("{이름}", name != null ? name : "고객")
                .replace("{과목}", subject != null ? subject : "기타")
                .replace("{지점}", branch != null ? branch : "어썸기타");
    }

    // 저장된 문자 템플릿 가져오기
    public static String getTemplate(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_SMS_TEMPLATE, DEFAULT_TEMPLATE);
    }

    // 문자 템플릿 저장하기
    public static void saveTemplate(Context context, String newTemplate) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_SMS_TEMPLATE, newTemplate).apply();
    }

    // 변수 치환 후 최종 발송용 문자열 생성
    public static String buildMessage(Context context, String name, String subject, String date) {
        String template = getTemplate(context);
        return template.replace("{이름}", name != null ? name : "고객")
                .replace("{과목}", subject != null ? subject : "기타")
                .replace("{일시}", date != null ? date : "");
    }
}