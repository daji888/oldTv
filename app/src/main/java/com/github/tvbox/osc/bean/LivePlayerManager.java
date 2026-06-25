package com.github.tvbox.osc.bean;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.doikki.videoplayer.player.VideoView;

public class LivePlayerManager {
    private Context mContext;
    JSONObject defaultPlayerConfig = new JSONObject();
    JSONObject currentPlayerConfig;

    public LivePlayerManager() {
        this.mContext = App.getInstance().getApplicationContext();
    }

    public void init(VideoView videoView) {
        try {
            defaultPlayerConfig.put("pl", Hawk.get(HawkConfig.LIVE_PLAY_TYPE, Hawk.get(HawkConfig.PLAY_TYPE, 0)));
            defaultPlayerConfig.put("pl", Hawk.get(HawkConfig.PLAY_TYPE, 0));
            defaultPlayerConfig.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, "硬解"));
            defaultPlayerConfig.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0));
            defaultPlayerConfig.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getDefaultLiveChannelPlayer(videoView);
    }

    public void getDefaultLiveChannelPlayer(VideoView videoView) {
        PlayerHelper.updateCfg(videoView, defaultPlayerConfig);
        try {
            currentPlayerConfig = new JSONObject(defaultPlayerConfig.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getLiveChannelPlayer(VideoView videoView, String channelName) {
        JSONObject playerConfig = Hawk.get(channelName, null);
        if (playerConfig == null) {
            if (!currentPlayerConfig.toString().equals(defaultPlayerConfig.toString()))
                getDefaultLiveChannelPlayer(videoView);
            return;
        }
        if (playerConfig.toString().equals(currentPlayerConfig.toString())) return;
        try {
            if (playerConfig.getInt("pl") == currentPlayerConfig.getInt("pl")
                    && playerConfig.getInt("pr") == currentPlayerConfig.getInt("pr")
                    && playerConfig.getString("ijk").equals(currentPlayerConfig.getString("ijk"))) {
                videoView.setScreenScaleType(playerConfig.getInt("sc"));
            } else {
                PlayerHelper.updateCfg(videoView, playerConfig);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        currentPlayerConfig = playerConfig;
    }

    public int getLivePlayerType() {
        int playerTypeIndex = 0;
        try {
            int playerType = currentPlayerConfig.getInt("pl");
            String ijkCodec = currentPlayerConfig.getString("ijk");
            switch (playerType) {
                case 0:
                    playerTypeIndex = 0;
                    break;
                case 1:
                    if (ijkCodec.equals("硬解"))
                        playerTypeIndex = 1;
                    else
                        playerTypeIndex = 2;
                    break;
                case 2:
                    playerTypeIndex = 3;
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return playerTypeIndex;
    }

    public int getLivePlayerScale() {
        try {
            return currentPlayerConfig.getInt("sc");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void changeLivePlayerType(VideoView videoView, int playerType, String channelName) {
        JSONObject playerConfig = currentPlayerConfig;
        try {
            switch (playerType) {
                case 0:
                    playerConfig.put("pl", 0);
                    playerConfig.put("系统", "");
                    break;
                case 1:
                    playerConfig.put("pl", 1);
                    playerConfig.put("ijk", "硬解");
                    break;
                case 2:
                    playerConfig.put("pl", 1);
                    playerConfig.put("ijk", "软解");
                    break;
                case 3:
                    playerConfig.put("pl", 2);
                    playerConfig.put("exo", "");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PlayerHelper.updateCfg(videoView, playerConfig);
        if (playerConfig.toString().equals(defaultPlayerConfig.toString()))
            Hawk.delete(channelName);
        else
            Hawk.put(channelName, playerConfig);
        currentPlayerConfig = playerConfig;
    }

    public boolean switchLivePlayer(VideoView videoView, String channelName) {
        JSONObject playerConfig = currentPlayerConfig;
        if (playerConfig == null) {
            LOG.i("echo-liveSwitchPlayer: skip empty player config");
            return false;
        }
        try {
            int playerType = playerConfig.getInt("pl");
            int switchPlayerType = (playerType == 1) ? 2 : (playerType == 2) ? 1 : playerType;
            if (switchPlayerType == playerType) {
                LOG.i("echo-liveSwitchPlayer: skip unsupported playerType=" + playerType);
                return false;
            }
            LOG.i("echo-liveSwitchPlayer: " + playerType + " -> " + switchPlayerType);
            if (mContext != null) {
                Toast.makeText(mContext, "切换到" + (switchPlayerType == 1 ? " IJK " : " EXO ") + "播放器重试", Toast.LENGTH_SHORT).show();
            }    
            playerConfig.put("pl", switchPlayerType);
        } catch (JSONException e) {
            LOG.i("echo-liveSwitchPlayer error: " + e.getMessage());
            return false;
        }
        PlayerHelper.updateCfg(videoView, playerConfig);
        if (playerConfig.toString().equals(defaultPlayerConfig.toString()))
            Hawk.delete(channelName);
        else
            Hawk.put(channelName, playerConfig);
        currentPlayerConfig = playerConfig;
        return true;
    }

    public void changeLivePlayerScale(@NonNull VideoView videoView, int playerScale, String channelName){
        videoView.setScreenScaleType(playerScale);
        JSONObject playerConfig = currentPlayerConfig;
        try {
            playerConfig.put("sc", playerScale);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (playerConfig.toString().equals(defaultPlayerConfig.toString()))
            Hawk.delete(channelName);
        else
            Hawk.put(channelName, playerConfig);
        currentPlayerConfig = playerConfig;
    }
}
