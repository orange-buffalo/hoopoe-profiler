package hoopoe.api;

import java.util.Collection;

public interface HoopoePlugin {

    String getId();

    boolean supports(String className, Collection<String> superclasses, String methodSignature);

    void onCall(String className, String[] superclasses, String methodSignature, Object[] arguments);

}
