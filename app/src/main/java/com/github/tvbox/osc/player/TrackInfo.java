package com.github.tvbox.osc.player;

import java.util.ArrayList;
import java.util.List;

public class TrackInfo {
    private final List<TrackInfoBean> audio;
    private final List<TrackInfoBean> subtitle;
    private final List<TrackInfoBean> language;

    public TrackInfo() {
        audio = new ArrayList<>();
        subtitle = new ArrayList<>();
        language = new ArrayList<>();
    }

    public List<TrackInfoBean> getAudio() {
        return audio;
    }

    public List<TrackInfoBean> getSubtitle() {
        return subtitle;
    }
    
    public List<TrackInfoBean> getLanguage() {
        return language;
    }
    
    public int getAudioSelected(boolean track) {
        return getSelected(audio, track);
    }

    public int getSubtitleSelected(boolean track) {
        return getSelected(subtitle, track);
    }

    public int getLanguageSelected(boolean track) {
        return getSelected(language, track);
    }

    public int getSelected(List<TrackInfoBean> list, boolean track) {
        int i = 0;
        for (TrackInfoBean audio : list) {
            if (audio.selected) return track ? audio.trackId : i;
            i++;
        }
        return 99999;
    }

    public void addAudio(TrackInfoBean audio) {
        this.audio.add(audio);
    }

    public void addSubtitle(TrackInfoBean subtitle) {
        this.subtitle.add(subtitle);
    }

    public void addLanguage(TrackInfoBean language) {
        this.language.add(language);
    }
}
