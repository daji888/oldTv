package com.github.tvbox.osc.bean;

import java.util.ArrayList;

public class LiveSourceBean {
    private String name;
    private String ua;
    private int type;   // 0 xml 1 json 3 Spider
    private String LiveUrl;
    private String EpgUrl;
    private String LogeUrl;
    private String ext; // 扩展数据
    private String jar; // 自定义jar
    private int playerType; // 0 system 1 ikj 2 exo 10 mxplayer -1 以参数设置页面的为准
    private String clickSelector; // 需要点击播放的嗅探站点selector   ddrk.me;#id

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public void setLiveUrl(String LiveUrl) {
        this.LiveUrl = LiveUrl;
    }

    public String getLiveUrl() {
        return LiveUrl;
    }

    public void setEpgUrl(String EpgUrl) {
        this.EpgUrl = EpgUrl;
    }

    public String getEpgUrl() {
        return EpgUrl;
    }

    public void setLogeUrl(String LogeUrl) {
        this.LogeUrl = LogeUrl;
    }

    public String getLogeUrl() {
        return LogeUrl;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public int getPlayerType() { return playerType; }

    public void setPlayerType(int playerType) { this.playerType = playerType; }

    public String getClickSelector() { return clickSelector; }

    public void setClickSelector(String clickSelector) { this.clickSelector = clickSelector; }

}
