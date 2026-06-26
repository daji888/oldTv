package com.github.tvbox.osc.player;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.TrackSelectionOverride;

import com.github.tvbox.osc.util.StringUtils;
import java.util.List;
import xyz.doikki.videoplayer.exo.ExoMediaPlayer;

public class EXOmPlayer extends ExoMediaPlayer {
    public EXOmPlayer(Context context) {
        super(context);
    }

    public TrackInfo getTrackInfo() {
        TrackInfo data = new TrackInfo();
        Tracks tracks = mMediaPlayer.getCurrentTracks();
        if (tracks == null) return data;
        List<Tracks.Group> groups = tracks.getGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            Tracks.Group group = groups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getTrackFormat(trackIndex);
                if (MimeTypes.isVideo(format.sampleMimeType)) {
                    String trackName = (data.getVideo().size() + 1) + ".  " + trackNameProvider.getTrackName(format);
                    TrackInfoBean t = new TrackInfoBean();
                    t.name = trackName;
                    t.trackId = trackIndex;
                    t.selected = group.isTrackSelected(trackIndex);
                    t.trackGroupId = groupIndex;
                    data.addVideo(t);
                } else if (MimeTypes.isAudio(format.sampleMimeType)) {
                    String trackName = (data.getAudio().size() + 1) + ".  " + trackNameProvider.getTrackName(format);
                    TrackInfoBean t = new TrackInfoBean();
                    t.name = trackName;
                    t.language = "";
                    t.trackId = trackIndex;
                    t.selected = group.isTrackSelected(trackIndex);
                    t.trackGroupId = groupIndex;
                    data.addAudio(t);    
                } else if (MimeTypes.isText(format.sampleMimeType)) {
                    String trackName = (data.getSubtitle().size() + 1) + ".  " + trackNameProvider.getTrackName(format);
                    TrackInfoBean t = new TrackInfoBean();
                    t.name = trackName;
                    t.language = "";
                    t.trackId = trackIndex;
                    t.selected = group.isTrackSelected(trackIndex);
                    t.trackGroupId = groupIndex;
                    data.addSubtitle(t);
                }
            }
        }
        return data;
    }

    public void selectExoTrack(@Nullable TrackInfoBean trackInfoBean) {
        if (trackInfoBean == null) {
            TrackSelectionParameters.Builder parametersBuilder = mTrackSelector.buildUponParameters();
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true);
            mTrackSelector.setParameters(parametersBuilder.build());
            return;
        } else {
            Tracks tracks = mMediaPlayer.getCurrentTracks();
            if (tracks == null) return;
            List<Tracks.Group> groups = tracks.getGroups();
            if (trackInfoBean.trackGroupId < 0 || trackInfoBean.trackGroupId >= groups.size()) return;
            Tracks.Group group = groups.get(trackInfoBean.trackGroupId);
            if (trackInfoBean.trackId < 0 || trackInfoBean.trackId >= group.length) return;
            TrackSelectionOverride override = new TrackSelectionOverride(group.getMediaTrackGroup(), trackInfoBean.trackId);
            TrackSelectionParameters.Builder parametersBuilder = mTrackSelector.buildUponParameters();
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
            parametersBuilder.setOverrideForType(override);
            mTrackSelector.setParameters(parametersBuilder.build());
        }
    }

    public void setOnTimedTextListener(Player.Listener listener) {
        mMediaPlayer.addListener(listener);
    }

}
