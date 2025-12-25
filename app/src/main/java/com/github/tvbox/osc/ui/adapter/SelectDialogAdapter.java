package com.github.tvbox.osc.ui.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectDialogAdapter<T> extends ListAdapter<T, SelectDialogAdapter.SelectViewHolder<T>> {

    static class SelectViewHolder<T> extends RecyclerView.ViewHolder {
        public SelectViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
        }
    }

    public interface SelectDialogInterface<T> {
        void click(T value, int pos);
        String getDisplay(T val);
    }

    public static DiffUtil.ItemCallback<String> stringDiff = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
            return oldItem.equals(newItem);
        }
    };

    private ArrayList<T> data = new ArrayList<>();
    private int select = 0;
    private SelectDialogInterface<T> dialogInterface;

    public SelectDialogAdapter(SelectDialogInterface<T> dialogInterface, DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        this.dialogInterface = dialogInterface;
    }

    public void setData(List<T> newData, int defaultSelect) {
        data.clear();
        data.addAll(newData);
        select = defaultSelect;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public SelectViewHolder<T> onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new SelectViewHolder<>(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_select, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull SelectViewHolder<T> holder, @SuppressLint("RecyclerView") int position) {    
        T value = data.get(position);
        String name = dialogInterface.getDisplay(value);
        TextView view = holder.itemView.findViewById(R.id.tvName);
        if (position == select) {
            view.setTextColor(0xffFF9900);
            view.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            view.setTextColor(0xD9FFFFFF);
            view.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        view.setText(name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position == select)
                    return;
                notifyItemChanged(select);
                select = position;
                notifyItemChanged(select);
                dialogInterface.click(value, position);
            }
        });
    }
}
