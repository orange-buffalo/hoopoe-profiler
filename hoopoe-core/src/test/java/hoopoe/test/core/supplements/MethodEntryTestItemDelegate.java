package hoopoe.test.core.supplements;

import java.lang.reflect.Method;

public class MethodEntryTestItemDelegate {

    private Object object;
    private Method method;
    private Class entryPointClass;
    private ProfilerTestItem owner;
    private String methodName;

    public MethodEntryTestItemDelegate(Class entryPointClass, String methodName, ProfilerTestItem owner) {
        this.entryPointClass = entryPointClass;
        this.methodName = methodName;
        this.owner = owner;
    }

    public Class getEntryPointClass() {
        return entryPointClass;
    }

    public void prepareTest() throws Exception {
        Class instrumentedClass = owner.getInstrumentedClass();
        method = instrumentedClass.getMethod(methodName);
        object = instrumentedClass.newInstance();
    }

    public void executeTest() throws Exception {
        method.invoke(object);
    }

}
