package com.example.bookinghelper.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookinghelper.util.BookingState;
import com.example.bookinghelper.util.TimeUtils;
import com.example.bookinghelper.R;
import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingDao;
import com.example.bookinghelper.data.BookingItem;

import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String PREF_NAME = "booking_helper_prefs";
    private static final String KEY_LAST_DUP_CHECKED_TIME = "last_duplicate_checked_time";

    private TextView btnModeAuto, btnModeManual, tvQueueBadgeCount, tvEmptyQueue;
    private View layoutDuplicateBanner;
    private TextView btnViewDuplicate;
    private RecyclerView rvPendingQueue;
    private PendingQueueAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnModeAuto = view.findViewById(R.id.btnModeAuto);
        btnModeManual = view.findViewById(R.id.btnModeManual);
        tvQueueBadgeCount = view.findViewById(R.id.tvQueueBadgeCount);
        tvEmptyQueue = view.findViewById(R.id.tvEmptyQueue);
        rvPendingQueue = view.findViewById(R.id.rvPendingQueue);

        layoutDuplicateBanner = view.findViewById(R.id.layoutDuplicateBanner);
        btnViewDuplicate = view.findViewById(R.id.btnViewDuplicate);

        rvPendingQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PendingQueueAdapter(item -> {
            ReviewMessageBottomSheet sheet = ReviewMessageBottomSheet.newInstance(item.id);
            sheet.setOnMessageSentListener(this::loadData);
            sheet.show(getChildFragmentManager(), "ReviewMessageBottomSheet");
        });
        rvPendingQueue.setAdapter(adapter);

        setupSegmentedControl();

        // 중복 배너의 '확인' 클릭 시 현재 확인 시간 저장 후 배너 닫기
        if (btnViewDuplicate != null) {
            btnViewDuplicate.setOnClickListener(v -> {
                SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putLong(KEY_LAST_DUP_CHECKED_TIME, System.currentTimeMillis()).apply();

                if (layoutDuplicateBanner != null) {
                    layoutDuplicateBanner.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupSegmentedControl() {
        updateSegmentUI(BookingState.MODE_AUTO.equals(BookingState.currentMode));

        btnModeAuto.setOnClickListener(v -> {
            BookingState.currentMode = BookingState.MODE_AUTO;
            updateSegmentUI(true);
        });

        btnModeManual.setOnClickListener(v -> {
            BookingState.currentMode = BookingState.MODE_MANUAL;
            updateSegmentUI(false);
        });
    }

    private void updateSegmentUI(boolean isAuto) {
        if (isAuto) {
            btnModeAuto.setBackgroundResource(R.drawable.bg_segmented_active);
            btnModeAuto.setTextColor(0xFFFFFFFF);

            btnModeManual.setBackground(null);
            btnModeManual.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnModeManual.setBackgroundResource(R.drawable.bg_segmented_active);
            btnModeManual.setTextColor(0xFFFFFFFF);

            btnModeAuto.setBackground(null);
            btnModeAuto.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    public void loadData() {
        if (!isAdded()) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            BookingDao dao = db.bookingDao();

            // 1. 배너 노출 검사: 마지막 확인 시간과 오늘 시작 시간 중 더 큰 기준점 이후의 신규 중복 건수 조회
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastCheckedTime = prefs.getLong(KEY_LAST_DUP_CHECKED_TIME, 0L);
            long startToday = TimeUtils.getStartOfToday();
            long checkSince = Math.max(startToday, lastCheckedTime);

            int unreadDuplicateCount = dao.countDuplicatesAfter(checkSince);

            // 2. 대기열 리스트 (PENDING + DUPLICATE)
            List<BookingItem> displayList = dao.getPendingAndDuplicateList();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    // 미확인 중복 건이 있을 때만 배너 노출
                    if (layoutDuplicateBanner != null) {
                        layoutDuplicateBanner.setVisibility(unreadDuplicateCount > 0 ? View.VISIBLE : View.GONE);
                    }

                    adapter.setItems(displayList);
                    tvQueueBadgeCount.setText(String.valueOf(displayList.size()));

                    if (displayList.isEmpty()) {
                        tvEmptyQueue.setVisibility(View.VISIBLE);
                        rvPendingQueue.setVisibility(View.GONE);
                    } else {
                        tvEmptyQueue.setVisibility(View.GONE);
                        rvPendingQueue.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}