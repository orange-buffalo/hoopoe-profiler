package hoopoe.test.supplements;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestConfigurationRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                TestConfiguration.resetMocks();
                base.evaluate();
            }
        };
    }

}
