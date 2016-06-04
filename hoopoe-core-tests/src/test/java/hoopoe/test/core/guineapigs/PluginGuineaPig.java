package hoopoe.test.core.guineapigs;

import java.io.Serializable;

public class PluginGuineaPig implements Serializable {

    public PluginGuineaPig() {
    }

    public PluginGuineaPig(Object argument) {
    }

    public int doStuff() {
        return 42;
    }

    public void testCache() {
        firstMethodInCacheTest();
        secondMethodInCacheTest();
    }

    public void firstMethodInCacheTest() {
    }

    public void secondMethodInCacheTest() {
    }

}
