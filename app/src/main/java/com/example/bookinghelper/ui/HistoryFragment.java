package com.example.bookinghelper.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookinghelper.R;
import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistoryList;
    private HistoryAdapter adapter;
    private TextView tvEmptyHistory;
    private EditText etHistorySearch;
    private ImageView btnClearSearch;

    private final SimpleDateFormat detailDateFormat = new SimpleDateFormat("yyyy. MM. dd.(E) HH:mm", Locale.KOREAN);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistoryList = view.findViewById(R.id.rvHistoryList);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        etHistorySearch = view.findViewById(R.id.etHistorySearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);

        rvHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();

        // 카드 클릭 시 상세 바텀시트 호출 연결
        adapter.setOnItemClickListener(this::showHistoryDetailBottomSheet);

        rvHistoryList.setAdapter(adapter);

        // 검색창 텍스트 변경 감지
        if (etHistorySearch != null) {
            etHistorySearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString();
                    adapter.filter(query);

                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.trim().isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // 검색창 지우기(X) 버튼 클릭
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etHistorySearch != null) {
                    etHistorySearch.setText("");
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        if (!isAdded()) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<BookingItem> historyList = db.bookingDao().getSentHistoryList();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setItems(historyList);
                    if (historyList.isEmpty()) {
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                        rvHistoryList.setVisibility(View.GONE);
                    } else {
                        tvEmptyHistory.setVisibility(View.GONE);
                        rvHistoryList.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    // 발송 상세 바텀시트 띄우기
    private void showHistoryDetailBottomSheet(BookingItem item) {
        if (!isAdded() || item == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_history_detail, null);
        dialog.setContentView(sheetView);

        // XML 뷰 바인딩
        TextView tvModeTag = sheetView.findViewById(R.id.tvDetailModeTag);
        ImageView btnClose = sheetView.findViewById(R.id.btnDetailClose);
        TextView tvAvatar = sheetView.findViewById(R.id.tvDetailAvatar);
        TextView tvName = sheetView.findViewById(R.id.tvDetailName);
        TextView tvPhone = sheetView.findViewById(R.id.tvDetailPhone);
        TextView tvItem = sheetView.findViewById(R.id.tvDetailItem);
        TextView tvDate = sheetView.findViewById(R.id.tvDetailDate);
        TextView tvSentAt = sheetView.findViewById(R.id.tvDetailSentAt);
        View layoutRequest = sheetView.findViewById(R.id.layoutDetailRequest);
        TextView tvRequest = sheetView.findViewById(R.id.tvDetailRequest);
        TextView tvMessageBody = sheetView.findViewById(R.id.tvDetailMessageBody);
        AppCompatButton btnConfirm = sheetView.findViewById(R.id.btnConfirmDetail);

        // 데이터 세팅
        String cleanMode = (item.modeTag != null && item.modeTag.contains("수동")) ? "수동 모드" : "자동 모드";
        tvModeTag.setText(cleanMode);

        String name = (item.customerName != null && !item.customerName.isEmpty()) ? item.customerName : "고객";
        tvAvatar.setText(String.valueOf(name.charAt(0)));
        tvName.setText(name);
        tvPhone.setText(formatPhoneNumber(item.phone));

        tvItem.setText(item.subject != null ? item.subject : "기타");
        tvDate.setText(item.bookingDate != null ? item.bookingDate : "-");

        long sentTimestamp = item.sentAt > 0 ? item.sentAt : item.createdAt;
        tvSentAt.setText(detailDateFormat.format(new Date(sentTimestamp)));

        // 고객 요청사항이 있는 경우에만 노출
        if (item.userRequest != null && !item.userRequest.trim().isEmpty()) {
            layoutRequest.setVisibility(View.VISIBLE);
            tvRequest.setText(item.userRequest.trim());
        } else {
            layoutRequest.setVisibility(View.GONE);
        }

        tvMessageBody.setText(item.messageBody != null ? item.messageBody : "");

        // 닫기 및 확인 버튼 액션
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String formatPhoneNumber(String raw) {
        if (raw == null) return "";
        if (raw.length() == 11) {
            return raw.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
        } else if (raw.length() == 10) {
            return raw.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
        }
        return raw;
    }
}