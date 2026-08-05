package com.github.tvbox.osc.util.js;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import androidx.media3.common.util.UriUtil;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import dalvik.system.DexClassLoader;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

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
    private final DexClassLoader dex;
    private QuickJSContext ctx;
    private JSObject jsObject;
    private final String key;
    private final String api;
    private Global global;
    private boolean cat;
    private byte[] emptyModuleBytecode;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public JsSpider(String key, String api, DexClassLoader dex) throws Exception {
        this.key = "J" + MD5.encode(key);
        this.executor = Executors.newSingleThreadExecutor();
        this.api = api;
        this.dex = dex;
    }
    
    public void cancelByTag() {
        Connect.cancelByTag("js_okhttp_tag");
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

    private Object getExt(String ext) {
        if (!cat) return Json.valid(ext) ? ctx.parse(ext) : ext;
        JSObject obj = ctx.createNewJSObject();
        obj.setProperty("stype", 3);
        obj.setProperty("skey", TextUtils.isEmpty(siteKey) ? key : siteKey);
        if (!Json.valid(ext)) obj.setProperty("ext", ext);
        else obj.setProperty("ext", (JSObject) ctx.parse(ext));
        return obj;
    }

    @Override
    public void init(Context context, String extend) {
        try {
            initializeJS();
            call("init", submit(() -> getExt(extend)).get());
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
        executor.shutdown();
        try {
            try {
                call("destroy");
            } catch (Throwable e) {
                LOG.e("call destroy error", e);
            }
            Future<?> future = null;
            try {
                future = executor.submit(() -> {
                    try {
                        if (global != null) global.destroy();
                        if (jsObject != null) jsObject.release();
                        if (ctx != null) ctx.destroy();
                    } catch (Throwable th) {
                        LOG.e("releaseJS error", th);
                    }
                    return null;
                });
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.e("Wait for release task error", e);
                if (future != null) future.cancel(true);
            }
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.e("Executor did not terminate in time, shutting down now");
                    executor.shutdownNow();
                    try {
                        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                            LOG.e("Executor failed to terminate completely");
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.e("Final awaitTermination interrupted", ie);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.e("AwaitTermination interrupted during destroy", e);
                executor.shutdownNow();
            }
        } finally {
            global = null;
            jsObject = null;
            ctx = null;
            System.gc();
        }
    }

    private static final String SPIDER_STRING_CODE = "import * as spider from '%s'\n\n" +
            "if (!globalThis.__JS_SPIDER__) {\n" +
            "    if (spider.__jsEvalReturn) {\n" +
            "        globalThis.req = http\n" +
            "        globalThis.__JS_SPIDER__ = spider.__jsEvalReturn()\n" +
            "    } else if (spider.default) {\n" +
            "        globalThis.__JS_SPIDER__ = typeof spider.default === 'function' ? spider.default() : spider.default\n" +
            "    }\n" +
            "}\n";
    
    private void initializeJS() throws Exception {
        submit(() -> {
            createCtx();
            createFun();
            String content = FileUtils.loadModule(api);            
            if (isInvalidModuleContent(content)) { return null; }
            createObj();
            return null;
        }).get();
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
                        byte[] bytes = bytecode(Base64.decode(ss.replace("//DRPY", ""), Base64.URL_SAFE));
                        return bytes == null ? compileEmptyModule(moduleName) : bytes;
                    } catch (Throwable th) {
                        LOG.i("echo-bytecode-module-error " + moduleName + ", msg=" + th.getMessage());
                        return compileEmptyModule(moduleName);
                    }
                } else if (ss.startsWith("//bb")) {
                    try {
                        byte[] b = Base64.decode(ss.replace("//bb", ""), 0);
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

        String net = FileUtils.loadModule("net.js");
        if (!isInvalidModuleContent(net)) ctx.evaluate(net);
        ctx.getGlobalObject().setProperty("local", local.class);
        preloadTemplate();
    }

    private void createFun() {
        try {
            global = new Global(ctx, executor);
            Class<?> clz = dex.loadClass("com.github.catvod.js.Function");
            clz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx);
        } catch (Throwable ignored) {
        }
    }

    private void createObj() {
        String spider = "__JS_SPIDER__";
        String global = "globalThis." + spider;
        String content = FileUtils.loadModule(api);
        boolean bb = content.startsWith("//bb");
        cat = bb || content.contains("__jsEvalReturn");
        if (!bb) ctx.evaluateModule(content.replace(spider, global), api);
        ctx.evaluateModule(String.format(SPIDER_STRING_CODE, api));
        jsObject = (JSObject) ctx.getProperty(ctx.getGlobalObject(), spider);
        if (jsObject != null) jsObject.hold();
    }

    public static byte[] byteFF(byte[] bytes) {
        byte[] newBt = new byte[bytes.length - 4];
        newBt[0] = BYTECODE_VERSION;
        System.arraycopy(bytes, 5, newBt, 1, bytes.length - 5);
        return newBt;
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
