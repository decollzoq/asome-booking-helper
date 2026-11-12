package com.example.bookinghelper.util;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bookinghelper.service.MyAccessibilityService;

public class PermissionHelper {

    // 1. 접근성 서비스 활성화 여부
    public static boolean isAccessibilityServiceEnabled(Context context) {
        ComponentName expectedComponent = new ComponentName(context, MyAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServices);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledComponent = ComponentName.unflattenFromString(componentNameString);
            if (enabledComponent != null && enabledComponent.equals(expectedComponent)) {
                return true;
            }
        }
        return false;
    }

    // 2. 알림 읽기(리스너) 권한 활성화 여부
    public static boolean isNotificationListenerEnabled(Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(context.getPackageName());
    }

    // 3. 런타임 권한 (문자, 주소록) 확인
    public static boolean hasRuntimePermissions(Context context) {
        boolean sms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        return sms && contacts;
    }

    // 4. 런타임 권한 (문자, 주소록) 시스템 팝업 요청
    public static void requestRuntimePermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.WRITE_CONTACTS
                },
                requestCode
        );
    }

    // 설정창 이동 인텐트들
    public static void openAccessibilitySettings(Context context) {
        context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public static void openNotificationListenerSettings(Context context) {
        context.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
    }
}