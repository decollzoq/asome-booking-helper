package com.example.bookinghelper.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookinghelper.R;
import com.example.bookinghelper.data.BookingItem;

import java.util.ArrayList;
import java.util.List;

public class PendingQueueAdapter extends RecyclerView.Adapter<PendingQueueAdapter.ViewHolder> {

    private List<BookingItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCheckMessageClick(BookingItem item);
    }

    public PendingQueueAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BookingItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingItem item = items.get(position);

        String customerName = (item.customerName != null && !item.customerName.isEmpty()) ? item.customerName : "고객";

        // 1. 상단 카드 타이틀 바인딩
        if (holder.tvHeaderName != null) {
            holder.tvHeaderName.setText(customerName + "님의 새 예약");
        }

        // 2. 경과 시간 및 과목
        holder.tvTimeAgo.setText(calculateTimeAgo(item.createdAt));
        holder.tvSubject.setText(item.subject);

        // 3. 중복/변경 상태 배지 처리
        if (holder.tvStatusBadge != null) {
            if ("DUPLICATE".equals(item.status)) {
                holder.tvStatusBadge.setText("중복/변경");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_red);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_red_text));
            } else {
                holder.tvStatusBadge.setText("응대 전");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_orange_text));
            }
        }

        // 4. 이름 및 연락처
        holder.tvName.setText(customerName);
        holder.tvPhone.setText(formatPhoneNumber(item.phone));

        // 5. 이용일시
        holder.tvBookingDate.setText(item.bookingDate != null ? item.bookingDate : "");

        // 6. 요청사항 (없는 경우 안 보이게 처리)
        if (item.userRequest != null && !item.userRequest.trim().isEmpty()) {
            holder.layoutRequest.setVisibility(View.VISIBLE);
            holder.tvRequest.setText(item.userRequest);
        } else {
            holder.layoutRequest.setVisibility(View.GONE);
        }

        // 7. 문자 확인 버튼
        holder.btnCheckMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCheckMessageClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderName;
        TextView tvSubject, tvTimeAgo, tvStatusBadge;
        TextView tvName, tvPhone, tvBookingDate, tvRequest;
        AppCompatButton btnCheckMessage;
        View layoutRequest;

        ViewHolder(View itemView) {
            super(itemView);
            tvHeaderName = itemView.findViewById(R.id.tvCardHeaderName);
            tvSubject = itemView.findViewById(R.id.tvCardSubjectBadge);
            tvTimeAgo = itemView.findViewById(R.id.tvCardTimeAgo);
            tvStatusBadge = itemView.findViewById(R.id.tvCardStatusBadge);
            tvName = itemView.findViewById(R.id.tvCardName);
            tvPhone = itemView.findViewById(R.id.tvCardPhone);
            tvBookingDate = itemView.findViewById(R.id.tvCardBookingDate);
            tvRequest = itemView.findViewById(R.id.tvCardRequest);
            btnCheckMessage = itemView.findViewById(R.id.btnCheckMessage);
            layoutRequest = itemView.findViewById(R.id.layoutCardRequest);
        }
    }

    private String calculateTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (1000 * 60);
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";
        return (hours / 24) + "일 전";
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