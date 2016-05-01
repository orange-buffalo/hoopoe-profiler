package hoopoe.core;

import static hoopoe.core.HoopoeProfiler.LOG_CATEGORY;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LOG_CATEGORY)
class HoopoeClassFileTransformer implements ClassFileTransformer {

    private static final String BEFORE_METHOD_CALL =
            "Profiler.beforeMethod(\"%s\", \"%s\", $args);";
    private static final String AFTER_METHOD_CALL =
            "Profiler.afterMethod(\"%s\", \"%s\", $args);";

    private ClassPool pool = ClassPool.getDefault();

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        String canonicalClassName = className.replaceAll("/", ".");
//                if (canonicalClassName.startsWith("java.lang.")) {
//                    return classfileBuffer;
//                }
//
//                if (canonicalClassName.startsWith("java.util.")) {
//                    return classfileBuffer;
//                }
//
//                if (canonicalClassName.startsWith("java.io")) {
//                    return classfileBuffer;
//                }
//
//                if (canonicalClassName.startsWith("java.math")) {
//                    return classfileBuffer;
//                }
//
//                if (canonicalClassName.startsWith("java.net.")) {
//                    return classfileBuffer;
//                }

        if (canonicalClassName.startsWith("hoopoe")) {
            return classfileBuffer;
        }

//                if (canonicalClassName.startsWith("java.util.")) {
//                    return classfileBuffer;
//                }
//
//                if (canonicalClassName.startsWith("java.util.")) {
//                    return classfileBuffer;
//                }

        try {
//                    pool.insertClassPath(
//                            new ByteArrayClassPath(className, classfileBuffer));
//                    String canonicalClassName = className.replaceAll("/", ".");
            CtClass ctClass = pool.get(canonicalClassName);
            if (!ctClass.isFrozen()) {
                for (CtMethod currentMethod : ctClass.getDeclaredMethods()) {
                    String methodName = currentMethod.getLongName();

                    currentMethod.insertBefore(
                            String.format(BEFORE_METHOD_CALL, canonicalClassName, methodName));

                    currentMethod.insertAfter(
                            String.format(AFTER_METHOD_CALL, canonicalClassName, methodName),
                            true);
                }
                return ctClass.toBytecode();
            }
            else {
                log.info("{} is frozen", className);
            }
        }
        catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

}
