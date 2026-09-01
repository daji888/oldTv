package xyz.doikki.videoplayer.exo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import java.util.HashMap;
import java.util.Map;

import xyz.doikki.videoplayer.player.AbstractPlayer;
import xyz.doikki.videoplayer.util.PlayerUtils;

public class ExoMediaPlayer extends AbstractPlayer implements Player.Listener {

    protected Context mAppContext;
    protected ExoPlayer mMediaPlayer;
    protected MediaSource mMediaSource;
    protected ExoMediaSourceHelper mMediaSourceHelper;
    protected ExoTrackNameProvider trackNameProvider;
    private PlaybackParameters mSpeedPlaybackParameters;
    private boolean mIsPreparing;

    private LoadControl mLoadControl;
    private DefaultRenderersFactory mRenderersFactory;
    protected DefaultTrackSelector mTrackSelector;

    private int errorCode = -100;
    private String currentPlayPath;
    protected Map<String, String> currentHeaders;
    private boolean mRetriedAsHls;
    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;

    public ExoMediaPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        mMediaSourceHelper = ExoMediaSourceHelper.getInstance(context);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void initPlayer() {
        if (mRenderersFactory == null) {
            mRenderersFactory = new DefaultRenderersFactory(mAppContext);
        //    mRenderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        }
        if (mTrackSelector == null) {
            mTrackSelector = new DefaultTrackSelector(mAppContext);
        }
        if (mLoadControl == null) {
            mLoadControl = new DefaultLoadControl();
        }
        mTrackSelector.setParameters(
            mTrackSelector.buildUponParameters()
                .setTunnelingEnabled(true)
                .setPreferredAudioLanguages("chi", "chs", "zh-Hans", "zho", "cht", "zh-Hant", "zh")
                .setPreferredTextLanguages("chi", "chs", "zh-Hans", "zho", "cht", "zh-Hant", "zh")                     
        );
        mMediaPlayer = new ExoPlayer.Builder(mAppContext)
            .setLoadControl(mLoadControl)
            .setRenderersFactory(mRenderersFactory)
            .setTrackSelector(mTrackSelector)
            .build();
        setOptions();
        mMediaPlayer.addListener(this);
    }

    @Override
    public void setDataSource(String path, Map<String, String> headers) {
        currentPlayPath = path;
        currentHeaders = copyHeaders(headers);
        mRetriedAsHls = false;
        mMediaSource = mMediaSourceHelper.getMediaSource(path, copyHeaders(currentHeaders));
        errorCode = -1;
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {
        //no support
    }

    @Override
    public void start() {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stop() {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.stop();
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void prepareAsync() {
        if (mMediaPlayer == null)
            return;
        if (mMediaSource == null) return;
        if (mSpeedPlaybackParameters != null) {
            mMediaPlayer.setPlaybackParameters(mSpeedPlaybackParameters);
        }
        mIsPreparing = true;
        mMediaPlayer.setMediaSource(mMediaSource);
        mMediaPlayer.prepare();
    }

    @Override
    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.clearMediaItems();
            mMediaPlayer.setVideoSurface(null);
            mIsPreparing = false;
            mRetriedAsHls = false;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer == null)
            return false;
        int state = mMediaPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_BUFFERING:
            case Player.STATE_READY:
                return mMediaPlayer.getPlayWhenReady();
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void seekTo(long time) {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.seekTo(time);
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.removeListener(this);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
        mIsPreparing = false;
        mSpeedPlaybackParameters = null;
    }

    @Override
    public long getCurrentPosition() {
        if (mMediaPlayer == null)
            return 0;
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        if (mMediaPlayer == null)
            return 0;
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getBufferedPercentage() {
        return mMediaPlayer == null ? 0 : mMediaPlayer.getBufferedPercentage();
    }

    @Override
    public void setSurface(Surface surface) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (holder == null)
            setSurface(null);
        else
            setSurface(holder.getSurface());
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (mMediaPlayer != null)
            mMediaPlayer.setVolume((leftVolume + rightVolume) / 2);
    }

    @Override
    public void setLooping(boolean isLooping) {
        if (mMediaPlayer != null)
            mMediaPlayer.setRepeatMode(isLooping ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
    }

    @Override
    public void setOptions() {
        //准备好就开始播放
        mMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public float getSpeed() {
        if (mSpeedPlaybackParameters != null) {
            return mSpeedPlaybackParameters.speed;
        }
        return 1f;
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters playbackParameters = new PlaybackParameters(speed);
        mSpeedPlaybackParameters = playbackParameters;
        if (mMediaPlayer != null) {
            mMediaPlayer.setPlaybackParameters(playbackParameters);
        }
    }

    private boolean unsupported() {
        return TrafficStats.getUidRxBytes(App.getInstance().getApplicationInfo().uid) == TrafficStats.UNSUPPORTED;
    }

    @Override
    public long getTcpSpeed() {
        if (mAppContext == null || unsupported()) {
            return 0;
        }
        return PlayerUtils.getNetSpeed(mAppContext);
    }

    private int lastSetWidth = 0;
    private int lastSetHeight = 0;
    
    @Override
    public void onTracksChanged(Tracks tracks) {
        if (trackNameProvider == null) {
            trackNameProvider = new ExoTrackNameProvider(mAppContext.getResources());
        }
    
        int maxVideoWidth = 0;
        int maxVideoHeight = 0;
    
        for (Tracks.Group group : tracks.getGroups()) {
            if (!group.isSelected()) continue;
            for (int j = 0; j < group.length; j++) {
                Format format = group.getTrackFormat(j);
                if (MimeTypes.isVideo(format.sampleMimeType)) {
                    maxVideoWidth = Math.max(maxVideoWidth, format.width);
                    maxVideoHeight = Math.max(maxVideoHeight, format.height);
                }
            }
        }
    
        // 只有参数真正变化时才设置，避免触发不必要的重选
        if (maxVideoWidth > 0 && maxVideoHeight > 0
                && (maxVideoWidth != lastSetWidth || maxVideoHeight != lastSetHeight)) {
            lastSetWidth = maxVideoWidth;
            lastSetHeight = maxVideoHeight;
    
            mTrackSelector.setParameters(
                mTrackSelector.buildUponParameters()
                    .setMinVideoSize(maxVideoWidth, maxVideoHeight)
            );
        }
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (mPlayerEventListener == null) return;
        if (mIsPreparing) {
            if (playbackState == Player.STATE_READY) {
                mPlayerEventListener.onPrepared();
                mPlayerEventListener.onInfo(MEDIA_INFO_RENDERING_START, 0);
                mIsPreparing = false;
            }
            return;
        }
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_START, getBufferedPercentage());
                break;
            case Player.STATE_READY:
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_END, getBufferedPercentage());
                break;
            case Player.STATE_ENDED:
                mPlayerEventListener.onCompletion();
                break;
            case Player.STATE_IDLE:
                break;
        }
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        errorCode = error.errorCode;
        Log.e("tag--", "" + error.errorCode);
        if (retryAsHls(error)) {
            return;
        }
        if (currentPlayPath != null) {
            setDataSource(currentPlayPath, copyHeaders(currentHeaders));
            currentPlayPath = null;
            prepareAsync();
            start();
        } else {
            if (mPlayerEventListener != null) {
                mPlayerEventListener.onError(error.errorCode, PlayerHelper.getRootCauseMessage(error));
            }
        }
    }

    private boolean retryAsHls(PlaybackException error) {
        if (mRetriedAsHls || mMediaPlayer == null || currentPlayPath == null) {
            return false;
        }
        if (!isParsingError(error)) {
            return false;
        }
        mRetriedAsHls = true;
        Log.i("Tvbox-runtime", "echo-Exo retry as HLS: " + currentPlayPath);
        mMediaSource = mMediaSourceHelper.getHlsMediaSource(currentPlayPath, copyHeaders(currentHeaders));
        mIsPreparing = true;
        mMediaPlayer.setMediaSource(mMediaSource);
        mMediaPlayer.prepare();
        mMediaPlayer.setPlayWhenReady(true);
        return true;
    }

    private boolean isParsingError(PlaybackException error) {
        int errorCode = error.errorCode;
        return errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                || errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
                || errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED;
    }

    private Map<String, String> copyHeaders(Map<String, String> headers) {
        return headers == null ? null : new HashMap<>(headers);
    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        Format videoFormat = mMediaPlayer.getVideoFormat();
        int rotationDegrees = (videoFormat != null) ? videoFormat.rotationDegrees : 0;
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onVideoSizeChanged(videoSize.width, videoSize.height);
            if (rotationDegrees > 0) {
                mPlayerEventListener.onInfo(MEDIA_INFO_VIDEO_ROTATION_CHANGED, rotationDegrees);
            }
        }
    }
}
