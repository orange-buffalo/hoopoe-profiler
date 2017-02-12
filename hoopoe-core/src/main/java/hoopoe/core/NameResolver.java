package hoopoe.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

public class NameResolver {


    public Collection<String> getSuperclasses(TypeDefinition classDescription) {
        Set<String> superclasses = new HashSet<>();
        collectSuperclasses(classDescription, superclasses);
        return Collections.unmodifiableSet(superclasses);
    }

    public void collectSuperclasses(TypeDefinition classDescription, Set<String> superclasses) {
        classDescription.getInterfaces().asErasures().forEach(
                interfaceDescription -> {
                    superclasses.add(getClassName(interfaceDescription));
                    collectSuperclasses(interfaceDescription, superclasses);
                }
        );

        TypeDescription.Generic superClassGeneric = classDescription.getSuperClass();
        if (superClassGeneric != null) {
            TypeDefinition superClassDescription = superClassGeneric.asErasure();
            superclasses.add(getClassName(superClassDescription));
            collectSuperclasses(superClassDescription, superclasses);
        }
    }

    public String getClassName(TypeDefinition classDescription) {
        return classDescription.getTypeName();
    }

    public String getMethodSignature(MethodDescription methodDescription) {
        StringBuilder builder = new StringBuilder();
        if (methodDescription.isConstructor()) {
            TypeDefinition declaringType = methodDescription.getDeclaringType();
            if (declaringType instanceof TypeDescription) {
                builder.append(((TypeDescription) declaringType).getSimpleName());
            }
            else {
                String typeName = declaringType.getTypeName();
                builder.append(typeName.substring(typeName.lastIndexOf('.')));
            }
        }
        else {
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
