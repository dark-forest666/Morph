// java
package com.example.morph_02.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.morph_02.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetectionAdapter extends RecyclerView.Adapter<DetectionAdapter.VH> {

    private final List<DetectionItem> items;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private OnItemClickListener itemClickListener;
    private OnRemindClickListener remindClickListener;
    private OnDeleteClickListener deleteClickListener;

    // 👇 新增接口
    public interface OnDeleteClickListener {
        void onDeleteClick(int position, DetectionItem item);
    }

    // 👇 新增 setter
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }
    public DetectionAdapter(List<DetectionItem> initial) {
        this.items = new ArrayList<>(initial != null ? initial : new ArrayList<>());
    }


    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detection, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DetectionItem item = items.get(position);   // 👈 改为直接获取 item
        String time = fmt.format(new Date(item.getTimestamp()));
        holder.tvLabelTime.setText(item.getLabel() + "  ·  " + time);
        holder.tvDetail.setText(String.format(Locale.getDefault(),
                "置信度: %.2f  强度: %.2f",
                item.getConfidence(), item.getIntensity()));
        holder.ivReminded.setAlpha(item.isReminded() ? 1.0f : 0.4f);

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(position, item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(position, item);
        });

        holder.ivReminded.setOnClickListener(v -> {
            boolean newState = !item.isReminded();
            // 切换状态（本地更新，数据库更新需回调至 Activity）
            if (remindClickListener != null) remindClickListener.onRemindClick(position, item);
        });

        // 👇 新增删除点击
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<DetectionItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    private DetectionItem updateRemindedState(int position, boolean reminded) {
        DetectionItem it = items.get(position);
        // 如果 DetectionItem 是不可变的，替换为新实例；此处直接修改字段需确保类允许
        try {
            // 反射或提供 setter 更复杂；假设 DetectionItem 提供合适方法则使用它
            // 这里简单处理，若没有 setter，可替换为新的 DetectionItem 实例
        } catch (Exception ignored) {}
        // 简单方式：如果 DetectionItem 有 setter，请调用；否则用替换构造的方法
        DetectionItem newIt = new DetectionItem(it.getTimestamp(), it.getLabel(), it.getConfidence(), it.getIntensity(), reminded);
        items.set(position, newIt);
        notifyItemChanged(position);
        return newIt;
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.itemClickListener = l; }
    public void setOnRemindClickListener(OnRemindClickListener l) { this.remindClickListener = l; }

    public interface OnItemClickListener {
        void onItemClick(int position, DetectionItem item);
    }

    public interface OnRemindClickListener {
        void onRemindClick(int position, DetectionItem item);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLabelTime, tvDetail;
        ImageView ivReminded;
        ImageView ivDelete;
        VH(@NonNull View itemView) {
            super(itemView);
            tvLabelTime = itemView.findViewById(R.id.tv_label_time);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            ivReminded = itemView.findViewById(R.id.iv_reminded);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }

}