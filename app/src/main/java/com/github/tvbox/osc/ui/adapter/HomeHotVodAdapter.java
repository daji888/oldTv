package com.github.tvbox.osc.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
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
        } else if(Hawk.get(HawkConfig.HOME_REC, 0) == 0){
            tvRate.setText("豆瓣热播");
        } else {
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
            item.pic = item.pic.trim();
            if (isBase64Image(item.pic)) {
                // 如果是 Base64 图片，解码并设置
                ivThumb.setImageBitmap(decodeBase64ToBitmap(item.pic));
            } else {
                ImgUtil.load(item.pic, ivThumb, 10);
            }
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }

    private boolean isBase64Image(String picUrl) {
        return picUrl.startsWith("data:image");
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        // 去掉 Base64 数据的头部前缀，例如 "data:image/png;base64,"
        String base64Data = base64Str.substring(base64Str.indexOf(",") + 1);
        byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}
