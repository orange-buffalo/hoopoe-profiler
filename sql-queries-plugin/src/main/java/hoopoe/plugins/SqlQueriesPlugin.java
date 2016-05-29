package hoopoe.plugins;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import java.util.Collection;

public class SqlQueriesPlugin implements HoopoePlugin {

    @Override
    public HoopoePluginAction createActionIfSupported(String className, Collection<String> superclasses, String methodSignature) {
        return null;
    }
}
