package com.github.tvbox.osc.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.text.TextUtils;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;
import xyz.doikki.videoplayer.ijk.IjkPlayer;
import xyz.doikki.videoplayer.ijk.RawDataSourceProvider;

public class IjkmPlayer extends IjkPlayer {

    private IJKCode codec = null;

    public IjkmPlayer(Context context, IJKCode codec) {
        super(context);
        this.codec = codec;
    }

    @Override
    public void setOptions() {
        IJKCode codecTmp = this.codec == null ? ApiConfig.get().getCurrentIJKCode() : this.codec;
        LinkedHashMap<String, String> options = codecTmp.getOption();
        if (options != null) {
            for (String key : options.keySet()) {
                String value = options.get(key);
                String[] opt = key.split("\\|");
                int category = Integer.parseInt(opt[0].trim());
                String name = opt[1].trim();
                try {
                    long valLong = Long.parseLong(value);
                    mMediaPlayer.setOption(category, name, valLong);
                } catch (Exception e) {
                    mMediaPlayer.setOption(category, name, value);
                }
            }
        }
        
        // 是否开启跳过 loop filter，0 为开启，画面质量高，但解码开销大，48 为关闭，画面质量稍差，但解码开销小
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
        // 是否清空DNS，1 为清空
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        // DNS 缓存超时时间，-1 为永久缓存
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_timeout", -1);
        // 参数值为 fastseek 时表示启用快速 seek 功能，可以显著提升视频拖放操作的响应速度
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");
        // 参数值为 0 时表示关闭HTTP范围检测支持，这个设置通常用于解决某些网络环境下出现的播放异常问题，特别是当遇到HTTP和HTTPS混合内容时可能导致的播放错误
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        // 允许使用的流媒体传输协议列表
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,ffio,async,cache,crypto,file,dash,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,concat,subfile,ffconcat");
        // 是否允许一些不安全的路径，默认值是 1 ，会拒绝一些不安全的文件路径, 设置为 0 ，关闭安全监测
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0);
        // 是否开启精准 seek，0：默认关闭，1：启用
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        // 当 CPU 处理不过来的时候丢帧帧数，默认为 0，参数范围是 [-1, 120]
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 0);
        // 音频播放是否使用 openSL，0：使用 AudioTrack，1：使用 openSL
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        // 设置视频显示格式
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        // 网络波动时自动重连
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "reconnect", 1);
        // 启用变速不变调模式
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
        //视频缓存好之后是否自动播放 1：允许 0：不允许
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        //开启内置字幕
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1);
        
        if (Hawk.get(HawkConfig.PLAYER_IS_LIVE)) {
            LOG.i("echo-type-直播");
            // 设置在处理每个数据包之后是否刷新 I/O 上下文, 当设置为 1 时，每个数据包处理完毕后 立即 刷新 I/O 上下文
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);
            // 设置持续输入缓冲模式, 值为 1 时启用持续输入缓冲，值为 0 则关闭持续输入缓冲
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
            // 设置最大缓冲区大小
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 256 * 1024);
            // 设置最大缓存时长
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 300);
            // 0 关闭预缓冲，可能会卡顿（不会回调info)
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        } else {
            LOG.i("echo-type-点播");
            // 设置在处理每个数据包之后是否刷新 I/O 上下文, 当设置为 0 时，每个数据包处理完毕后 不 刷新 I/O 上下文
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 0);
            // 设置持续输入缓冲模式, 值为 1 时启用持续输入缓冲，值为 0 则关闭持续输入缓冲
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 0);
            // 设置最大缓冲区大小
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 10 * 1024 * 1024);
            // 设置最大缓存时长
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
            // 1 启用预缓冲，减少卡顿（会回调info)
        //    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);
        }
        super.setOptions();
    }

    private static final String ITV_TARGET_DOMAIN = "gslbserv.itv.cmvideo.cn";

    @Override
    public void setDataSource(String path, Map<String, String> headers) {
        try {
            switch (getStreamType(path)) {
                case RTSP_UDP_RTP:
                    // 设置持续输入缓冲模式, 当值为 0 时，表示关闭持续输入缓冲，值为 1 时，表示启用持续输入缓冲
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 1);
                    // 指定RTSP协议的传输方式，默认使用UDP，使用TCP可以降低延迟并提升稳定性
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
                    // 强制RTSP流使用TCP传输协议, 而非默认的UDP
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp");
                    // 设置媒体文件探测大小
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 512 * 1000);
                    // 设置媒体文件分析时长
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 2 * 1000 * 1000);
                case CACHE_VIDEO:
                    if (Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) {
                        String cachePath = FileUtils.getCachePath() + "/ijkcaches/";
                        File cacheFile = new File(cachePath);
                        if (!cacheFile.exists()) cacheFile.mkdirs();
                        String tmpMd5 = MD5.string2MD5(path);
                        String cacheFilePath = cachePath + tmpMd5 + ".file";
                        String cacheMapPath = cachePath + tmpMd5 + ".map";

                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_file_path", cacheFilePath);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_map_path", cacheMapPath);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "parse_cache_map", 1);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "auto_save_map", 1);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_max_capacity", 60 * 1024 * 1024);
                        path = "ijkio:cache:ffio:" + path;
                    }
                    break;

                case M3U8:
                    // 直播且是ijk的时候自动走代理解决DNS
                    if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) {
                        URI uri = new URI(path);
                        String host = uri.getHost();
                        if (ITV_TARGET_DOMAIN.equalsIgnoreCase(host)) path = ControlManager.get().getAddress(true) + "proxy?go=live&type=m3u8&url=" + URLEncoder.encode(path, "UTF-8");
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            mPlayerEventListener.onError(-1, PlayerHelper.getRootCauseMessage(e));
        }
        setDataSourceHeader(headers);
        
