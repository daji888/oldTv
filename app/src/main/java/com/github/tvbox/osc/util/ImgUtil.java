package com.github.tvbox.osc.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
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

    public static void loadChannelIcon(String logoUrl, ImageView imgLiveIcon) {
        GlideApp.with(imgLiveIcon)
            .load(getUrl(logoUrl))
            .into(imgLiveIcon);
    }

    public static void load(String url, ImageView view, int roundingRadius) {
        if (roundingRadius <= 0) roundingRadius = 1;
        GlideApp.with(view)
            .asBitmap()
            .load(getUrl(url))
            .placeholder(R.drawable.img_loading_placeholder)
            .error(R.drawable.img_loading_placeholder)
            .fallback(R.drawable.img_loading_placeholder)
            .listener(getListener(view))
            .dontAnimate()
            .transform(new MultiTransformation<Bitmap>(new CenterCrop(), new RoundedCorners(roundingRadius)))
            .into(view);
    }

    public static void clearMemoryCache() {
        try {
            GlideApp.get(App.getInstance()).clearMemory();
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

    private static RequestListener <Bitmap> getListener(ImageView view) {
        return new RequestListener<Bitmap> () {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target <Bitmap> target, boolean isFirstResource) {
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target <Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                return false;
            }
        };
    }
}
