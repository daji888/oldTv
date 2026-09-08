package com.github.tvbox.osc.util;

import android.content.res.AssetManager;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.base.App;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Hashtable;

import okhttp3.Call;
import okhttp3.Callback;

import org.json.JSONObject;

public class EpgNameFuzzyMatch {

    private static JsonObject epgNameDoc = null;
    private static Hashtable<String, Object> hsEpgName = new Hashtable<>();

    public static void init() {
        if (epgNameDoc != null)
            return;

        try {
            AssetManager assetManager = App.getInstance().getAssets(); //获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            InputStreamReader inputStreamReader = new InputStreamReader(assetManager.open("Roinlong_Epg.json"), "UTF-8"); //使用IO流读取json文件内容
            BufferedReader br = new BufferedReader(inputStreamReader);//使用字符高效流
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();
            inputStreamReader.close();
            if (!builder.toString().isEmpty()) {
                JsonObject jsonObj = new Gson().fromJson(builder.toString(), (Type) JsonObject.class); // 从builder中读取了json中的数据。
                epgNameDoc = jsonObj;
                hasAddData(epgNameDoc);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //上述两种途径都失败后,读取网络自定义文件中的内容
        HashMap<String, String> epgHeaders = new HashMap<>();
        epgHeaders.put("User-Agent", UA.random());
        OkHttp.newCall("http://www.baidu.com/maotv/epg.json", epgHeaders).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    String pageStr = response.body().string();
                    JsonObject infoJson = new Gson().fromJson(pageStr, (Type) JsonObject.class);
                    epgNameDoc = infoJson;
                    hasAddData(epgNameDoc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void hasAddData(JsonObject epgNameDoc) {
        for (JsonElement opt : epgNameDoc.get("epgs").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            String name = obj.get("name").getAsString().trim();
            String[] names = name.split(",");
            for (String string : names) {
                hsEpgName.put(string, obj);
            }
        }
    }

    public static JsonObject getEpgNameInfo(String channelName) {
        if (hsEpgName.containsKey(channelName)) {
            JsonObject obj = (JsonObject) hsEpgName.get(channelName);
            return obj;
        }
        return null;
    }
}
