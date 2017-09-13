package hoopoe.core.instrumentation;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.ClassMetadataReader;
import hoopoe.core.HoopoeProfilerFacade;
import hoopoe.core.components.PluginsManager;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.experimental.Builder;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class CodeInstrumentationTestCase {

    @Mock
    private PluginsManager pluginsManagerMock;

    @Mock
    private ClassMetadataReader classMetadataReaderMock;

    @Mock
    private HoopoeConfiguration hoopoeConfigurationMock;

    private HoopoeProfilerFacade.MethodInvocationProfiler invocationProfilerStub;

    private String caseDescription;
    private CodeInstrumentation codeInstrumentation;
    private Class<?> sourceClass;
    private InstrumentedClassAction instrumentedClassAction;
    private List<ExpectedInvocationBuilder> expectedInvocationBuilders = new ArrayList<>();
    private List<MethodInvocation> actualInvocations = new ArrayList<>();

    private CodeInstrumentationTestCase(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public static CodeInstrumentationTestCase start(String caseDescription) {
        return new CodeInstrumentationTestCase(caseDescription);
    }

    public CodeInstrumentationTestCase forClass(Class<?> sourceClass) {
        this.sourceClass = sourceClass;
        return this;
    }

    public CodeInstrumentationTestCase executeWithInstrumentedClass(InstrumentedClassAction instrumentedClassAction) {
        this.instrumentedClassAction = instrumentedClassAction;
        return this;
    }

    public CodeInstrumentationTestCase expectMethodInvocation(ExpectedInvocationBuilder expectedInvocationBuilder) {
        this.expectedInvocationBuilders.add(expectedInvocationBuilder);
        return this;
    }

    public void execute() throws Exception {
        Class<?> instrumentedClass = instrumentClass();

        Object constructedObject = instrumentedClassAction.execute(instrumentedClass);

        List<MethodInvocation> expectedInvocations = expectedInvocationBuilders.stream()
                .map(builder -> {
                    MethodInvocation.MethodInvocationBuilder invocationBuilder = MethodInvocation.builder();
                    builder.buildExpectedInvocation(constructedObject, invocationBuilder);
                    return invocationBuilder.build();
                })
                .collect(Collectors.toList());

        int invocationNo = 0;
        while (!expectedInvocations.isEmpty() && !actualInvocations.isEmpty()) {
            MethodInvocation actualInvocation = actualInvocations.remove(0);
            MethodInvocation expectedInvocation = expectedInvocations.remove(0);

            assertThat("Class name recorded for invocation #" + invocationNo + " does not match expected one",
                    actualInvocation.className, equalTo(expectedInvocation.className));

            assertThat("Method signature recorded for invocation #" + invocationNo + " does not match expected one",
                    actualInvocation.methodSignature, equalTo(expectedInvocation.methodSignature));

            assertThat("Plugin indicies recorded for invocation #" + invocationNo + " do not match expected one",
                    actualInvocation.pluginActionIndicies, equalTo(expectedInvocation.pluginActionIndicies));

            assertThat("Return value recorded for invocation #" + invocationNo + " does not match expected one",
                    actualInvocation.returnValue, equalTo(expectedInvocation.returnValue));

            assertThat("'this' reference recorded for invocation #" + invocationNo + " does not match expected one",
                    actualInvocation.thisInMethod, equalTo(expectedInvocation.thisInMethod));

            assertThat("Class name recorded for invocation #" + invocationNo + " does not match expected one",
                    actualInvocation.args, equalTo(expectedInvocation.args));

            invocationNo++;
        }

        assertThat("Unexpected invocations have been recorded: " + actualInvocations,
                actualInvocations.size(), equalTo(0));

        assertThat("Expected invocations have not been triggered: " + expectedInvocations,
                expectedInvocations.size(), equalTo(0));
    }

    private Class<?> instrumentClass() throws IOException, ClassNotFoundException {
        CodeInstrumentationClassLoader classLoader = new CodeInstrumentationClassLoader(sourceClass);

        Instrumentation instrumentation = ByteBuddyAgent.install();
        codeInstrumentation.createClassFileTransformer(instrumentation);

        Class<?> instrumentedClass = classLoader.loadClass(sourceClass.getCanonicalName(), true);

        codeInstrumentation.unload();

        return instrumentedClass;
    }

    @Override
    public String toString() {
        return caseDescription;
    }

    public void setup() {
        codeInstrumentation = new CodeInstrumentation(
                pluginsManagerMock, classMetadataReaderMock, hoopoeConfigurationMock);

        invocationProfilerStub = (
                startTimeInNs, endTimeInNs, className, methodSignature,
                pluginActionIndicies, args, returnValue, thisInMethod) ->

                actualInvocations.add(MethodInvocation.builder()
                        .className(className)
                        .methodSignature(methodSignature)
                        .pluginActionIndicies(pluginActionIndicies)
                        .args(args)
                        .returnValue(returnValue)
                        .thisInMethod(thisInMethod)
                        .build()
                );

        HoopoeProfilerFacade.methodInvocationProfiler = invocationProfilerStub;
        HoopoeProfilerFacade.enabled = true;
    }

    public void tearDown() {
        HoopoeProfilerFacade.methodInvocationProfiler = null;
        HoopoeProfilerFacade.enabled = false;
    }

    @FunctionalInterface
    public interface InstrumentedClassAction {
        /**
         * Does anything necessary for the test, using instrumented class. Returns either constructed object or null -
         * will be used as input for expected invocation builder.
         */
        Object execute(Class<?> instrumentedClass) throws Exception;
    }

    @FunctionalInterface
    public interface ExpectedInvocationBuilder {
        /**
         * Builds expected invocation. First parameter is a return result of {@link hoopoe.core.instrumentation.CodeInstrumentationTestCase.InstrumentedClassAction#execute}
         */
        void buildExpectedInvocation(
                Object constructedObject,
                MethodInvocation.MethodInvocationBuilder builder);
    }

    @Builder
    @ToString
    public static class MethodInvocation {
        private String className;
        private String methodSignature;
        private Object pluginActionIndicies;
        private Object[] args;
        private Object returnValue;
        private Object thisInMethod;
    }
}
