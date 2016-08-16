package hoopoe.core.supplements;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoeThreadLocalCache;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;

public class TraceNodesWrapper {

    @Getter
    private List<TraceNode> traceNodes;

    @Getter
    private String threadName;

    private Map<HoopoePlugin, HoopoeThreadLocalCache> pluginsCaches;

    public void clear() {
        traceNodes = null;
        threadName = null;
        pluginsCaches = null;
    }

    public List<TraceNode> init() {
        traceNodes = new LinkedList<>();
        threadName = Thread.currentThread().getName();

        return traceNodes;
    }

    public HoopoeThreadLocalCache getPluginCache(HoopoePlugin plugin) {
        if (pluginsCaches == null) {
            pluginsCaches = new HashMap<>();
        }
        return pluginsCaches.computeIfAbsent(plugin, key -> new HoopoeThreadLocalCacheImpl());
    }

    private static class HoopoeThreadLocalCacheImpl implements HoopoeThreadLocalCache {

        private Map cache = new HashMap();

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

        @Override
        public void clear() {
            cache.clear();
        }

    }

}
