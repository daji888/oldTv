package com.whl.quickjs.wrapper;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JSUtils {

    public static boolean isEmpty(Object obj) {
        if (obj == null) return true;
        else if (obj instanceof CharSequence) return ((CharSequence) obj).length() == 0;
        else if (obj instanceof Collection) return ((Collection) obj).isEmpty();
        else if (obj instanceof Map) return ((Map) obj).isEmpty();
        else if (obj.getClass().isArray()) return Array.getLength(obj) == 0;

        return false;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static JSArray toArray(QuickJSContext ctx, List<String> items) {
        JSArray array = ctx.createNewJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) array.set(items.get(i), i);
        return array;
    }

    public static JSArray toArray(QuickJSContext ctx, byte[] bytes) {
        JSArray array = ctx.createNewJSArray();
        if (bytes == null || bytes.length == 0) return array;
        for (int i = 0; i < bytes.length; i++) array.set((int) bytes[i], i);
        return array;
    }

    public static JSArray toArray(QuickJSContext ctx, String[] arrays) {
        JSArray array = ctx.createNewJSArray();
        if (arrays == null || arrays.length == 0) return array;
        for (int i = 0; i < arrays.length; i++) array.set(arrays[i], i);
        return array;
    }

    public static JSObject toObj(QuickJSContext ctx, Map<String, String> map) {
        JSObject obj = ctx.createNewJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) {
            obj.setProperty(s, map.get(s));
        }
        return obj;
    }
}
