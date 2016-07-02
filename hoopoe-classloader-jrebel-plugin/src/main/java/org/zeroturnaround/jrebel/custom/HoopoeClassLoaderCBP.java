package org.zeroturnaround.jrebel.custom;

import org.zeroturnaround.bundled.javassist.*;
import org.zeroturnaround.javarebel.integration.support.JavassistClassBytecodeProcessor;

public class HoopoeClassLoaderCBP extends JavassistClassBytecodeProcessor {
    @Override public void process(ClassPool classPool, ClassLoader classLoader, CtClass ctClass) throws Exception {
        classPool.importPackage("org.zeroturnaround.javarebel");
        classPool.importPackage("org.zeroturnaround.javarebel.integration.generic");
        classPool.importPackage("org.zeroturnaround.jrebel.custom");
        classPool.importPackage("java.net");
        classPool.importPackage("java.io");

        registerClassLoader(ctClass);
        patchLoadClass(ctClass);
        patchFindResources(ctClass);
    }


    private void registerClassLoader(CtClass ctClass) throws CannotCompileException {
        for (CtConstructor c : ctClass.getDeclaredConstructors()){
            c.insertAfter("IntegrationFactory.getInstance().registerClassLoader($0, new ByteSource($0));");
        }
    }

    private void patchFindResources(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod getResourceAsStreamMethod = ctClass.getDeclaredMethod("getResourceAsStream");

        getResourceAsStreamMethod.insertBefore(
                "if (IntegrationFactory.getInstance().isResourceReplaced($0,$1)){" +
                "    URL url = IntegrationFactory.getInstance().findResource($0, $1);" +
                "    if(url != null){" +
                "       LoggerFactory.getLogger(\"Custom-Hoopoe\").info(\"Found replaced resource '\" + $1 + \"' for classloader '\" + $0 + \"'\");" +
                "       try {" +
                "           return url.openStream();" +
                "       } catch (IOException exception) {" +
                "           LoggerFactory.getLogger(\"Custom-Hoopoe\").error(exception);" +
                "       }" +
                "    }" +
                "}"
        );
    }

    private void patchLoadClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod loadClassMethod = ctClass.getDeclaredMethod("loadClass");
        loadClassMethod.insertBefore(
                "synchronized (getClassLoadingLock($1)){" +
                "   Class clazz = findLoadedClass($1);" +
                "   if (clazz != null){" +
                "      return clazz;" +
                "   }" +
                "   clazz = IntegrationFactory.getInstance().findReloadableClass($0, $1);" +
                "   if (clazz != null){" +
                "      return clazz;" +
                "   }" +
                "}"
        );
    }
}
