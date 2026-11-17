package com.example.bookinghelper.ui;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.bookinghelper.R;
import com.example.bookinghelper.data.AppDatabase;
import com.example.bookinghelper.data.BookingItem;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class ReviewMessageBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "BookingPipeline";
    private static final String ARG_BOOKING_ID = "booking_id";

    private long bookingId;
    private BookingItem currentItem;
    private OnMessageSentListener listener;

    public interface OnMessageSentListener {
        void onSentSuccess();
    }

    public static ReviewMessageBottomSheet newInstance(long bookingId) {
        ReviewMessageBottomSheet fragment = new ReviewMessageBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnMessageSentListener(OnMessageSentListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookingId = getArguments().getLong(ARG_BOOKING_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_review_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        TextView tvCustomerName = view.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone);
        TextView tvSheetItem = view.findViewById(R.id.tvSheetItem);
        TextView tvSheetDate = view.findViewById(R.id.tvSheetDate);
        View layoutSheetRequest = view.findViewById(R.id.layoutSheetRequest);
        TextView tvSheetRequest = view.findViewById(R.id.tvSheetRequest);
        EditText etMessagePreview = view.findViewById(R.id.etMessagePreview);
        ImageView btnClose = view.findViewById(R.id.btnSheetClose);
        AppCompatButton btnSend = view.findViewById(R.id.btnSendSms);

        btnClose.setOnClickListener(v -> dismiss());

        // 비동기로 DB에서 해당 예약 건 조회
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            currentItem = db.bookingDao().getById(bookingId);

            if (currentItem != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    String name = currentItem.customerName;
                    if (name != null && !name.isEmpty()) {
                        tvAvatarInitial.setText(String.valueOf(name.charAt(0)));
                    }
                    tvCustomerName.setText(name);
                    tvCustomerPhone.setText(formatPhoneNumber(currentItem.phone));

                    tvSheetItem.setText(currentItem.subject != null ? currentItem.subject : "기타");
                    tvSheetDate.setText(currentItem.bookingDate != null ? currentItem.bookingDate : "-");

                    // 요청사항이 있을 때만 노출
                    if (currentItem.userRequest != null && !currentItem.userRequest.trim().isEmpty()) {
                        layoutSheetRequest.setVisibility(View.VISIBLE);
                        tvSheetRequest.setText(currentItem.userRequest.trim());
                    } else {
                        layoutSheetRequest.setVisibility(View.GONE);
                    }

                    etMessagePreview.setText(currentItem.messageBody);
                });
            }
        });

        // [메시지로 보내기] 버튼 클릭
        btnSend.setOnClickListener(v -> {
            if (currentItem == null) return;

            String editedMessage = etMessagePreview.getText().toString().trim();
            if (editedMessage.isEmpty()) {
                Toast.makeText(requireContext(), "보낼 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. 실제 고객 번호로 SMS 발송
            sendSms(currentItem.phone, editedMessage);

            // 2. Room DB 상태를 SENT로 변경
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                currentItem.messageBody = editedMessage;
                currentItem.status = "SENT";
                currentItem.modeTag = "[수동]";
                currentItem.sentAt = System.currentTimeMillis();
                db.bookingDao().update(currentItem);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "문자가 정상적으로 발송되었습니다.", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onSentSuccess();
                        }
                        dismiss();
                    });
                }
            });
        });
    }

    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
            Log.d(TAG, "수동 모드 발송 성공 -> " + phone);
        } catch (Exception e) {
            Log.e(TAG, "수동 발송 실패", e);
            Toast.makeText(requireContext(), "문자 발송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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