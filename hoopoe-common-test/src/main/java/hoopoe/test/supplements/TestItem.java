package hoopoe.test.supplements;

public class TestItem {

    private String description;

    public TestItem(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
