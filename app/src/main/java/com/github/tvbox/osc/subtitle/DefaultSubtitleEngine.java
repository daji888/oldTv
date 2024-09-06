/*
 *                       Copyright (C) of Avery
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`- -'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\- -/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .' /- -.- -\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=- -='
 *           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *              Buddha bless, there will never be bug!!!
 */

package com.github.tvbox.osc.subtitle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.player.EXOmPlayer;
import com.github.tvbox.osc.subtitle.model.Subtitle;
import com.github.tvbox.osc.subtitle.model.Time;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.SubtitleHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import xyz.doikki.videoplayer.player.AbstractPlayer;
import static com.lzy.okgo.utils.HttpUtils.runOnUiThread;

/**
 * @author AveryZhong.
 */

public class DefaultSubtitleEngine implements SubtitleEngine {
    private static final String TAG = DefaultSubtitleEngine.class.getSimpleName();
    private static final int MSG_REFRESH = 0x888;
    private static final int REFRESH_INTERVAL = 100;

    @Nullable
    private Handler mWorkHandler;
    @Nullable
    private List<Subtitle> mSubtitles;
    private UIRenderTask mUIRenderTask;
    private AbstractPlayer mMediaPlayer;
    private OnSubtitlePreparedListener mOnSubtitlePreparedListener;
    private OnSubtitleChangeListener mOnSubtitleChangeListener;

    public DefaultSubtitleEngine() {

    }

    @Override
    public void bindToMediaPlayer(AbstractPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void setSubtitlePath(final String path) {
        initWorkThread();
        reset();
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "loadSubtitleFromRemote: path is null.");
            return;
        }

