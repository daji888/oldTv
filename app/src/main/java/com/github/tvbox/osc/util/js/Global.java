package com.github.tvbox.osc.util.js;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.github.tvbox.osc.server.ControlManager;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Global {
    private QuickJSContext runtime;
    public ExecutorService executor;
    private final Timer timer;

    public Global(ExecutorService executor) {
        this.executor = executor;
        this.timer = new Timer();
    }

    @Keep
    @JSMethod
    public String getProxy(boolean local) {
        return ControlManager.get().getAddress(local) + "proxy?do=js";
    }

    @Keep
    @JSMethod
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, JSObject headers) {
        return getProxy(true) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncoder.encode(headers.stringify()) + "&url=" + URLEncoder.encode(url);
    }

    @Keep
    @JSMethod
    public String joinUrl(String parent, String child) {
        return HtmlParser.joinUrl(parent, child);
    }

    @Keep
    @JSMethod
    public String pd(String html, String rule, String add_url) {
        return HtmlParser.parseDomForUrl(html, rule, add_url);
    }

    @Keep
    @JSMethod
    public String pdfh(String html, String rule) {
        return HtmlParser.parseDomForUrl(html, rule, "");
    }

    @Keep
    @JSMethod
    public JSArray pdfa(String html, String rule) {

        return JSUtils.toArray(runtime, HtmlParser.parseDomForArray(html, rule));
    }

    @Keep
    @JSMethod
    public JSArray pdfla(String html, String p1, String list_text, String list_url, String add_url) {
        return JSUtils.toArray(runtime, HtmlParser.parseDomForList(html, p1, list_text, list_url, add_url));
    }
    
    @Keep
    @JSMethod
    public String s2t(String text) {
        try {
            return Trans.s2t(false, text);
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @JSMethod
    public String t2s(String text) {
        try {
            return Trans.t2s(false, text);
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @JSMethod
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        String result = Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);
        //LOG.e("aesX",String.format("mode:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:%s\niv:%s\ninput:\n%s\nresult:\n%s", mode, encrypt, inBase64, outBase64, key, iv, input, result));
        return result;
    }

    @Keep
    @JSMethod
    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        String result = Crypto.rsa(pub, encrypt, input, inBase64, key, outBase64);
        //LOG.e("aesX",String.format("mode:%s\npub:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:\n%s\ninput:\n%s\nresult:\n%s", mode, pub, encrypt, inBase64, outBase64, key, input, result));
        return result;
    }

    private JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(options.stringify());
            Response res = Connect.to(url, req).execute();
            return Connect.success(runtime, req, res);
        } catch (Exception e) {
            return Connect.error(runtime);
        }
    }

    @Keep
    @JSMethod
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        Req req = Req.objectFrom(options.stringify());
        Connect.to(url, req).enqueue(getCallback(complete, req));
        return null;
    }

    @Keep
    @JSMethod
    public void setTimeout(JSFunction func, Integer delay) {
        func.hold();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!executor.isShutdown()) executor.submit(() -> {func.call();});
            }
        }, delay);
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                executor.submit(() -> {
                    complete.call(Connect.success(runtime, req, res));
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                executor.submit(() -> {
                    complete.call(Connect.error(runtime));
                });
            }
        };
    }

}
