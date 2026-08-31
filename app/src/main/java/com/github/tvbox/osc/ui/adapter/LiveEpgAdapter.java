package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.tv.widget.AudioWaveView;
import com.github.tvbox.osc.bean.Epginfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class LiveEpgAdapter extends BaseQuickAdapter<Epginfo, BaseViewHolder> {
    private int selectedEpgIndex = -1;
    private int focusedEpgIndex = -1;
    public static float fontSize = 20;
    private int defaultShiyiSelection = 0;
    private boolean ShiyiSelection = false;
    private String shiyiDate = null;
    private String currentEpgDate = null;
    private int focusSelection = -1;
    private boolean source_include_back = false;

    SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
    public LiveEpgAdapter() {
        super(R.layout.item_live_epglist, new ArrayList<>());
    }

    public void CanBack(Boolean source_include_back) {
        this.source_include_back = source_include_back;
    }

    @Override
    protected void convert(BaseViewHolder holder, Epginfo value) {
        TextView textview = holder.getView(R.id.tv_epg_name);
        TextView timeview = holder.getView(R.id.tv_epg_time);
        TextView shiyi = holder.getView(R.id.shiyi);
        AudioWaveView wqddg_AudioWaveView = holder.getView(R.id.wqddg_AudioWaveView);
        wqddg_AudioWaveView.setVisibility(View.GONE);
        Date now = new Date();
        String nowStr = timeFormat.format(now);
        GradientDrawable roundedBg = new GradientDrawable();
        roundedBg.setCornerRadius(AutoSizeUtils.dp2px(holder.itemView.getContext(), 5));
        if (value.index == selectedEpgIndex && value.currentEpgDate != null && (value.currentEpgDate.equals(shiyiDate) || value.currentEpgDate.equals(nowStr))) {
            textview.setTextColor(ContextCompat.getColor(mContext, R.color.color_FF5F00));
            timeview.setTextColor(ContextCompat.getColor(mContext, R.color.color_FF5F00));
        } else {
            textview.setTextColor(Color.WHITE);
            timeview.setTextColor(Color.WHITE);
        }
        if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
            shiyi.setVisibility(View.VISIBLE);
            roundedBg.setColor(Color.YELLOW);
            shiyi.setBackground(roundedBg);
            shiyi.setTextColor(Color.RED);
            shiyi.setText("直播中");
        } else if (now.compareTo(value.startdateTime) > 0 && now.compareTo(value.enddateTime) > 0) {
            shiyi.setVisibility(View.VISIBLE);
            roundedBg.setColor(source_include_back ? 0xff28713E : Color.GRAY);
            shiyi.setBackground(roundedBg);
            shiyi.setTextColor(source_include_back ? Color.WHITE : Color.BLACK);
            shiyi.setText("回看");
        } else if (now.compareTo(value.startdateTime) < 0) {
            shiyi.setVisibility(View.VISIBLE);
            roundedBg.setColor(Color.GRAY);
            shiyi.setBackground(roundedBg);
            shiyi.setTextColor(Color.BLACK);
            shiyi.setText("预约");
        } else {
            shiyi.setVisibility(View.GONE);
        }
        textview.setText(value.title);
        timeview.setText(value.start + "-" + value.end);
        if (!ShiyiSelection) {
            if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
                wqddg_AudioWaveView.setVisibility(View.VISIBLE);
                textview.setFreezesText(true);
                timeview.setFreezesText(true);
            } else {
                wqddg_AudioWaveView.setVisibility(View.GONE);
            }
        } else {
            if ((value.index == this.selectedEpgIndex && value.currentEpgDate.equals(shiyiDate)) && LivePlayActivity.isBack == true) {
                wqddg_AudioWaveView.setVisibility(View.VISIBLE);
                textview.setFreezesText(true);
                timeview.setFreezesText(true);
                roundedBg.setColor(Color.rgb(12, 255, 0));
                shiyi.setBackground(roundedBg);
                shiyi.setTextColor(Color.RED);
                shiyi.setText("回看中");
            } else {
                wqddg_AudioWaveView.setVisibility(View.GONE);
            }
            if (LivePlayActivity.isBack == false) {
                if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
                    wqddg_AudioWaveView.setVisibility(View.VISIBLE);
                    textview.setFreezesText(true);
                    timeview.setFreezesText(true);
                    roundedBg.setColor(Color.YELLOW);
                    shiyi.setBackground(roundedBg);
                    shiyi.setTextColor(Color.RED);
                    shiyi.setText("直播中");
                }    
            }
        }
    }
    
    public void setShiyiSelection(int i, boolean t, String currentEpgDate) {
        this.selectedEpgIndex = i;
        this.shiyiDate = t ? currentEpgDate : null;
        ShiyiSelection = t;
        notifyItemChanged(this.selectedEpgIndex);
    }
    
    public int getSelectedIndex() {
        return selectedEpgIndex;
    }

    public void setSelectedEpgIndex(int selectedEpgIndex) {
        if (selectedEpgIndex == this.selectedEpgIndex) return;
        this.selectedEpgIndex = selectedEpgIndex;
        notifyItemChanged(this.selectedEpgIndex);
        if (this.focusedEpgIndex != -1)
            notifyItemChanged(this.focusedEpgIndex);
    }

    public int getFocusedEpgIndex() {
        return focusedEpgIndex;
    }

    public void setFocusedEpgIndex(int focusedEpgIndex) {
        this.focusedEpgIndex = focusedEpgIndex;
    }
     
}
