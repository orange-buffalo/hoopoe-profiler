package hoopoe.api;

import java.util.Collection;

public interface HoopoePlugin {

    String getId();

    //todo make consistent String[] superclasses
    boolean supports(String className, Collection<String> superclasses, String methodSignature);

    Collection<HoopoeAttribute> getAttributes(String className,
                                              String[] superclasses,
                                              String methodSignature,
                                              Object[] arguments);

}
