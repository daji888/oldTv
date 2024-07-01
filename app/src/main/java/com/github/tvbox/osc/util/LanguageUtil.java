package com.github.tvbox.osc.util;

import android.app.Application;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.Setting;
import com.yariksoffice.lingver.Lingver;

import java.util.Locale;

public class LanguageUtil {

    public static void init(Application application) {
        Lingver.init(application, getLocale(Setting.getLanguage()));
    }

    public static void setLocale(Locale locale) {
        Lingver.getInstance().setLocale(App.getInstance(), locale);
    }

    public static int locale() {
        if (!Locale.getDefault().getLanguage().equals("zh")) return 0;
        if (Locale.getDefault().getCountry().equals("CN")) return 1;
        return 2;
    }

    public static Locale getLocale(int lang) {
        if (lang == 1) return Locale.SIMPLIFIED_CHINESE;
        else if (lang == 2) return Locale.TRADITIONAL_CHINESE;
        else return Locale.ENGLISH;
    }

}
