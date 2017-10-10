package hoopoe.core.instrumentation;

import hoopoe.api.plugins.HoopoeMethodInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

public class ClassMetadataReader {

    /**
     * Returns a collection of distinct names (see {@link ClassMetadataReader#getClassName(TypeDefinition)}) of provided
     * class. Includes all super classes and all implemented interfaces for target class (including indirect ones).
     *
     * @param classDescription class to find super classes for.
     *
     * @return unmodifiable collection of distinct super classes names. There is no any guaranteed order of elements in
     * this collection.
     */
    public Collection<String> getSuperClassesNames(TypeDefinition classDescription) {
        Set<String> superclasses = new HashSet<>();
        collectSuperClasses(classDescription, superclasses);
        return superclasses;
    }

    private void collectSuperClasses(TypeDefinition classDescription, Set<String> superclasses) {
        classDescription.getInterfaces().asErasures().forEach(
                interfaceDescription -> {
                    superclasses.add(getClassName(interfaceDescription));
                    collectSuperClasses(interfaceDescription, superclasses);
                }
        );

        TypeDescription.Generic superClassGeneric = classDescription.getSuperClass();
        if (superClassGeneric != null) {
            TypeDefinition superClassDescription = superClassGeneric.asErasure();
            superclasses.add(getClassName(superClassDescription));
            collectSuperClasses(superClassDescription, superclasses);
        }
    }

    /**
     * Gets canonical name of target class. For generics, generic parameters are excluded. If class does not have a
     * canonical name, binary class name is returned.
     *
     * @param classDescription target class to get the name for.
     *
     * @return canonical class name.
     */
    public String getClassName(TypeDefinition classDescription) {
        TypeDescription asErasure = classDescription.asErasure();
        String className = asErasure.getCanonicalName();
        if (className == null) {
            className = asErasure.getTypeName();
        }
        return className;
    }

    /**
     * Returns method signature in format: {@code <method_name>(<argument1_type>,<argument2_type,...,<argumentN_type)}.
     * <p>
     * For constructors, simple class name is used as method name.
     * <p>
     * Every argument type is a Java class name (as defined in {@link ClassMetadataReader#getClassName(TypeDefinition)})
     * or primitive type name. For array arguments {@code []} is added after type name. Argument types do not include
     * generic parameters.
     * <p>
     * Does not include return type.
     * <p>
     * Examples: <p> {@code stringMethod(java.lang.String)} <p> {@code intArrayMethod(int[])} <p> {@code
     * stringArrayMethod(java.lang.String[])} <p> {@code collectionMethod(java.util.List)} <p> {@code
     * multipleArgumentsMethod(java.lang.String,int[])}.
     *
     * @param methodDescription description to build signature by.
     *
     * @return method signature.
     */
    public String getMethodSignature(MethodDescription methodDescription) {
        StringBuilder builder = new StringBuilder(methodDescription.getInternalName());
        builder.append('(');

        boolean first = true;
        for (TypeDescription next : methodDescription.getParameters().asTypeList().asErasures()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(next.getCanonicalName());
            first = false;
        }

        builder.append(')');
        return builder.toString();
    }

    /**
     * Returns {@link HoopoeMethodInfo} describing provided {@link MethodDescription}.
     *
     * @param method method description to adapt.
     *
     * @return instance of {@link HoopoeMethodInfo}, maybe with lazy initialization.
     */
    public HoopoeMethodInfo createMethodInfo(MethodDescription method) {
        TypeDefinition declaringType = method.getDeclaringType();
        String className = getClassName(declaringType);
        String methodSignature = getMethodSignature(method);

        // todo lazy calculate
        return new HoopoeMethodInfoImpl(
                className,
                methodSignature,
                getSuperClassesNames(declaringType));
    }

    @EqualsAndHashCode
    @ToString
    private static class HoopoeMethodInfoImpl implements HoopoeMethodInfo {

        @Getter
        private String canonicalClassName;

        @Getter
        private String methodSignature;

        @Getter
        private Collection<String> superclasses;

        public HoopoeMethodInfoImpl(
                String canonicalClassName,
                String methodSignature,
                Collection<String> superclasses) {
            this.canonicalClassName = canonicalClassName;
            this.methodSignature = methodSignature;
            this.superclasses = new HashSet<>(superclasses);
        }

        @Override      //fixme check hosted class
        public boolean isInstanceOf(String className) {
            return superclasses.contains(className);
        }
    }
}
