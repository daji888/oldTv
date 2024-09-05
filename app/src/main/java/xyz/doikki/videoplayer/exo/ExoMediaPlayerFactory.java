package xyz.doikki.videoplayer.exo;

import android.content.Context;

import xyz.doikki.videoplayer.player.PlayerFactory;

public class ExoMediaPlayerFactory extends PlayerFactory<ExoMediaPlayer> {
    private static ExoMediaPlayerFactory exoMediaPlayerFactory = null;
    private static ExoMediaPlayer exoMediaPlayer = null;
    public static ExoMediaPlayerFactory create() {
        if (exoMediaPlayerFactory == null) {
            exoMediaPlayerFactory = new ExoMediaPlayerFactory();
        }
        return exoMediaPlayerFactory;
    }

    @Override
    public ExoMediaPlayer createPlayer(Context context) {
        if (exoMediaPlayer == null) {
            exoMediaPlayer = new ExoMediaPlayer(context);
        }
        ExoMediaPlayer.mAppContext = context;
        return exoMediaPlayer;
    }
}
