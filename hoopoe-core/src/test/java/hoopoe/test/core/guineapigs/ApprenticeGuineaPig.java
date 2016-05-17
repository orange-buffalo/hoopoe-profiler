package hoopoe.test.core.guineapigs;

public class ApprenticeGuineaPig {

    private String s;

    public ApprenticeGuineaPig() {
    }

    public ApprenticeGuineaPig(String s) {
        this.s = s;
    }

    public String someSimpleMethod() {
        return s;
    }

    public void callBack(BaseGuineaPig base) {
        base.methodWithOneInnerCall();
    }

}
