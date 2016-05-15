package hoopoe.test.core;

import hoopoe.test.core.supplements.HoopoeTestAgent;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import java.io.IOException;
import java.lang.reflect.Method;
import javassist.CannotCompileException;
import javassist.NotFoundException;

public abstract class AbstractHoopoeProfilerTest {

    protected void executeProfiling(Class entryPointClass, String entryPointMethod) {
        HoopoeTestClassLoader classLoader;
        try {
            classLoader = new HoopoeTestClassLoader();
        }
        catch (NotFoundException | IOException | CannotCompileException e) {
            throw new IllegalStateException(e);
        }

        HoopoeTestAgent.load();

        try {
            String threadName = "testThread" + System.nanoTime();

            Class<?> instrumentedClass = classLoader.loadClass(entryPointClass.getCanonicalName());
            Object testObject = instrumentedClass.newInstance();
            Method instrumentedMethod = instrumentedClass.getDeclaredMethod(entryPointMethod);

            Thread thread = new Thread(() -> {
                try {
                    instrumentedMethod.invoke(testObject);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, threadName);
            thread.start();
            thread.join();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            HoopoeTestAgent.unload();
        }
    }

}
