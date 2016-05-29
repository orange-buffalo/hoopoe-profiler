package hoopoe.core.supplements;

import hoopoe.api.HoopoeThreadLocalCache;
import java.util.HashMap;
import java.util.Map;

public class HoopoeThreadLocalCacheImpl implements HoopoeThreadLocalCache {

    private Map cache = new HashMap<>();

    @Override
    public void set(Object key, Object value) {
        cache.put(key, value);
    }

    @Override
    public <T> T get(Object key) {
        return (T) cache.get(key);
    }

    @Override
    public void remove(Object key) {
        cache.remove(key);
    }
}
