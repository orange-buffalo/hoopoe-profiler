package hoopoe.core;

import hoopoe.api.plugins.HoopoeMethodInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import java.util.Collection;
import java.util.HashSet;

@EqualsAndHashCode
@ToString
public class HoopoeMethodInfoImpl implements HoopoeMethodInfo {

    @Getter
    private String canonicalClassName;

    @Getter
    private String methodSignature;

    @Getter
    private Collection<String> superclasses;

    // fixme superclasses should not include hostedclass
    public HoopoeMethodInfoImpl(String canonicalClassName, String methodSignature, Collection<String> superclasses) {
        this.canonicalClassName = canonicalClassName;
        this.methodSignature = methodSignature;
        this.superclasses = new HashSet<>(superclasses);
    }

    @Override      //fixme check hosted class
    public boolean isInstanceOf(String className) {
        return superclasses.contains(className);
    }
}
