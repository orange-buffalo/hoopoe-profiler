package hoopoe.core.instrumentation;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.HoopoeProfilerFacade;
import hoopoe.core.components.PluginsManager;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.experimental.Builder;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import testies.PhilipJFryI;
import testies.TurangaLeela;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

public class CodeInstrumentationTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private PluginsManager pluginsManagerMock;

    @Mock
    private ClassMetadataReader classMetadataReaderMock;

    @Mock
    private HoopoeConfiguration hoopoeConfigurationMock;

    private List<MethodInvocation> actualInvocations = new ArrayList<>();

    @Before
    public void setup() {
        HoopoeProfilerFacade.methodInvocationProfiler = (
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
        HoopoeProfilerFacade.enabled = true;
    }

    @After
    public void tearDown() {
        HoopoeProfilerFacade.methodInvocationProfiler = null;
        HoopoeProfilerFacade.enabled = false;
    }

    @Test
    public void testConstructorIsInstrumented() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);

        instrumentedClass.newInstance();

        assertInvocations(
                MethodInvocation.builder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>()")
                        .thisInMethod(null)
                        .returnValue(null)
                        .pluginActionIndicies(0)
        );
    }

    @Test
    public void testStaticInitializationIsInstrumented() throws Exception {
        Class<?> instrumentClass = instrumentClass(TurangaLeela.class);
        // force class initialization
        instrumentClass.getField("yearBorn").set(null, 2975);

        assertInvocations(
                MethodInvocation.builder()
                        .className("testies.TurangaLeela")
                        .methodSignature("<clinit>()")
                        .thisInMethod(null)
                        .returnValue(null)
                        .pluginActionIndicies(0)
        );
    }

    private void assertInvocations(MethodInvocation.MethodInvocationBuilder... expectedInvocationBuilders) {
        List<MethodInvocation> expectedInvocations = Stream.of(expectedInvocationBuilders)
                .map(MethodInvocation.MethodInvocationBuilder::build)
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

        assertTrue("Unexpected invocations have been recorded: " + actualInvocations,
                actualInvocations.isEmpty());

        assertTrue("Expected invocations have not been triggered: " + expectedInvocations,
                expectedInvocations.isEmpty());
    }

    private Class<?> instrumentClass(
            Class<?> sourceClass) throws IOException, ClassNotFoundException {

        CodeInstrumentation codeInstrumentation = new CodeInstrumentation(
                pluginsManagerMock, classMetadataReaderMock, hoopoeConfigurationMock);

        CodeInstrumentationClassLoader classLoader = new CodeInstrumentationClassLoader(sourceClass);

        Instrumentation instrumentation = ByteBuddyAgent.install();
        codeInstrumentation.createClassFileTransformer(instrumentation);

        Class<?> instrumentedClass = classLoader.loadClass(sourceClass.getCanonicalName(), true);

        codeInstrumentation.unload();

        return instrumentedClass;
    }

    @Builder
    @ToString
    private static class MethodInvocation {
        private String className;
        private String methodSignature;
        private long pluginActionIndicies;
        private Object[] args;
        private Object returnValue;
        private Object thisInMethod;
    }

}