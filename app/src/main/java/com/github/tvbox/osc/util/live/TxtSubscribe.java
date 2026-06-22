package com.github.tvbox.osc.util.live;

import com.github.tvbox.osc.bean.LiveChannelItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtSubscribe {
    private static final Pattern NAME_PATTERN = Pattern.compile(".*,(.+?)$");
    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"(.*?)\"");
    private static final Pattern CATCHUP_PATTERN = Pattern.compile("catchup=\"(.*?)\"");
    private static final Pattern CATCHUP_SOURCE_PATTERN = Pattern.compile("catchup-source=\"(.*?)\"");
    private static final Pattern CATCHUP_REPLACE_PATTERN = Pattern.compile("catchup-replace=\"(.*?)\"");
    private static final Pattern LOGO_PATTERN = Pattern.compile("tvg-logo=\"(.*?)\"");
    private static final Pattern USER_AGENT_PATTERN = Pattern.compile("http-user-agent=\"(.*?)\"");

    public static void parse(LinkedHashMap<String, ArrayList<LiveChannelItem>> linkedHashMap, String str) {
        if (str == null || str.isEmpty()) return;
        linkedHashMap.clear();
        String cleanStr = str.trim();
        if (cleanStr.startsWith("\uFEFF")) {
            cleanStr = cleanStr.substring(1);
        }
        if (cleanStr.startsWith("#EXTM3U")) {
            parseM3u(linkedHashMap, cleanStr);
        } else {
            parseTxt(linkedHashMap, cleanStr);
        }
    }

    private static void parseM3u(LinkedHashMap<String, ArrayList<LiveChannelItem>> linkedHashMap, String str) {
        if (str == null || str.isEmpty()) return;
        String globalCatchupType = "";
        String globalCatchupSource = "";
        String globalCatchupReplace = "";
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(str))) {
            String line;
            String currentName = null;
            String currentGroup = "未分组";
            String currentLogo = "";
            String currentUseragent = "";
            String lineCatchupType = null;
            String lineCatchupSource = null;
            String lineCatchupReplace = null;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#EXTM3U")) {
                    globalCatchupType = getStrByRegex(CATCHUP_PATTERN, line);
                    globalCatchupSource = getStrByRegex(CATCHUP_SOURCE_PATTERN, line);
                    globalCatchupReplace = getStrByRegex(CATCHUP_REPLACE_PATTERN, line);
                    continue;
                }
                if (line.startsWith("#EXTINF")) {
                    Matcher channelNameMatcher = NAME_PATTERN.matcher(line);
                    if (channelNameMatcher.find()) {
                        currentName = channelNameMatcher.group(1).trim();
                    } else {
                        currentName = "未知频道";
                    }
                    Matcher groupMatcher = GROUP_PATTERN.matcher(line);
                    if (groupMatcher.find()) {
                        String grp = groupMatcher.group(1).trim();
                        if (!grp.isEmpty()) {
                            currentGroup = grp;
                        }
                    }
                    Matcher logoMatcher = LOGO_PATTERN.matcher(line);
                    if (logoMatcher.find()) {
                        currentLogo = logoMatcher.group(1).trim();
                    } else {
                        currentLogo = "";
                    }
                    Matcher useragentMatcher = USER_AGENT_PATTERN.matcher(line);
                    if (useragentMatcher.find()) {
                        currentUseragent = useragentMatcher.group(1).trim();
                    } else {
                        currentUseragent = "";
                    }
                    lineCatchupType = getStrByRegex(CATCHUP_PATTERN, line);
                    lineCatchupSource = getStrByRegex(CATCHUP_SOURCE_PATTERN, line);
                    lineCatchupReplace = getStrByRegex(CATCHUP_REPLACE_PATTERN, line);
                    continue;
                }
                if (currentName != null && isUrl(line)) {
                    String url = line.trim();
                    String finalType = lineCatchupType != null && !lineCatchupType.isEmpty() ? lineCatchupType : globalCatchupType;
                    String finalSource = lineCatchupSource != null && !lineCatchupSource.isEmpty() ? lineCatchupSource : globalCatchupSource;
                    String finalReplace = lineCatchupReplace != null && !lineCatchupReplace.isEmpty() ? lineCatchupReplace : globalCatchupReplace;
                    ArrayList<LiveChannelItem> channelArrayList = linkedHashMap.computeIfAbsent(currentGroup, k -> new ArrayList<>());
                    LiveChannelItem channelItem = null;
                    for (LiveChannelItem existing : channelArrayList) {
                        if (existing.channelName.equals(currentName)) {
                            channelItem = existing;
                            break;
                        }
                    }
                    if (channelItem == null) {
                        channelItem = new LiveChannelItem();
                        channelItem.channelName = currentName;
                        channelItem.logo = currentLogo;
                        channelItem.useragent = currentUseragent;
                        channelItem.addCatchupInfo(finalType, finalSource, finalReplace);
                        channelArrayList.add(channelItem);
                    } else {
                        if (!currentLogo.isEmpty() && channelItem.logo.isEmpty()) {
                            channelItem.logo = currentLogo;
                        }
                        if (!currentUseragent.isEmpty() && channelItem.useragent.isEmpty()) {
                            channelItem.useragent = currentUseragent;
                        }
                        if (!channelItem.hasCatchup() && (finalType != null || finalSource != null)) {
                             channelItem.addCatchupInfo(finalType, finalSource, finalReplace);
                        }
                    }
                    if (!channelItem.channelUrls.contains(url)) {
                        channelItem.channelUrls.add(url);
                    }
                    currentName = null; 
                    lineCatchupType = null;
                    lineCatchupSource = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseTxt(LinkedHashMap<String, ArrayList<LiveChannelItem>> linkedHashMap, String str) {
        if (str == null || str.isEmpty()) return;
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(str))) {
            String readLine;
            String currentGroup = "未分组";
            while ((readLine = bufferedReader.readLine()) != null) {
                readLine = readLine.trim();
                if (readLine.isEmpty() || readLine.startsWith("#")) continue;
                String[] split = readLine.split(",", 2);
                if (split.length < 2) continue;
                if (split[1].trim().equals("#genre#")) {
                    currentGroup = split[0].trim();
                } else {
                    String channelName = split[0].trim();
                    String urlPart = split[1].trim();
                    ArrayList<LiveChannelItem> channelArrayList = linkedHashMap.computeIfAbsent(currentGroup, k -> new ArrayList<>());
                    String[] channelUrls = urlPart.split("#");
                    ArrayList<String> validUrls = new ArrayList<>();
                    for (String u : channelUrls) {
                        String trimUrl = u.trim();
                        if (isUrl(trimUrl)) {
                            validUrls.add(trimUrl);
                        }
                    }
                    if (validUrls.isEmpty()) {
                        continue;
                    }
                    LiveChannelItem existingItem = null;
                    for (LiveChannelItem channelItem : channelArrayList) {
                        if (channelItem.channelName != null && channelItem.channelName.equals(channelName)) {
                            existingItem = channelItem;
                            break;
                        }
                    }
                    if (existingItem != null) {
                        for (String url : validUrls) {
                            if (!existingItem.channelUrls.contains(url)) {
                                existingItem.channelUrls.add(url);
                            }
                        }
                    } else {
                        LiveChannelItem newItem = new LiveChannelItem();
                        newItem.channelName = channelName;
                        newItem.channelUrls.addAll(validUrls);
                        channelArrayList.add(newItem);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static JsonArray live2JsonArray(LinkedHashMap<String, ArrayList<LiveChannelItem>> linkedHashMap) {
        JsonArray jsonArr = new JsonArray();
        if (linkedHashMap == null || linkedHashMap.isEmpty()) return jsonArr;
        for (String groupName : linkedHashMap.keySet()) {
            ArrayList<LiveChannelItem> channels = linkedHashMap.get(groupName);
            if (channels == null || channels.isEmpty()) continue;
            JsonObject groupObj = new JsonObject();
            groupObj.addProperty("group", groupName);
            JsonArray channelsArr = new JsonArray();
            for (LiveChannelItem channelItem : channels) {
                JsonObject channelObj = new JsonObject();
                channelObj.addProperty("name", channelItem.channelName);
                JsonArray channelUrlsArr = new JsonArray();
                for (String url : channelItem.channelUrls) {
                    channelUrlsArr.add(url);
                }
                channelObj.add("urls", channelUrlsArr);
                if (channelItem.logo != null && !channelItem.logo.isEmpty()) {
                    channelObj.addProperty("logo", channelItem.logo);
                }
                if (channelItem.useragent != null && !channelItem.useragent.isEmpty()) {
                    channelObj.addProperty("useragent", channelItem.useragent);
                }
                if (channelItem.hasCatchup()) {
                    channelObj.add("catchup", channelItem.catchupConfig);
                }
                channelsArr.add(channelObj);
            }
            groupObj.add("channels", channelsArr);
            jsonArr.add(groupObj);
        }
        return jsonArr;
    }

    private static String getStrByRegex(Pattern pattern, String str) {
        if (str == null || pattern == null) return "";
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static boolean isUrl(String str) {
        if (str == null || str.isEmpty()) return false;
        String lower = str.toLowerCase();
        return lower.startsWith("http") || lower.startsWith("rtmp") || lower.startsWith("rtsp") || lower.startsWith("rtp") || lower.startsWith("udp") || lower.startsWith("ftp");
    }
}
