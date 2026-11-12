package com.example.bookinghelper.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookinghelper.R;
import com.example.bookinghelper.data.BookingItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<BookingItem> items = new ArrayList<>();
    private List<BookingItem> allItems = new ArrayList<>();
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy. MM. dd.(E) HH:mm", Locale.KOREAN);
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BookingItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BookingItem> newItems) {
        this.allItems = new ArrayList<>(newItems);
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            items = new ArrayList<>(allItems);
        } else {
            String lowerQuery = query.trim().toLowerCase(Locale.KOREAN);
            List<BookingItem> filtered = new ArrayList<>();
            for (BookingItem item : allItems) {
                boolean nameMatch = item.customerName != null && item.customerName.toLowerCase(Locale.KOREAN).contains(lowerQuery);
                boolean phoneMatch = item.phone != null && item.phone.contains(lowerQuery);
                boolean subjectMatch = item.subject != null && item.subject.toLowerCase(Locale.KOREAN).contains(lowerQuery);

                if (nameMatch || phoneMatch || subjectMatch) {
                    filtered.add(item);
                }
            }
            items = filtered;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingItem item = items.get(position);

        String name = (item.customerName != null && !item.customerName.isEmpty()) ? item.customerName : "고객";
        holder.tvCustomerName.setText(name);
        holder.tvSubjectBadge.setText(item.subject);

        long sentTimestamp = item.sentAt > 0 ? item.sentAt : item.createdAt;
        holder.tvTimeAgo.setText(calculateTimeAgo(sentTimestamp));

        String modeTag = item.modeTag != null ? item.modeTag.replace("[", "").replace("]", "") : "자동";
        holder.tvModeBadge.setText(modeTag);

        holder.tvPhone.setText(formatPhoneNumber(item.phone));
        holder.tvBookingDate.setText(item.bookingDate != null ? item.bookingDate : "");

        if (item.sentAt > 0) {
            holder.tvSentTime.setText(fullDateFormat.format(new Date(item.sentAt)));
        } else {
            holder.tvSentTime.setText(fullDateFormat.format(new Date(item.createdAt)));
        }

        if (item.messageBody != null && !item.messageBody.trim().isEmpty()) {
            holder.tvMessagePreview.setVisibility(View.VISIBLE);
            holder.tvMessagePreview.setText(item.messageBody);
        } else {
            holder.tvMessagePreview.setVisibility(View.GONE);
        }

        // 카드 클릭 시 상세 바텀시트 호출 이벤트 전달
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvTimeAgo, tvModeBadge, tvSubjectBadge;
        TextView tvPhone, tvBookingDate, tvSentTime, tvMessagePreview;

        ViewHolder(View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvHistoryCustomerName);
            tvTimeAgo = itemView.findViewById(R.id.tvHistoryTimeAgo);
            tvModeBadge = itemView.findViewById(R.id.tvHistoryModeBadge);
            tvSubjectBadge = itemView.findViewById(R.id.tvHistorySubjectBadge);
            tvPhone = itemView.findViewById(R.id.tvHistoryPhone);
            tvBookingDate = itemView.findViewById(R.id.tvHistoryBookingDate);
            tvSentTime = itemView.findViewById(R.id.tvHistorySentTime);
            tvMessagePreview = itemView.findViewById(R.id.tvHistoryMessagePreview);
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