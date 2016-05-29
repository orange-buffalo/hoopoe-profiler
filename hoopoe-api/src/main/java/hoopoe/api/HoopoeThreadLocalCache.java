package hoopoe.api;

public interface HoopoeThreadLocalCache {

    void set(Object key, Object value);

    <T> T get(Object key);

    void remove(Object key);

}
