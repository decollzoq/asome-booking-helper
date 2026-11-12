package com.example.bookinghelper.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bookinghelper.util.PermissionHelper;
import com.example.bookinghelper.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_RUNTIME_PERMS = 1001;

    private BottomNavigationView bottomNav;
    private TextView tvStatusText;
    private View viewStatusDot;
    private ImageView btnPermissionInfo;

    // 권한 다이얼로그 및 상태 배지 뷰 참조
    private AlertDialog permissionDialog;
    private TextView tvAccStatus;
    private TextView tvNotiStatus;
    private TextView tvAppStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation(savedInstanceState);

        // 앱 최초 실행 시 런타임 권한(SMS, 연락처 등) 확인 및 요청
        checkAndRequestRuntimePermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 상단 헤더의 연결됨/연결끊김 상태 갱신
        updateHeaderStatus();

        // 열려있는 권한 안내 팝업의 배지 실시간 갱신
        updatePermissionDialogBadges();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        tvStatusText = findViewById(R.id.tvStatusText);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        btnPermissionInfo = findViewById(R.id.btnPermissionInfo);

        // 'i' 버튼 클릭 시 권한 설정 팝업 호출
        btnPermissionInfo.setOnClickListener(v -> showPermissionDialog());
    }

    private void setupNavigation(Bundle savedInstanceState) {
        // 첫 앱 실행 시 홈 화면을 기본 로드
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // 런타임 권한 미허용 시 시스템 팝업 요청
    private void checkAndRequestRuntimePermissions() {
        if (!PermissionHelper.hasRuntimePermissions(this)) {
            PermissionHelper.requestRuntimePermissions(this, REQ_CODE_RUNTIME_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_RUNTIME_PERMS) {
            updateHeaderStatus();
            updatePermissionDialogBadges();
        }
    }

    // 상단 헤더의 연결 상태 배지 갱신
    private void updateHeaderStatus() {
        boolean acc = PermissionHelper.isAccessibilityServiceEnabled(this);
        boolean noti = PermissionHelper.isNotificationListenerEnabled(this);
        boolean app = PermissionHelper.hasRuntimePermissions(this);

        boolean allGranted = acc && noti && app;

        View layoutBadge = findViewById(R.id.layoutStatusBadge);
        if (layoutBadge == null) return;

        if (allGranted) {
            // 연결됨 상태
            layoutBadge.setBackgroundResource(R.drawable.bg_status_badge);
            tvStatusText.setText("연결됨");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary));
            viewStatusDot.setBackgroundResource(R.drawable.dot_green);
        } else {
            // 연결끊김 상태
            layoutBadge.setBackgroundResource(R.drawable.bg_status_badge_red);
            tvStatusText.setText("연결끊김");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected));
            viewStatusDot.setBackgroundResource(R.drawable.dot_red);
        }
    }

    // 'i' 버튼 클릭 시 노출되는 권한 안내 및 설정 이동 팝업
    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_permissions, null);
        builder.setView(dialogView);

        permissionDialog = builder.create();

        // 다이얼로그 바깥 기본 사각 프레임 투명화
        if (permissionDialog.getWindow() != null) {
            permissionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 뷰 바인딩
        tvAccStatus = dialogView.findViewById(R.id.tvAccessibilityStatus);
        tvNotiStatus = dialogView.findViewById(R.id.tvNotificationStatus);
        tvAppStatus = dialogView.findViewById(R.id.tvAppPermStatus);

        // 설정 바로가기 액션
        dialogView.findViewById(R.id.btnGoAccessibility).setOnClickListener(v -> PermissionHelper.openAccessibilitySettings(this));
        dialogView.findViewById(R.id.btnGoNotification).setOnClickListener(v -> PermissionHelper.openNotificationListenerSettings(this));
        dialogView.findViewById(R.id.btnGoAppPerm).setOnClickListener(v -> PermissionHelper.openAppSettings(this));

        // 닫기 액션
        dialogView.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> permissionDialog.dismiss());
        dialogView.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> permissionDialog.dismiss());

        // 1. 다이얼로그를 먼저 화면에 띄웁니다.
        permissionDialog.show();

        // 2. 다이얼로그가 뜬 직후 현재 실제 권한 상태를 반영합니다.
        updatePermissionDialogBadges();
    }

    // 다이얼로그 배지만 실시간으로 다시 갱신하는 메서드
    private void updatePermissionDialogBadges() {
        if (permissionDialog == null || !permissionDialog.isShowing()) return;

        boolean acc = PermissionHelper.isAccessibilityServiceEnabled(this);
        boolean noti = PermissionHelper.isNotificationListenerEnabled(this);
        boolean app = PermissionHelper.hasRuntimePermissions(this);

        applyBadgeStyle(tvAccStatus, acc, "활성화됨", "설정 필요");
        applyBadgeStyle(tvNotiStatus, noti, "허용됨", "설정 필요");
        applyBadgeStyle(tvAppStatus, app, "허용됨", "설정 필요");
    }

    // 상태에 따른 배지 스타일 적용
    private void applyBadgeStyle(TextView tv, boolean isGranted, String grantedText, String deniedText) {
        if (tv == null) return;
        if (isGranted) {
            tv.setText(grantedText);
            tv.setBackgroundResource(R.drawable.bg_badge_green);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_green_text));
        } else {
            tv.setText(deniedText);
            tv.setBackgroundResource(R.drawable.bg_badge_orange);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_orange_text));
        }
    }
}