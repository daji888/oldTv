package com.github.tvbox.osc.util.js;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.quickjs.JSArray;
import com.github.tvbox.quickjs.JSCallFunction;
import com.github.tvbox.quickjs.JSModule;
import com.github.tvbox.quickjs.JSObject;
import com.github.tvbox.quickjs.QuickJSContext;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JSEngine {
    private static final String TAG = "JSEngine";

    static JSEngine instance = null;

    public static JSEngine getInstance() {
        if (instance == null)
            instance = new JSEngine();
        return instance;
    }

    public class JSThread {
        private QuickJSContext jsContext;
        private Handler handler;
        private Thread thread;
        private volatile byte retain;

        public QuickJSContext getJsContext() {
            return jsContext;
        }

        public JSObject getGlobalObj() {
            return jsContext.getGlobalObject();
        }

        public <T> T post(Event<T> event) throws Throwable {
            if ((thread != null && thread.isInterrupted())) {
                Log.e("QuickJS", "QuickJS is released");
                return null;
            }
            if (Thread.currentThread() == thread) {
                return event.run(jsContext, getGlobalObj());
            }
            if (handler == null) {
                return event.run(jsContext, getGlobalObj());
            }
            Object[] result = new Object[2];
            RuntimeException[] errors = new RuntimeException[1];
            handler.post(() -> {
                try {
                    result[0] = event.run(jsContext, getGlobalObj());
                } catch (RuntimeException e) {
                    errors[0] = e;
                }
                synchronized (result) {
                    result[1] = true;
                    result.notifyAll();
                }
            });
            synchronized (result) {
                try {
                    if (result[1] == null) {
                        result.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (errors[0] != null) {
                throw errors[0];
            }
            return (T) result[0];
        }

        public void postVoid(Event<Void> event) throws Throwable {
            postVoid(event, true);
        }

        public void postVoid(Event<Void> event, boolean block) throws Throwable {
            if ((thread != null && thread.isInterrupted())) {
                Log.e("QuickJS", "QuickJS is released");
                return;
            }
            if (Thread.currentThread() == thread) {
                event.run(jsContext, getGlobalObj());
                return;
            }
            if (handler == null) {
                event.run(jsContext, getGlobalObj());
                return;
            }
            Object[] result = new Object[2];
            RuntimeException[] errors = new RuntimeException[1];
            handler.post(() -> {
                try {
                    event.run(jsContext, getGlobalObj());
                } catch (RuntimeException e) {
                    errors[0] = e;
                }
                if (block) {
                    synchronized (result) {
                        result[1] = true;
                        result.notifyAll();
                    }
                }
            });
            if (block) {
                synchronized (result) {
                    try {
                        if (result[1] == null) {
                            result.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (errors[0] != null) {
                    throw errors[0];
                }
            }
        }

        public void init() {
            initConsole();
            initLocalStorage();
        }

        void initConsole() {
            jsContext.evaluate("var console = {};");
            JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("console");
            console.setProperty("log", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    StringBuilder b = new StringBuilder();
                    for (Object o : args) {
                        b.append(o == null ? "null" : o.toString());
                    }
                    System.out.println(TAG + " >>> " + b.toString());
                    return null;
                }
            });
        }

        void initLocalStorage() {
            jsContext.evaluate("var local = {};");
            JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("local");
            console.setProperty("get", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    return sharedPreferences.getString(args[1].toString(), "");
                }
            });
            console.setProperty("set", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString(args[1].toString(), args[2].toString()).commit();
                    return null;
                }
            });
            console.setProperty("delete", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    sharedPreferences.edit().remove(args[1].toString()).commit();
                    return null;
                }
            });
        }

        



    
    
}
