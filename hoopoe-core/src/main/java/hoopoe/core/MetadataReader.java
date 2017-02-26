package hoopoe.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

public class MetadataReader {

    /**
     * Returns a collection of distinct names (see {@link MetadataReader#getClassName(TypeDefinition)}) of provided
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
     * Gets canonical name of target class. For generics, generic parameters are excluded.
     * If class does not have a canonical name, binary class name is returned.
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
     * For constructors, class name is used as method name.
     * <p>
     * Every argument type is a canonical Java class name or primitive type name. For array arguments {@code []} is
     * added after type name. Argument types do not include generic parameters.
     * <p>
     * Examples: <p> {@code stringMethod(java.lang.String)} <p> {@code intArrayMethod(int[])} <p> {@code
     * stringArrayMethod(java.lang.String[])} <p> {@code collectionMethod(java.util.List)} <p> {@code
     * multipleArgumentsMethod(java.lang.String,int[])}
     *
     * @param methodDescription description to build signature by
     * @return method signature.
     */
    public String getMethodSignature(MethodDescription methodDescription) {
        StringBuilder builder = new StringBuilder();
        if (methodDescription.isConstructor()) {
            TypeDefinition declaringType = methodDescription.getDeclaringType();
            if (declaringType instanceof TypeDescription) {
                builder.append(((TypeDescription) declaringType).getSimpleName());
            } else {
                String typeName = declaringType.getTypeName();
                builder.append(typeName.substring(typeName.lastIndexOf('.')));
            }
        } else {
            builder.append(methodDescription.getName());
        }
        builder.append('(');

        TypeList typeDescriptions = methodDescription.getParameters().asTypeList().asErasures();
        boolean first = true;
        for (TypeDescription next : typeDescriptions) {
            if (!first) {
                builder.append(',');
            }
            builder.append(next.getCanonicalName());
            first = false;
        }

        builder.append(')');
        return builder.toString();
    }

}
