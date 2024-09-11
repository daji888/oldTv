package com.github.tvbox.osc.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.EXOCode;
import xyz.doikki.videoplayer.exo.ExoMediaPlayer;
import java.util.LinkedHashMap;

import androidx.annotation.IntDef;

public class EXOmPlayer extends ExoMediaPlayer {
    private String audioId = "";
    private String videoId = "";
    private String subtitleId = "";
//    private EXOCode exocodec = null;
    private static DefaultRenderersFactory mRenderersFactory;

    public EXOmPlayer(Context context, EXOCode exocodec) {
        super(context);
        this.exocodec = exocodec;
    }

/*    @IntDef({EXTENSION_RENDERER_MODE_ON, EXTENSION_RENDERER_MODE_PREFER, EXTENSION_RENDERER_MODE_OFF})
    public @interface ExtensionRendererMode {}

    public static final int EXTENSION_RENDERER_MODE_OFF = 0;
    public static final int EXTENSION_RENDERER_MODE_ON = 1;
    public static final int EXTENSION_RENDERER_MODE_PREFER = 2;
    private @ExtensionRendererMode int extensionRendererMode;

    @Override
    public void setOptions() {
        EXOCode exocodecTmp = this.exocodec == null ? ApiConfig.get().getCurrentEXOCode() : this.exocodec;
        LinkedHashMap<String, String> options = exocodecTmp.getOption();
        if (options != null) {
            for (String key : options.keySet()) {
           //     String value = options.get(key);
                String[] opt = key.split("\\|");
                @ExtensionRendererMode int extensionRendererMode = Integer.parseInt(opt[0].trim());
                String name = opt[1].trim();
                try {
            //        if (mRenderersFactory == null) {
            //            mRenderersFactory = new DefaultRenderersFactory(mAppContext);
            //            mRenderersFactory.setExtensionRendererMode(extensionRendererMode);
            //         }   
                    if (mRenderersFactory == null) {
                        mRenderersFactory = new DefaultRenderersFactory(mAppContext);
                        if (extensionRendererMode == 0) {
                            mRenderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
                        } else if (extensionRendererMode == 1) {
                            mRenderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
                        } else if (extensionRendererMode == 2) {
                            mRenderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
                       }   
                    }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.setOptions();
    }*/

    @SuppressLint("UnsafeOptInUsageError")
    public TrackInfo getTrackInfo() {
        TrackInfo data = new TrackInfo();
        MappingTrackSelector.MappedTrackInfo trackInfo = getTrackSelector().getCurrentMappedTrackInfo();
        if (trackInfo != null) {
            getExoSelectedTrack();
            for (int groupArrayIndex = 0; groupArrayIndex < trackInfo.getRendererCount(); groupArrayIndex++) {
                TrackGroupArray groupArray = trackInfo.getTrackGroups(groupArrayIndex);
                for (int groupIndex = 0; groupIndex < groupArray.length; groupIndex++) {
                    TrackGroup group = groupArray.get(groupIndex);
                    for (int formatIndex = 0; formatIndex < group.length; formatIndex++) {
                        Format format = group.getFormat(formatIndex);
                        if (MimeTypes.isAudio(format.sampleMimeType)) {
                            String trackName = (data.getAudio().size() + 1) + "、" + trackNameProvider.getTrackName(format) + "，"  +  format.codecs;
                            TrackInfoBean t = new TrackInfoBean();
                            t.name = trackName;
                            t.language = "";
                            t.trackId = formatIndex;
                            t.selected = !StringUtils.isEmpty(audioId) && audioId.equals(format.id);
                            t.trackGroupId = groupIndex;
                            t.renderId = groupArrayIndex;
                            data.addAudio(t);
                        } else if (MimeTypes.isVideo(format.sampleMimeType)) {
                            String trackName = (data.getVideo().size() + 1) + "、" + trackNameProvider.getTrackName(format) + "，"  +  format.codecs;
                            TrackInfoBean t = new TrackInfoBean();
                            t.name = trackName;
                        //    t.language = "";
                            t.trackId = formatIndex;
                            t.selected = !StringUtils.isEmpty(videoId) && videoId.equals(format.id);
                            t.trackGroupId = groupIndex;
                            t.renderId = groupArrayIndex;
                            data.addVideo(t);
                        } else if (MimeTypes.isText(format.sampleMimeType)) {
                            String trackName = (data.getSubtitle().size() + 1) + "、" + trackNameProvider.getTrackName(format);
                            TrackInfoBean t = new TrackInfoBean();
                            t.name = trackName;
                            t.language = "";
                            t.trackId = formatIndex;
                            t.selected = !StringUtils.isEmpty(subtitleId) && subtitleId.equals(format.id);
                            t.trackGroupId = groupIndex;
                            t.renderId = groupArrayIndex;
                            data.addSubtitle(t);
                        }
                    }
                }
            }
        }
        return data;
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void getExoSelectedTrack() {
        audioId = "";
        videoId = "";
        subtitleId = "";        
        for (Tracks.Group group : mMediaPlayer.getCurrentTracks().getGroups()) {
            if (!group.isSelected()) continue;
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (!group.isTrackSelected(trackIndex)) continue;
                Format format = group.getTrackFormat(trackIndex);
                if (MimeTypes.isAudio(format.sampleMimeType)) {
                    audioId = format.id;
                }
                if (MimeTypes.isVideo(format.sampleMimeType)) {
                    videoId = format.id;
                }
                if (MimeTypes.isText(format.sampleMimeType)) {
                    subtitleId = format.id;
                }                
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void selectExoTrack(@Nullable TrackInfoBean videoTrackBean) {
        MappingTrackSelector.MappedTrackInfo trackInfo = getTrackSelector().getCurrentMappedTrackInfo();
        if (trackInfo != null) {
            if (videoTrackBean == null) {
                for (int renderIndex = 0; renderIndex < trackInfo.getRendererCount(); renderIndex++) {
                    if (trackInfo.getRendererType(renderIndex) == C.TRACK_TYPE_TEXT) {
                        DefaultTrackSelector.Parameters.Builder parametersBuilder = getTrackSelector().getParameters().buildUpon();
                        parametersBuilder.setRendererDisabled(renderIndex, true);
                        getTrackSelector().setParameters(parametersBuilder);
                        break;
                    }
                }
            } else {
                TrackGroupArray trackGroupArray = trackInfo.getTrackGroups(videoTrackBean.renderId);
                @SuppressLint("UnsafeOptInUsageError") DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(videoTrackBean.trackGroupId, videoTrackBean.trackId);
                DefaultTrackSelector.Parameters.Builder parametersBuilder = getTrackSelector().buildUponParameters();
                parametersBuilder.setRendererDisabled(videoTrackBean.renderId, false);
                parametersBuilder.setSelectionOverride(videoTrackBean.renderId, trackGroupArray, override);
                getTrackSelector().setParameters(parametersBuilder);
            }
        }

    }

    public void setOnTimedTextListener(Player.Listener listener) {
        mMediaPlayer.addListener(listener);
    }

}
