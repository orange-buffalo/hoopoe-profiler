package hoopoe.plugins;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoePlugin;
import java.util.Collection;

public class SqlQueriesPlugin implements HoopoePlugin {

    @Override
    public String getId() {
        return "sql-queries-plugin";
    }

    @Override
    public boolean supports(String className, Collection<String> superclasses, String methodSignature) {
        return false;
    }

    @Override
    public Collection<HoopoeAttribute> getAttributes(String className, String[] superclasses, String methodSignature, Object[] arguments) {
        return null;
    }
}
