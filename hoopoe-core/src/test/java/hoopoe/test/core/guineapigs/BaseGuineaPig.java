package hoopoe.test.core.guineapigs;

public class BaseGuineaPig {

    public void emptyMethod() {

    }

    public void simpleMethod() {
        String s = "some string";
    }

    public int methodWithOneInnerCall() {
        simpleMethod();
        return 42;
    }

    public int methodWithTwoInnerCalls() {
        simpleMethod();
        emptyMethod();
        return 42;
    }

    public void callsPrivateMethod() {
        privateMethod();
    }

    private void privateMethod() {
        int i = 42;
    }

    public void callsStaticMethod() {
        staticMethod();
    }

    public static void staticMethod() {
        long l = 323;
    }

    public void callsMethodWithParams() {
        methodWithParams(42);
    }

    public void methodWithParams(int i) {
        String s = "42";
    }

    public void methodWithConstructorCall() {
        new ApprenticeGuineaPig();
    }

    public void methodWithCallTree() {
        emptyMethod();
        ApprenticeGuineaPig apprenticeGuineaPig = new ApprenticeGuineaPig("tada");
        apprenticeGuineaPig.someSimpleMethod();
        apprenticeGuineaPig.callBack(this);
    }

    public void methodWithException() {
        int i = 42;
        throw new IllegalStateException();
    }

}
