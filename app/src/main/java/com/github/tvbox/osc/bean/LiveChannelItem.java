package com.github.tvbox.osc.bean;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
public class LiveChannelItem {
    /**
     * channelIndex : 频道索引号
     * channelName : 频道名称
     * channelNum : 频道数
     * channelSourceNames : 频道源名称
     * channelUrls : 频道源地址
     * sourceIndex : 频道源索引
     * sourceNum : 频道源总数
     */
    private int channelIndex;
    private int channelNum;
    public String channelName;
    private List<String> channelSourceNames = new ArrayList<>();
    public List<String> channelUrls = new ArrayList<>();
    public int sourceIndex = 0;
    public int sourceNum = 0;
    public boolean include_back = false;
    public JsonObject catchupConfig = new JsonObject();
    public String logo = "";
    public String useragent = "";

    public void setinclude_back(boolean include_back) {
        this.include_back = include_back;
    }

    public boolean getinclude_back() {
        return include_back;
    }

    public void setChannelIndex(int channelIndex) {
        this.channelIndex = channelIndex;
    }

    public int getChannelIndex() {
        return channelIndex;
    }

    public void setChannelNum(int channelNum) {
        this.channelNum = channelNum;
    }

    public int getChannelNum() {
        return channelNum;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public List<String> getChannelUrls() {
        return channelUrls != null ? channelUrls : Collections.emptyList();
    }

    public void setChannelUrls(List<String> channelUrls) {
        if (channelUrls == null) {
            this.channelUrls = new ArrayList<>();
            this.sourceNum = 0;
        } else {
            this.channelUrls = channelUrls;
            this.sourceNum = this.channelUrls.size();
        }
        if (this.sourceIndex >= this.sourceNum) {
            this.sourceIndex = this.sourceNum > 0 ? 0 : -1;
        }
    }

    public String getUrl() {
        if (channelUrls == null || channelUrls.isEmpty() || sourceIndex < 0 || sourceIndex >= channelUrls.size()) {
            return null;
        }
        return channelUrls.get(sourceIndex);
    }

    public List<String> getChannelSourceNames() {
        return channelSourceNames != null ? channelSourceNames : Collections.emptyList();
    }

    public void setChannelSourceNames(List<String> channelSourceNames) {
        if (channelSourceNames == null) {
            this.channelSourceNames = new ArrayList<>();
        } else {
            this.channelSourceNames = channelSourceNames;
        }
    }

    public String getSourceName() {
        if (channelSourceNames == null || channelSourceNames.isEmpty() || sourceIndex < 0 || sourceIndex >= channelSourceNames.size()) {
            return null;
        }
        return channelSourceNames.get(sourceIndex);
    }

    public void setSourceIndex(int sourceIndex) {
        if (sourceNum > 0) {
            this.sourceIndex = sourceIndex % sourceNum;
            if (this.sourceIndex < 0) this.sourceIndex += sourceNum;
        } else {
            this.sourceIndex = 0;
        }
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public int getSourceNum() {
        return sourceNum;
    }

    public void preSource() {
        sourceIndex--;
        if (sourceIndex < 0) sourceIndex = sourceNum - 1;
    }
    
    public void nextSource() {
        sourceIndex++;
        if (sourceIndex == sourceNum) sourceIndex = 0;
    }

    public void addCatchupInfo(String type, String source, String replace) {
        if (type != null && !type.isEmpty()) {
            catchupConfig.addProperty("type", type);
        }
        if (source != null && !source.isEmpty()) {
            catchupConfig.addProperty("source", source);
        }
        if (replace != null && !replace.isEmpty()) {
            catchupConfig.addProperty("replace", replace);
        }
    }

    public boolean hasCatchup() {
        return catchupConfig.has("type") || catchupConfig.has("source");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveChannelItem that = (LiveChannelItem) o;
        String thisUrl = this.getUrl();
        String thatUrl = that.getUrl();
        return Objects.equals(channelName, that.channelName)
                && Objects.equals(thisUrl, thatUrl);
    }
 
     @Override
     public int hashCode() {
        String url = this.getUrl();
        return Objects.hash(channelName, url);
    }
}
