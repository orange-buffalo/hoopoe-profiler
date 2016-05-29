package hoopoe.api;

import java.util.Collection;

public interface HoopoePluginAction {

    Collection<HoopoeAttribute> getAttributes(Object[] arguments, Object returnValue);

}
