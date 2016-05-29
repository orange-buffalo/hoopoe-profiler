package hoopoe.api;

import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class HoopoeMethodInfo {

    @Getter
    private String canonicalClassName;

    @Getter
    private String methodSignature;

    @Getter
    private Collection<String> superclasses;

    public HoopoeMethodInfo(String canonicalClassName, String methodSignature, Collection<String> superclasses) {
        this.canonicalClassName = canonicalClassName;
        this.methodSignature = methodSignature;
        this.superclasses = superclasses;
    }

    public boolean instanceOf(String className) {
        return superclasses.contains(className);
    }
}
