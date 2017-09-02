package hoopoe.tests;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.output.ToStringConsumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

final class TestsCommons {

    public static void waitForContainerAndAssertOutput(Container container, String expectedOutput) {
        ToStringConsumer containerOutputConsumer = new ToStringConsumer();
        container.followOutput(containerOutputConsumer);

        TestContainersUtils.waitForContainerToStop(container, 10000);

        String containerOutput = containerOutputConsumer.toUtf8String();

        assertThat("We expect container to output something",
                containerOutput, notNullValue());

        assertThat("We expect container to output some specific thing",
                containerOutput.trim(), equalTo(expectedOutput));
    }

}
