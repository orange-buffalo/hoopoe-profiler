package hoopoe.api;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;

class MethodInfo {

    @Getter
    private String className;

    @Getter
    private String methodSignature;

    private Collection<HoopoeAttribute> attributes = new ArrayList<>();

    public MethodInfo(String className, String methodSignature) {
        this.className = className;
        this.methodSignature = methodSignature;
    }

    public MethodInfo(String className, String methodSignature, Collection<HoopoeAttribute> attributes) {
        this.className = className;
        this.methodSignature = methodSignature;
        this.attributes = attributes;
    }

    public Collection<HoopoeAttribute> getAttributes() {
        return new ArrayList<>(attributes);
    }

    public void addAttribute(HoopoeAttribute attribute) {
        attributes.add(attribute);
    }

}
