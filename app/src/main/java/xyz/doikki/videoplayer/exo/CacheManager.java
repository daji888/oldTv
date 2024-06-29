package xyz.doikki.videoplayer.exo;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import android.content.Context;
import com.github.catvod.utils.Path;

public class CacheManager {

    private SimpleCache cache;
    private Context mAppContext;
    public CacheManager(Context context) {
        mAppContext = context.getApplicationContext();
    }

    private static class Loader {
        static volatile CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager get() {
        return Loader.INSTANCE;
    }

    public Cache getCache() {
        if (cache == null) create();
        return cache;
    }

    private void create() {
        cache = new SimpleCache(File(FileUtils.getExternalCachePath(), "exo-video-cache"), new new LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024), new StandaloneDatabaseProvider(mAppContext));
    }
}
