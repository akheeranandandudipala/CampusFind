package com.campusfind.app.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.campusfind.app.R;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.ImageHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private final Context context;
    private List<Item> itemList;
    private final OnItemClickListener listener;

    public ItemAdapter(Context context, List<Item> itemList, OnItemClickListener listener) {
        this.context = context;
        this.itemList = itemList != null ? itemList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Item> newList) {
        this.itemList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);

        holder.tvItemName.setText(item.getItemName());
        holder.tvCategory.setText("Category: " + item.getCategory());
        holder.tvLocation.setText(item.getLocation());
        holder.tvDate.setText(item.getDate());

        // Item Type Badge
        String itemType = item.getItemType() != null ? item.getItemType().toUpperCase() : "ITEM";
        holder.tvItemTypeBadge.setText(itemType + " ITEM");
        if ("FOUND".equalsIgnoreCase(itemType)) {
            holder.tvItemTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.tvItemTypeBadge.setBackgroundResource(R.drawable.badge_matched);
        } else {
            holder.tvItemTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
            holder.tvItemTypeBadge.setBackgroundResource(R.drawable.badge_active);
        }

        // Status Badge
        String status = item.getStatus() != null ? item.getStatus() : "Active";
        holder.tvStatusBadge.setText(status);
        switch (status.toLowerCase()) {
            case "returned":
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_returned));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_returned);
                break;
            case "matched":
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_matched));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_matched);
                break;
            case "active":
            default:
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_active);
                break;
        }

        // Photo Thumbnail
        String photoPath = item.getPhotoPath();
        if (photoPath != null && !photoPath.trim().isEmpty()) {
            Bitmap thumb = ImageHelper.loadPhotoBitmap(context, photoPath);
            if (thumb != null) {
                holder.cardItemThumbnail.setVisibility(View.VISIBLE);
                holder.ivItemThumbnail.setImageBitmap(thumb);
            } else {
                holder.cardItemThumbnail.setVisibility(View.GONE);
            }
        } else {
            holder.cardItemThumbnail.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView tvItemTypeBadge;
        final TextView tvStatusBadge;
        final TextView tvItemName;
        final TextView tvCategory;
        final TextView tvLocation;
        final TextView tvDate;
        final MaterialCardView cardItemThumbnail;
        final ImageView ivItemThumbnail;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemTypeBadge = itemView.findViewById(R.id.tvItemTypeBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDate = itemView.findViewById(R.id.tvDate);
            cardItemThumbnail = itemView.findViewById(R.id.cardItemThumbnail);
            ivItemThumbnail = itemView.findViewById(R.id.ivItemThumbnail);
        }
    }
}
