package hoopoe.test.core.supplements;

import hoopoe.test.core.ProfilerTracingTest;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ProfilerTraceTestItem {

    private String description;

    @Setter
    @Getter
    protected Class instrumentedClass;

    public ProfilerTraceTestItem(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public abstract Class getEntryPointClass();

    public abstract void prepareTest() throws Exception;

    public abstract void executeTest() throws Exception;

    public abstract void assertCapturedData(String originalThreadName,
                                            Map<String, List<ProfilerTracingTest.CapturedInvocation>> capturedData);

    protected void assertInvocationSequence(List<ProfilerTracingTest.CapturedInvocation> actualInvocations,
                                            Object... expectedSequenceCalls) {
        int expectedSequenceLength = expectedSequenceCalls.length / 2;
        assertThat(actualInvocations.size(), equalTo(expectedSequenceLength));
        for (int i = 0; i < expectedSequenceLength; i++) {
            Class expectedClass = (Class) expectedSequenceCalls[2 * i];
            String expectedMethodSignature = (String) expectedSequenceCalls[2 * i + 1];

            ProfilerTracingTest.CapturedInvocation actualInvocation = actualInvocations.get(i);
            assertThat(actualInvocation.getClassName(), equalTo(expectedClass.getCanonicalName()));
            assertThat(actualInvocation.getMethodSignature(), equalTo(expectedMethodSignature));
        }
    }

}
