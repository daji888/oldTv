package com.github.tvbox.osc.player;

import java.util.ArrayList;
import java.util.List;

public class TrackInfo {
    private final List<TrackInfoBean> audio;
    private final List<TrackInfoBean> video;
    private final List<TrackInfoBean> subtitle;

    public TrackInfo() {
        audio = new ArrayList<>();
        video = new ArrayList<>();
        subtitle = new ArrayList<>();
    }

    public List<TrackInfoBean> getAudio() {
        return audio;
    }

    public List<TrackInfoBean> getVideo() {
        return video;
    }

    public List<TrackInfoBean> getSubtitle() {
        return subtitle;
    }

    public int getAudioSelected(boolean track) {
        return getSelected(audio, track);
    }

    public int getVideoSelected(boolean track) {
        return getSelected(video, track);
    }

    public int getSubtitleSelected(boolean track) {
        return getSelected(subtitle, track);
    }

    public int getSelected(List<TrackInfoBean> list, boolean track) {
        int i = 0;
        for (TrackInfoBean audio : list) {
            if (audio.selected) return track ? audio.trackId : i;
            i++;
        }
        for (TrackInfoBean video : list) {
            if (video.selected) return track ? video.trackId : i;
            i++;
        }
        for (TrackInfoBean subtitle : list) {
            if (subtitle.selected) return track ? subtitle.trackId : i;
            i++;
        }
        return 99999;
    }

    public void addAudio(TrackInfoBean audio) {
        this.audio.add(audio);
    }

    public void addVideo(TrackInfoBean video) {
        this.video.add(video);
    }

    public void addSubtitle(TrackInfoBean subtitle) {
        this.subtitle.add(subtitle);
    }
}
