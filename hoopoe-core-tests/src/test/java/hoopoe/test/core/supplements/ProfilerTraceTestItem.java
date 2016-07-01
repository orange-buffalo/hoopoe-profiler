package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.test.supplements.TestItem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ProfilerTraceTestItem extends TestItem {

    @Setter
    @Getter
    protected Class instrumentedClass;

    public ProfilerTraceTestItem(String description) {
        super(description);
    }

    public void setupConfiguration() {
        // no op, could be overridden if needed
    }

    public abstract Class getEntryPointClass();

    public abstract void prepareTest() throws Exception;

    public abstract void executeTest() throws Exception;

    public abstract void assertCapturedData(String originalThreadName,
                                            Map<String, HoopoeProfiledInvocation> capturedData);

    protected void assertInvocationSequence(HoopoeProfiledInvocation actualInvocationsRoot,
                                            Object... expectedSequenceCalls) {
        int expectedSequenceLength = expectedSequenceCalls.length / 2;
        List<HoopoeProfiledInvocation> actualInvocations =
                actualInvocationsRoot.flattened().collect(Collectors.toList());

        assertThat(actualInvocations + " do not fit " + Arrays.toString(expectedSequenceCalls),
                actualInvocations.size(), equalTo(expectedSequenceLength));
        for (int i = 0; i < expectedSequenceLength; i++) {
            Class expectedClass = (Class) expectedSequenceCalls[2 * i];
            String expectedMethodSignature = (String) expectedSequenceCalls[2 * i + 1];

            HoopoeProfiledInvocation actualInvocation = actualInvocations.get(i);
            assertThat(actualInvocation.getClassName(), equalTo(expectedClass.getCanonicalName()));
            assertThat(actualInvocation.getMethodSignature(), equalTo(expectedMethodSignature));
        }
    }

}
