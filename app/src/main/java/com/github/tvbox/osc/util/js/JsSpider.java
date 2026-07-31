package com.github.tvbox.osc.util.js;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.whl.quickjs.wrapper.UriUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsSpider extends Spider {
    private static final byte BYTECODE_VERSION = 67;
    private static final String EMPTY_MODULE_CODE =
            "const empty = null;\n" +
            "export default empty;\n" +
            "export const JSEncrypt = empty;\n" +
            "export const NodeRSA = empty;\n" +
            "export const pako = empty;\n" +
            "export const JSON5 = empty;\n" +
            "export const mb = empty;\n" +
            "export const parse = empty;\n" +
            "export const stringify = empty;\n" +
            "export const inflate = empty;\n" +
            "export const deflate = empty;\n" +
            "export const gzip = empty;\n" +
            "export const ungzip = empty;\n" +
            "export const encrypt = empty;\n" +
            "export const decrypt = empty;";
    private final ExecutorService executor;
    private final Class<?> dex;
    private QuickJSContext ctx;
    private JSObject jsObject;
    private final String key;
    private final String api;
    private boolean cat;
    private byte[] emptyModuleBytecode;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public JsSpider(String key, String api, Class<?> cls) throws Exception {
        this.key = "J" + MD5.encode(key);
        this.executor = Executors.newSingleThreadExecutor();
        this.api = api;
        this.dex = cls;
        initializeJS();
    }
    
    public void cancelByTag() {
        Connect.cancelByTag("js_okhttp_tag");
    }

    private JSObject createObject() {
        return ctx.createNewJSObject();
    }

    private JSArray createArray() {
        return ctx.createNewJSArray();
    }

    private void set(JSObject object, String name, Object value) {
        ctx.setProperty(object, name, value);
    }

    private Object get(JSObject object, String name) {
        return ctx.getProperty(object, name);
    }

    private JSONArray toJsonArray(JSArray array) {
        return JSUtils.toJsonArray(array);
    }

    private void bind(JSObject target, Object receiver) {
        for (Method method : receiver.getClass().getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            String name = methodName(method);
            set(target, name, new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    try {
                        return method.invoke(receiver, args);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            });
        }
    }

    private String methodName(Method method) {
        return method.getName();
    }

    private void submit(Runnable runnable) {
        if (!destroyed.get()) executor.submit(runnable);
    }

    private <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    private Object call(String func, Object... args) {
        if (destroyed.get() || jsObject == null) return null;
        try {
            return submit(() -> Async.run(jsObject, func, args).get()).get();  // 等待 executor 线程完成 JS 调用
        } catch (InterruptedException | ExecutionException e) {
            LOG.i("Executor 提交或等待失败" + e);
            return null;
        }
    }

    private JSObject cfg(String ext) {
        JSObject cfg = createObject();
        set(cfg, "stype", 3);
        set(cfg, "skey", TextUtils.isEmpty(siteKey) ? key : siteKey);
        if (Json.invalid(ext)) set(cfg, "ext", ext);
        else set(cfg, "ext", (JSObject) ctx.parse(ext));
        return cfg;
    }

    @Override
    public void init(Context context, String extend) {
        try {
            if (cat) call("init", submit(() -> cfg(extend)).get());
            else call("init", Json.valid(extend) ? ctx.parse(extend) : extend);
        } catch (Exception e) {
        }
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            return (String) call("home", filter);
        } catch (Exception e) {
           return null;
        }
    }

    @Override
    public String homeVideoContent() {
        try {
            return (String) call("homeVod");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend)  {
        try {
            JSObject obj = submit(() -> new JSUtils<String>().toObj(ctx, extend)).get();
            return (String) call("category", tid, pg, filter, obj);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String detailContent(List<String> ids)  {
        try {
            return (String) call("detail", ids.get(0));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String searchContent(String key, boolean quick)  {
        try {
            return (String) call("search", key, quick);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String searchContent(String key, boolean quick, String pg)  {
        try {
            return (String) call("search", key, quick, pg);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSArray array = submit(() -> new JSUtils<String>().toArray(ctx, vipFlags)).get();
            return (String) call("play", flag, id, array);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String liveContent(String url) {
        try {
            return (String) call("live", url);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean manualVideoCheck()  {
        try {
            return (Boolean) call("sniffer");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isVideoFormat(String url) {
        try {
            return (Boolean) call("isVideo", url);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object[] proxyLocal(Map<String, String> params)  {
        try {
            return "catvod".equals(params.get("from")) ? proxy2(params) : proxy1(params);
        } catch (Exception E) {
            return new Object[0];
        }
    }

    @Override
    public String action(String action) {
        try {
            return (String) call("action", action);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) return;
        try {
            executor.submit(() -> {
                try {
                    jsObject = null;
                    if (ctx != null) ctx.destroy();
                } catch (Throwable th) {
                    LOG.i("echo-js-destroy-error " + th.getMessage());
                } finally {
                    executor.shutdown();
                }
            });
        } catch (Throwable th) {
            executor.shutdownNow();
        }
    }

    private static final String SPIDER_STRING_CODE = "import * as spider from '%s'\n\n" +
            "if (!globalThis.__JS_SPIDER__) {\n" +
            "    if (spider.__jsEvalReturn) {\n" +
            "        globalThis.req = http\n" +
            "        globalThis.__JS_SPIDER__ = spider.__jsEvalReturn()\n" +
            "        globalThis.__JS_SPIDER__.is_cat = true\n" +
            "    } else if (spider.default) {\n" +
            "        globalThis.__JS_SPIDER__ = typeof spider.default === 'function' ? spider.default() : spider.default\n" +
            "    }\n" +
            "}\n";
    
    private void initializeJS() throws Exception {
        submit(() -> {
            if (ctx == null) createCtx();
            if (dex != null) createDex();

            String content = FileUtils.loadModule(api);            
            if (isInvalidModuleContent(content)) {return null;}
            
            if (content.startsWith("//bb")) {
                cat = true;
                byte[] b = Base64.decode(content.replace("//bb",""), 0);
                try {
                    ctx.execute(byteFF(b));
                    ctx.evaluateModule(String.format(SPIDER_STRING_CODE, key + ".js") + "globalThis." + key + " = globalThis.__JS_SPIDER__;", "tv_box_root.js");
                } catch (Throwable th) {
                    LOG.i("echo-bytecode-execute-error " + api + ", msg=" + th.getMessage());
                    return null;
                }
                //ctx.execute(byteFF(b), key + ".js","__jsEvalReturn");
                //ctx.evaluate("globalThis." + key + " = __JS_SPIDER__;");
            } else {
                if (content.contains("__JS_SPIDER__")) {
                    content = content.replaceAll("__JS_SPIDER__\\s*=", "export default ");
                }
                String moduleExtName = "default";
                if (content.contains("__jsEvalReturn") && !content.contains("export default")) {
                    moduleExtName = "__jsEvalReturn";
                    cat = true;
                }
                try {
                    ctx.evaluateModule(content, api);
                    ctx.evaluateModule(String.format(SPIDER_STRING_CODE, api) + "globalThis." + key + " = globalThis.__JS_SPIDER__;", "tv_box_root.js");
                } catch (Throwable th) {
                    LOG.i("echo-evaluateModule-error " + api + ", msg=" + th.getMessage());
                    return null;
                }
                //ctx.evaluateModule(content, api, moduleExtName);
                //ctx.evaluate("globalThis." + key + " = __JS_SPIDER__;");                
            }
            jsObject = (JSObject) get(ctx.getGlobalObject(), key);
            if (jsObject != null) jsObject.hold();
            return null;
        }).get();
    }

    public static byte[] byteFF(byte[] bytes) {
        byte[] newBt = new byte[bytes.length - 4];
        newBt[0] = BYTECODE_VERSION;
        System.arraycopy(bytes, 5, newBt, 1, bytes.length - 5);
        return newBt;
    }

    private void createCtx() {
        ctx = QuickJSContext.create();
        emptyModuleBytecode = ctx.compileModule(EMPTY_MODULE_CODE, "empty.js");
        ctx.setModuleLoader(new QuickJSContext.BytecodeModuleLoader() {
            @Override
            public byte[] getModuleBytecode(String moduleName) {
                String ss = FileUtils.loadModule(moduleName);
                if (isInvalidModuleContent(ss)) {
                    return compileEmptyModule(moduleName);
                }
                if (ss.startsWith("//DRPY")) {
                    try {
                        byte[] bytes = bytecode(Base64.decode(ss.replace("//DRPY",""), Base64.URL_SAFE));
                        return bytes == null ? compileEmptyModule(moduleName) : bytes;
                    } catch (Throwable th) {
                        LOG.i("echo-bytecode-module-error " + moduleName + ", msg=" + th.getMessage());
                        return compileEmptyModule(moduleName);
                    }
                } else if (ss.startsWith("//bb")) {
                    try {
                        byte[] b = Base64.decode(ss.replace("//bb",""), 0);
                        return byteFF(b);
                    } catch (Throwable th) {
                        LOG.i("echo-bytecode-module-error " + moduleName + ", msg=" + th.getMessage());
                        return compileEmptyModule(moduleName);
                    }
                } else {
                    return compileModule(moduleName, ss);
                }
            }

            @Override
            public String moduleNormalizeName(String moduleBaseName, String moduleName) {
                return UriUtil.resolve(moduleBaseName, moduleName);
            }
        });
        ctx.setConsole(new QuickJSContext.Console() {
            @Override
            public void log(String s) {
                LOG.i("echo-QuJs " + s);
            }
            @Override
            public void info(String s) {
                LOG.i("echo-QuJs " + s);
            }
            @Override
            public void warn(String s) {
                LOG.i("echo-QuJs " + s);
            }
            @Override
            public void error(String s) {
                LOG.i("echo-QuJs " + s);
            }
        });

        bind(ctx.getGlobalObject(), new Global(ctx, executor));

        JSObject local = createObject();
        set(ctx.getGlobalObject(), "local", local);
        bind(local, new local());

        String net = FileUtils.loadModule("net.js");
        if (!isInvalidModuleContent(net)) ctx.getGlobalObject().getContext().evaluate(net);
        preloadTemplate();
    }

    private byte[] compileEmptyModule(String moduleName) {
        LOG.i("echo-getModuleBytecode empty :" + moduleName);
        return emptyModuleBytecode;
    }

    private static byte[] bytecode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        if (bytes[0] == BYTECODE_VERSION) return bytes;
        LOG.i("echo-bytecode-version-mismatch actual=" + bytes[0] + ", expected=" + BYTECODE_VERSION);
        return null;
    }

    private byte[] compileModule(String moduleName, String content) {
        try {
            if (moduleName != null && moduleName.contains("cheerio.min.js")) {
                byte[] bytecode = ctx.compileModule(content, "cheerio.min.js");
                FileUtils.setCacheByte("cheerio.min", bytecode);
                return bytecode;
            } else if (moduleName != null && moduleName.contains("crypto-js.js")) {
                byte[] bytecode = ctx.compileModule(content, "crypto-js.js");
                FileUtils.setCacheByte("crypto-js", bytecode);
                return bytecode;
            }
            return ctx.compileModule(content, moduleName);
        } catch (Throwable th) {
            LOG.i("echo-compileModule-error " + moduleName + ", msg=" + th.getMessage());
            return compileEmptyModule(moduleName);
        }
    }

    private boolean isInvalidModuleContent(String content) {
        if (TextUtils.isEmpty(content)) return true;
        String trim = content.trim();
        if (trim.startsWith("\uFEFF")) trim = trim.substring(1).trim();
        String lower = trim.toLowerCase();
        return lower.startsWith("<")
                || lower.startsWith("{\"code\":404")
                || lower.startsWith("404")
                || lower.startsWith("not found");
    }

    private void preloadTemplate() {
        try {
            String template = "import tpl from '模板.js';\n"
                    + "globalThis.muban = tpl.muban;\n"
                    + "globalThis.getMubans = tpl.getMubans;";
            ctx.evaluateModule(template, "tv_box_template.js");
        } catch (Throwable th) {
            LOG.i("echo-preloadTemplate-error " + th.getMessage());
        }
    }

    private void createDex() {
        try {
            JSObject obj = createObject();
            Class<?> clz = dex;
            Class<?>[] classes = clz.getDeclaredClasses();
            set(ctx.getGlobalObject(), "jsapi", obj);
            if (classes.length == 0) invokeSingle(clz, obj);
            if (classes.length >= 1) invokeMultiple(clz, obj);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void invokeSingle(Class<?> clz, JSObject jsObj) throws Throwable {
        invoke(clz, jsObj, clz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx));
    }

    private void invokeMultiple(Class<?> clz, JSObject jsObj) throws Throwable {
        for (Class<?> subClz : clz.getDeclaredClasses()) {
            Object javaObj = subClz.getDeclaredConstructor(clz).newInstance(clz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx));
            JSObject subObj = createObject();
            invoke(subClz, subObj, javaObj);
            set(jsObj, subClz.getSimpleName(), subObj);
        }
    }

    private void invoke(Class<?> clz, JSObject jsObj, Object javaObj) {
        for (Method method : clz.getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            invoke(jsObj, method, javaObj);
        }
    }

    private void invoke(JSObject jsObj, Method method, Object javaObj) {
        set(jsObj, methodName(method), new JSCallFunction() {
            @Override
            public Object call(Object... objects) {
                try {
                    return method.invoke(javaObj, objects);
                } catch (Throwable e) {
                    return null;
                }
            }
        });
    }

    private Object[] proxy1(Map<String, String> params) throws Exception {
        JSObject obj = submit(() -> new JSUtils<String>().toObj(ctx, params)).get();
        JSArray proxy = (JSArray) call("proxy", obj);
        String json = submit(proxy::stringify).get();
        JSONArray array = new JSONArray(json);
        Map<String, String> headers = array.length() > 3 ? Json.toMap(array.optString(3)) : null;
        boolean base64 = array.length() > 4 && array.optInt(4) == 1;
        Object[] result = new Object[4];
        result[0] = array.optInt(0);
        result[1] = array.optString(1);
        result[2] = getStream(array.opt(2), base64);
        result[3] = headers;
        return result;
    }
    
    private Object[] proxy2(Map<String, String> params) throws Exception {
        String url = params.get("url");
        String header = params.get("header");
        JSArray array = submit(() -> new JSUtils<String>().toArray(ctx, Arrays.asList(url.split("/")))).get();
        Object object = submit(() -> ctx.parse(header)).get();
        String proxy = (String) call("proxy", array, object);
        Res res = Res.objectFrom(proxy);
        Object[] result = new Object[3];
        result[0] = res.getCode();
        result[1] = res.getContentType();
        result[2] = res.getStream();
        return result;
    }
    
    private ByteArrayInputStream getStream(Object o, boolean base64) {
        if (o instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) o);
        } else {
            String content = o.toString();
            if (base64 && content.contains("base64,")) content = content.split("base64,")[1];
            return new ByteArrayInputStream(base64 ? Base64.decode(content, Base64.DEFAULT | Base64.NO_WRAP) : content.getBytes());
        }
    }
}
