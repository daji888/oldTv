package xyz.doikki.videoplayer.exo;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import android.content.Context;
import com.github.catvod.utils.Path;

public class CacheManager {

    private SimpleCache cache;
    private final Context mAppContext;
    private static volatile CacheManager sInstance;
    
    private static class Loader {
        static volatile CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (CacheManager.class) {
                if (sInstance == null) {
                    sInstance = new CacheManager(context);
                }
            }
        }
        return sInstance;
    }

    public static CacheManager get() {
        return Loader.INSTANCE;
    }

    public Cache getCache() {
        if (cache == null) create();
        return cache;
    }

    private void create() {
        cache = new SimpleCache(Path.exo(), new NoOpCacheEvictor(), new StandaloneDatabaseProvider(mAppContext));
    }
}
