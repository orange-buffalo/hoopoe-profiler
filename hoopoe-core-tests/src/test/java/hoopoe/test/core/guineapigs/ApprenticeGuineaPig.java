package hoopoe.test.core.guineapigs;

public class ApprenticeGuineaPig {

    private String s;

    public ApprenticeGuineaPig() {
    }

    public ApprenticeGuineaPig(String s) {
        this.s = s;
        try {
            Thread.sleep(30);
        }
        catch (InterruptedException e) {
        }
    }

    public String someSimpleMethod() {
        try {
            Thread.sleep(20);
        }
        catch (InterruptedException e) {
        }
        return s;
    }

    public void callBack(BaseGuineaPig base) {
        base.methodWithOneInnerCall();
    }

}