//        try {
//            path = encodeSpaceChinese(path);//会导致本地文件无法播放，故注释掉
//        } catch (Exception ignored) {
//
//        }
        super.setDataSource(path, headers);
    }

    /**
     * 解析 URL
     */
    private static final int RTSP_UDP_RTP = 1;
    private static final int CACHE_VIDEO = 2;
    private static final int M3U8 = 3;
    private static final int OTHER = 0;

    private int getStreamType(String path) {
        if (TextUtils.isEmpty(path)) {
            return OTHER;
        }
        // 低成本检查 RTSP/UDP/RTP 类型
        String lowerPath = path.toLowerCase();
        if (lowerPath.startsWith("rtsp://") || lowerPath.startsWith("udp://") || lowerPath.startsWith("rtp://")) {
            return RTSP_UDP_RTP;
        }
        String cleanUrl = path.split("\\?")[0];
        if (cleanUrl.endsWith(".m3u8")) {
            return M3U8;
        }
        if (cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".mkv") || cleanUrl.endsWith(".avi")) {
            return CACHE_VIDEO;
        }
        return OTHER;
    }

    private String encodeSpaceChinese(String str) throws UnsupportedEncodingException {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5 ]+");
        Matcher m = p.matcher(str);
        StringBuffer b = new StringBuffer();
        while (m.find()) m.appendReplacement(b, URLEncoder.encode(m.group(0), "UTF-8"));
        m.appendTail(b);
        return b.toString();
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {
        try {
            mMediaPlayer.setDataSource(new RawDataSourceProvider(fd));
        } catch (Exception e) {
            mPlayerEventListener.onError(-1, PlayerHelper.getRootCauseMessage(e));
        }
    }
    private void setDataSourceHeader(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            String userAgent = headers.get("User-Agent");
            if (!TextUtils.isEmpty(userAgent)) {
                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", userAgent);
                // 移除header中的User-Agent，防止重复
                headers.remove("User-Agent");
            }
            if (headers.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    sb.append(entry.getKey());
                    sb.append(":");
                    String value = entry.getValue();
                    if (!TextUtils.isEmpty(value))
                        sb.append(entry.getValue());
                    sb.append("\r\n");
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", sb.toString());
                }
            }
        }
    }
    public TrackInfo getTrackInfo() {
        IjkTrackInfo[] trackInfo = mMediaPlayer.getTrackInfo();
        if (trackInfo == null) return null;
        TrackInfo data = new TrackInfo();
        int audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        int videoSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        int subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
        int index = 0;
        for (IjkTrackInfo info : trackInfo) {
            if (info.getTrackType() == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) {//音轨信息
                String trackName = (data.getAudio().size() + 1) + ".  " + info.getInfoInline();
                TrackInfoBean t = new TrackInfoBean();
                t.name = trackName;
                t.language = info.getLanguage();
                t.trackId = index;
                t.selected = index == audioSelected;
                data.addAudio(t);
            }
            if (info.getTrackType() == ITrackInfo.MEDIA_TRACK_TYPE_VIDEO) {//视轨信息
                String trackName = (data.getVideo().size() + 1) + ".  " + info.getInfoInline();
                TrackInfoBean t = new TrackInfoBean();
                t.name = trackName;
                t.language = info.getLanguage();
                t.trackId = index;
                t.selected = index == videoSelected;
                data.addVideo(t);
            }
            if (info.getTrackType() == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {//内置字幕
                String trackName = (data.getSubtitle().size() + 1) + ".  " + info.getInfoInline();
                TrackInfoBean t = new TrackInfoBean();
                t.name = trackName;
                t.language = info.getLanguage();
                t.trackId = index;
                t.selected = index == subtitleSelected;
                data.addSubtitle(t);
            }
            index++;
        }
        return data;
    }

    public void setTrack(int trackIndex) {
        int audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        int videoSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        int subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
        if (trackIndex != audioSelected && trackIndex != videoSelected && trackIndex != subtitleSelected) {
            mMediaPlayer.selectTrack(trackIndex);
        }
    }

    public void setOnTimedTextListener(IMediaPlayer.OnTimedTextListener listener) {
        mMediaPlayer.setOnTimedTextListener(listener);
    }

}
