package com.github.tvbox.osc.util;

import static com.bumptech.glide.load.resource.bitmap.VideoDecoder.FRAME_OPTION;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Base64;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.R;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class ImgUtil {
    public static boolean isBase64Image(String picUrl) {
         return picUrl != null && picUrl.startsWith("data:image");
     }
 
     public static Bitmap decodeBase64ToBitmap(String base64Str) {
         // 去掉 Base64 数据的头部前缀，例如 "data:image/png;base64,"
         String base64Data = base64Str.substring(base64Str.indexOf(",") + 1);
         byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
         return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
     }

    public static void load(String url, ImageView view, int roundingRadius) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (TextUtils.isEmpty(url)) {
            view.setImageResource(R.drawable.img_loading_placeholder);
        } else {
            if (roundingRadius == 0) roundingRadius = 1;
            RequestOptions requestOptions = new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontAnimate()
                .transform(new RoundedCorners(roundingRadius));
            Glide.with(App.getInstance())
                .asBitmap()
                .load(getUrl(url))
                .error(R.drawable.img_loading_placeholder)
                .placeholder(R.drawable.img_loading_placeholder)
                .listener(getListener(view, ImageView.ScaleType.FIT_XY))
                .apply(requestOptions)
                .into(view);
        }
    }

    /*
     * 使用Glide方式获取视频某一帧
     * @param uri 视频地址
     * @param imageView 设置image
     * @param frameTimeMicros 获取某一时间帧.
     */
    public static void loadVideoScreenshot(String uri, ImageView imageView, long frameTimeMicros) {
        RequestOptions requestOptions = RequestOptions.frameOf(frameTimeMicros * 1000)
            .set(FRAME_OPTION, MediaMetadataRetriever.OPTION_CLOSEST)
            .transform(new RoundedCorners(10));
        Glide.with(App.getInstance())
            .load(uri)
            .skipMemoryCache(true)
            .apply(requestOptions)
            .into(imageView);
    }

    public static void clearMemoryCache() {
        try {
            Glide.get(App.getInstance()).clearMemory();
            LOG.i("echo-img-clear-memory-cache");
        } catch (Throwable th) {
            LOG.i("echo-img-clear-memory-cache-error:" + th.getMessage());
        }
    }

    private static Object getUrl(String url) {
        if (url.startsWith("data:")) return url;
        String header = null;
        String referer = null;
        String ua = null;
        String cookie = null;
        if (url.contains("@Headers=")) {
            header = url.split("@Headers=")[1].split("@")[0];
            try {
                header = URLDecoder.decode(header, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        if (url.contains("@Cookie=")) cookie = url.split("@Cookie=")[1].split("@")[0];
        if (url.contains("@User-Agent=")) ua = url.split("@User-Agent=")[1].split("@")[0];
        if (url.contains("@Referer=")) referer = url.split("@Referer=")[1].split("@")[0];
        url = url.split("@")[0];
        if (TextUtils.isEmpty(url)) return null;

        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        Map<String, String> headers = new HashMap<>();
        if (!TextUtils.isEmpty(header)) {
            try {
                JsonObject jsonInfo = new Gson().fromJson(header, JsonObject.class);
                for (String key : jsonInfo.keySet()) {
                    putHeader(headers, key, jsonInfo.get(key).getAsString());
                }
            } catch (Throwable ignored) {
            }
        }
        putHeader(headers, "Cookie", cookie);
        if (!TextUtils.isEmpty(ua)) putHeader(headers, "User-Agent", ua);
        if (!TextUtils.isEmpty(referer)) putHeader(headers, "Referer", referer);
        for (Map.Entry<String, String> entry : headers.entrySet()) builder.setHeader(entry.getKey(), entry.getValue());
        return new GlideUrl(url, builder.build());
    }

    private static void putHeader(Map<String, String> headers, String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) return;
        headers.put(key, value.trim());
    }

    private static RequestListener < Bitmap > getListener(ImageView view, ImageView.ScaleType scaleType) {
        return new RequestListener < Bitmap > () {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target <Bitmap> target, boolean isFirstResource) {
                view.setScaleType(scaleType);
                view.setImageResource(R.drawable.img_loading_placeholder);
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target <Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(scaleType);
                return false;
            }
        };
    }
}
