package hoopoe.api;

import java.util.Collection;

public interface HoopoePlugin {

    HoopoePluginAction createActionIfSupported(String className,
                                               Collection<String> superclasses,
                                               String methodSignature);

}
