package xyz.doikki.videoplayer.exo;

import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.common.util.Util;
import android.content.Context;
import com.github.catvod.utils.Path;

public class CacheManager {

    private SimpleCache cache;
    private final Context mAppContext;
    private final String mUserAgent;
    
    private static class Loader {
        static volatile CacheManager INSTANCE = new CacheManager();
    }

    private CacheManager(Context context) {
        mAppContext = context.getApplicationContext();
        mUserAgent = Util.getUserAgent(mAppContext, mAppContext.getApplicationInfo().name);
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
