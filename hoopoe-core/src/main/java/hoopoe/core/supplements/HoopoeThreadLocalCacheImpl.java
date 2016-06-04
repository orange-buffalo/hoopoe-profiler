package hoopoe.core.supplements;

import hoopoe.api.HoopoeThreadLocalCache;
import java.util.HashMap;
import java.util.Map;

public class HoopoeThreadLocalCacheImpl implements HoopoeThreadLocalCache {

    private static final ThreadLocal<Map> cacheHolder = new ThreadLocal<>();

    @Override
    public void set(Object key, Object value) {
        getCache().put(key, value);
    }

    @Override
    public <T> T get(Object key) {
        return (T) getCache().get(key);
    }

    @Override
    public void remove(Object key) {
        getCache().remove(key);
    }

    @Override
    public void clear() {
        cacheHolder.set(null);
    }

    private Map getCache() {
        Map cache = cacheHolder.get();
        if (cache == null) {
            cache = new HashMap<>();
            cacheHolder.set(cache);
        }
        return cache;
    }

}