        SubtitleLoader.loadSubtitle(path, new SubtitleLoader.Callback() {
            @Override
            public void onSuccess(final SubtitleLoadSuccessResult subtitleLoadSuccessResult) {
                if (subtitleLoadSuccessResult == null) {
                    Log.d(TAG, "onSuccess: subtitleLoadSuccessResult is null.");
                    return;
                }
                if (subtitleLoadSuccessResult.timedTextObject == null) {
                    Log.d(TAG, "onSuccess: timedTextObject is null.");
                    return;
                }
                final TreeMap<Integer, Subtitle> captions = subtitleLoadSuccessResult.timedTextObject.captions;
                if (captions == null) {
                    Log.d(TAG, "onSuccess: captions is null.");
                    return;
                }
                mSubtitles = new ArrayList<>(captions.values());
                setSubtitleDelay(SubtitleHelper.getTimeDelay());
                notifyPrepared();

                String subtitlePath = subtitleLoadSuccessResult.subtitlePath;
                if (subtitlePath.startsWith("http://") || subtitlePath.startsWith("https://")) {
                    String subtitleFileCacheDir = App.getInstance().getCacheDir().getAbsolutePath() + "/zimu/";
                    File cacheDir = new File(subtitleFileCacheDir);
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }
                    String subtitleFile = subtitleFileCacheDir + subtitleLoadSuccessResult.fileName;
                    File cacheSubtitleFile = new File(subtitleFile);
                    boolean writeResult = FileUtils.writeSimple(subtitleLoadSuccessResult.content.getBytes(), cacheSubtitleFile);
                    if (writeResult && playSubtitleCacheKey != null) {
                        CacheManager.save(MD5.string2MD5(getPlaySubtitleCacheKey()), subtitleFile);
                    }
                } else {
                    CacheManager.save(MD5.string2MD5(getPlaySubtitleCacheKey()), path);
                }
            }

            @Override
            public void onError(final Exception exception) {
                Log.e(TAG, "onError: " + exception.getMessage());
            }
        });
    }

    @Override
    public void setSubtitleDelay(Integer milliseconds) {
        if (milliseconds == 0) {
            return;
        }
        if (mSubtitles == null || mSubtitles.size() == 0) {
            return;
        }
        List<Subtitle> thisSubtitles = mSubtitles;
        mSubtitles = null;
        for (int i = 0; i < thisSubtitles.size(); i++) {
            Subtitle subtitle = thisSubtitles.get(i);
            Time start = subtitle.start;
            Time end = subtitle.end;
            start.mseconds += milliseconds;
            end.mseconds += milliseconds;
            if (start.mseconds <= 0) {
                start.mseconds = 0;
            }
            if (end.mseconds <= 0) {
                end.mseconds = 0;
            }
            subtitle.start = start;
            subtitle.end = end;
        }
        mSubtitles = thisSubtitles;
    }

    private static String playSubtitleCacheKey;
    public void setPlaySubtitleCacheKey(String cacheKey) {
        playSubtitleCacheKey = cacheKey;
    }

    public String getPlaySubtitleCacheKey() {
        return playSubtitleCacheKey;
    }

    @Override
    public void reset() {
        stop();
        mSubtitles = null;
        mUIRenderTask = null;
    }

    @Override
    public void start() {
        Log.d(TAG, "start: ");
        if (mMediaPlayer == null) {
            Log.w(TAG, "MediaPlayer is not bind, You must bind MediaPlayer to "
                    + SubtitleEngine.class.getSimpleName()
                    + " before start() method be called,"
                    + " you can do this by call " +
                    "bindToMediaPlayer(MediaPlayer mediaPlayer) method.");
            return;
        }
        stop();
        if (mWorkHandler != null) {
            mWorkHandler.sendEmptyMessageDelayed(MSG_REFRESH, REFRESH_INTERVAL);
        }

    }

    @Override
    public void pause() {
        stop();
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void stop() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSG_REFRESH);
        }
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy: ");
        stopWorkThread();
        reset();

    }

    private void initWorkThread() {
        stopWorkThread();
        mWorkHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(final Message msg) {
                try {
                         if (mMediaPlayer != null && mMediaPlayer instanceof EXOmPlayer) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long delay = REFRESH_INTERVAL;
                                long position = mMediaPlayer.getCurrentPosition();
                                Subtitle subtitle = SubtitleFinder.find(position, mSubtitles);
                                notifyRefreshUI(subtitle);
                                if (subtitle != null) {
                                    delay = subtitle.end.mseconds - position;
                                }

                                if (mWorkHandler != null) {
                                    mWorkHandler.sendEmptyMessageDelayed(MSG_REFRESH, delay);
                                }
                            }
                        });
                        return true;
                    }
                    long delay = REFRESH_INTERVAL;
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        long position = mMediaPlayer.getCurrentPosition();
                        Subtitle subtitle = SubtitleFinder.find(position, mSubtitles);
                        notifyRefreshUI(subtitle);
                        if (subtitle != null) {
                            delay = subtitle.end.mseconds - position;
                        }

                    }
                    if (mWorkHandler != null) {
                        mWorkHandler.sendEmptyMessageDelayed(MSG_REFRESH, delay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void stopWorkThread() {
        if (mWorkHandler != null) {
            mWorkHandler.removeCallbacksAndMessages(null);
            mWorkHandler = null;
        }
    }

    private void notifyRefreshUI(final Subtitle subtitle) {
        if (mUIRenderTask == null) {
            mUIRenderTask = new UIRenderTask(mOnSubtitleChangeListener);
        }
        mUIRenderTask.execute(subtitle);
    }

    private void notifyPrepared() {
        if (mOnSubtitlePreparedListener != null) {
            mOnSubtitlePreparedListener.onSubtitlePrepared(mSubtitles);
        }
    }

    @Override
    public void setOnSubtitlePreparedListener(final OnSubtitlePreparedListener listener) {
        mOnSubtitlePreparedListener = listener;
    }

    @Override
    public void setOnSubtitleChangeListener(final OnSubtitleChangeListener listener) {
        mOnSubtitleChangeListener = listener;
    }

}
