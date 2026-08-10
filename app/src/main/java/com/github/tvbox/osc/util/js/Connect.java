package com.github.tvbox.osc.util.js;

import android.util.Base64;

import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkHttpHelper;
import com.google.common.net.HttpHeaders;
import com.lzy.okgo.OkGo;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Connect {
    
    static OkHttpClient client;
    
    public static Call to(String url, Req req) {
        client = OkHttpHelper.getDefaultClient();
        return client.newCall(getRequest(url, req, Headers.of(req.getHeader())));
    }    

    public static JSObject success(QuickJSContext ctx, Req req, Response response) {
        try (Response res = response) {
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            setHeader(ctx, res, jsHeader);
            jsObject.setProperty("code", res.code());
            jsObject.setProperty("headers", jsHeader);
            if (req.getBuffer() == 0) jsObject.setProperty("content", new String(res.body().bytes(), req.getCharset()));
            if (req.getBuffer() == 1) jsObject.setProperty("content", new JSUtils<String>().toArray(ctx, res.body().bytes()));
            if (req.getBuffer() == 2) jsObject.setProperty("content", Base64.encodeToString(res.body().bytes(), Base64.DEFAULT | Base64.NO_WRAP));
            if (req.getBuffer() == 3) jsObject.setProperty("content", res.body().bytes());
            return jsObject;
        } catch (Exception e) {
            return error(ctx);
        }
    }

    public static JSObject error(QuickJSContext ctx) {
        JSObject jsObject = ctx.createNewJSObject();
        JSObject jsHeader = ctx.createNewJSObject();
        jsObject.setProperty("headers", jsHeader);
        jsObject.setProperty("content", "");
        jsObject.setProperty("code", "");
        return jsObject;
    }

    private static Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).post(getPostBody(req, headers.get(HttpHeaders.CONTENT_TYPE))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).get().build();
        }
    }

    private static RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && req.getPostType().equals("json")) return getJsonBody(req);
        if (req.getData() != null && req.getPostType().equals("form")) return getFormBody(req);
        if (req.getData() != null && req.getPostType().equals("form-data")) return getFormDataBody(req);
        if (req.getBody() != null && contentType != null) return RequestBody.create(req.getBody(), MediaType.get(contentType));
        return RequestBody.create(new byte[0]);
    }

    private static RequestBody getJsonBody(Req req) {
        return RequestBody.create(req.getData().toString(), MediaType.get("application/json; charset=utf-8"));
    }

    private static RequestBody getFormBody(Req req) {
        FormBody.Builder formBody = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) formBody.add(key, params.get(key));
        return formBody.build();
    }

    private static RequestBody getFormDataBody(Req req) {
        String boundary = "--dio-boundary-" + new SecureRandom().nextInt(42949) + new SecureRandom().nextInt(67296);
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) builder.addFormDataPart(key, params.get(key));
        return builder.build();
    }

    private static void setHeader(QuickJSContext ctx, Response res, JSObject object) {
        for (Map.Entry<String, List<String>> entry : res.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) ctx.setProperty(object, entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) ctx.setProperty(object, entry.getKey(), new JSUtils<String>().toArray(ctx, entry.getValue()));
        }
    }
    
    public static void cancelByTag(Object tag) {
        try {
            if (client != null) {
                for (Call call : client.dispatcher().queuedCalls()) {
                    if (tag.equals(call.request().tag())) {
                        call.cancel();
                    }
                }
                for (Call call : client.dispatcher().runningCalls()) {
                    if (tag.equals(call.request().tag())) {
                        call.cancel();
                    }
                }
            }
            OkGo.getInstance().cancelTag(tag);
            cancelDefaultClient(tag);
        } catch (Exception e) {
        }
    }

    private static void cancelDefaultClient(Object tag) {
        OkHttpClient defaultClient = OkHttpHelper.getDefaultClient();
        if (defaultClient == null || tag == null) return;
        for (Call call : defaultClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : defaultClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }
}
