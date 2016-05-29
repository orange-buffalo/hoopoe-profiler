package hoopoe.test.core.supplements;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class HoopoeTestConfigurationRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                HoopoeTestConfiguration.resetMocks();
                base.evaluate();
            }
        };
    }

}
