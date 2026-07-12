package com.github.tvbox.osc.util;

import android.util.Log;

import com.github.tvbox.osc.event.LogEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class LOG {
    private static String TAG = "TVBox-runtime";
    private static final int MAX_LOG_LENGTH = 3000;

    public static void e(Throwable t) {
        Log.e(TAG, t.getMessage(), t);
        EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", TAG) + Log.getStackTraceString(t)));
    }

    public static void e(String tag, Throwable t) {
        Log.e(tag, t.getMessage(), t);
        EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", tag) + Log.getStackTraceString(t)));
    }

    public static void e(String msg) {
        Log.e(TAG, "" + msg);
        EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", TAG) + msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", tag) + msg));
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
        EventBus.getDefault().post(new LogEvent(String.format("【I/%s】=>>>", TAG) + msg));
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        EventBus.getDefault().post(new LogEvent(String.format("【I/%s】=>>>", tag) + msg));
    }

    public static void longI(String prefix, String msg) {
        longLog(Log.INFO, prefix, msg);
    }

    public static void longE(String prefix, String msg) {
        longLog(Log.ERROR, prefix, msg);
    }

    private static void longLog(int priority, String prefix, String msg) {
        String text = msg == null ? "null" : msg;
        String title = prefix == null ? "" : prefix;
        int length = text.length();
        if (length <= MAX_LOG_LENGTH) {
            Log.println(priority, TAG, title + text);
            return;
        }
        int count = (length + MAX_LOG_LENGTH - 1) / MAX_LOG_LENGTH;
        for (int i = 0; i < count; i++) {
            int start = i * MAX_LOG_LENGTH;
            int end = Math.min(start + MAX_LOG_LENGTH, length);
            Log.println(priority, TAG, title + "[" + (i + 1) + "/" + count + "] " + text.substring(start, end));
        }
    }
}
