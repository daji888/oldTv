package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ImgUtil;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;


public class HomeHotVodAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public HomeHotVodAdapter() {
        super(R.layout.item_user_hot_vod, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
    	// takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }

        TextView tvRate = helper.getView(R.id.tvRate);
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2){
            tvRate.setText(ApiConfig.get().getSource(item.sourceKey).getName());
        }else if(Hawk.get(HawkConfig.HOME_REC, 0) == 0){
            tvRate.setText("豆瓣热播");
        }else {
            tvRate.setVisibility(View.GONE);
        }

        TextView tvNote = helper.getView(R.id.tvNote);
        if (item.note == null || item.note.isEmpty()) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.note);
            tvNote.setVisibility(View.VISIBLE);
        }
        helper.setText(R.id.tvName, item.name);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            ImgUtil.load(item.pic, ivThumb, 10);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}
