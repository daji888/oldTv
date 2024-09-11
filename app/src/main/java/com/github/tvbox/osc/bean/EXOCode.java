package com.github.tvbox.osc.bean;

import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.util.LinkedHashMap;

/**
 * @author pj567
 * @date :2021/3/8
 * @description:
 */
public class EXOCode {
    private String name;
    private LinkedHashMap<String> option;
    private boolean selected;

    public void selected(boolean selected) {
        this.selected = selected;
        if (selected) {
            Hawk.put(HawkConfig.EXO_CODEC, name);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkedHashMap<String> getOption() {
        return option;
    }

    public void setOption(LinkedHashMap<String> option) {
        this.option = option;
    }
}
