package hoopoe.core.instrumentation;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.HoopoeProfilerFacade;
import hoopoe.core.components.PluginsManager;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.experimental.Builder;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import testies.BenderBendingRodriguez;
import testies.PhilipJFryI;
import testies.Robot;
import testies.TurangaLeela;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CodeInstrumentationTest {

    private static final MethodInvocation.MethodInvocationBuilder[] NO_INVOCATIONS
            = new MethodInvocation.MethodInvocationBuilder[0];

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.WARN);

    @Mock
    private PluginsManager pluginsManagerMock;

    @Mock
    private ClassMetadataReader classMetadataReaderMock;

    @Mock
    private HoopoeConfiguration hoopoeConfigurationMock;

    private List<MethodInvocation> actualInvocations = new ArrayList<>();
    private Instrumentation instrumentation;

    @Before
    public void setup() {
        instrumentation = ByteBuddyAgent.install();

        HoopoeProfilerFacade.methodInvocationProfiler = (
                startTimeInNs, endTimeInNs, className, methodSignature,
                pluginRecordersReference, args, returnValue, thisInMethod) ->

                actualInvocations.add(MethodInvocation.builder()
                        .className(className)
                        .methodSignature(methodSignature)
                        .pluginRecordersReference(pluginRecordersReference)
                        .args(args)
                        .returnValue(returnValue)
                        .thisInMethod(thisInMethod)
                        .build()
                );
        HoopoeProfilerFacade.enabled = true;

        when(classMetadataReaderMock.getMethodSignature(any())).thenAnswer((Answer<String>) invocation -> {
            MethodDescription methodDescription = invocation.getArgument(0);
            return methodDescription.getInternalName();
        });
    }

    @After
    public void tearDown() {
        HoopoeProfilerFacade.methodInvocationProfiler = null;
        HoopoeProfilerFacade.enabled = false;
        HoopoeProfilerFacade.profilingStartTime = 0;
    }

    @Test
    public void testConstructorIsInstrumented() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);

        Object fry = instrumentedClass.newInstance();

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>")
                        .thisInMethod(fry)
        );
    }

    @Test
    public void testStaticInitializationIsInstrumented() throws Exception {
        Class<?> instrumentedClass = instrumentClass(TurangaLeela.class);
        // force class initialization
        instrumentedClass.getField("yearBorn").set(null, 2975);

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.TurangaLeela")
                        .methodSignature("<clinit>")
        );
    }

    @Test
    public void testMethodIsInstrumented() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        Object fry = instrumentedClass.newInstance();
        instrumentedClass.getMethod("deliverPizza", String.class).invoke(fry, "Fox Network");

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>")
                        .thisInMethod(fry),

                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("deliverPizza")
                        .thisInMethod(fry)
                        .args(new Object[] {"Fox Network"})
        );
    }

    @Test
    public void testDefaultInterfaceMethodIsInstrumented() throws Exception {
        Class<?> instrumentedClass = instrumentClass(BenderBendingRodriguez.class, Robot.class);
        Object bender = instrumentedClass.newInstance();

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.Robot")
                        .methodSignature("tankWithAlcohol")
                        .thisInMethod(bender),

                defaultInvocationBuilder()
                        .className("testies.BenderBendingRodriguez")
                        .methodSignature("<init>")
                        .thisInMethod(bender)
        );
    }

    @Test
    public void testMethodReturnedValueIsRecorded() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        instrumentedClass.getMethod("getQuote").invoke(null);

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("getQuote")
                        .returnValue("Why am I sticky and naked? Did I miss something fun?")
        );
    }

    @Test
    public void testPluginRecordersAreRequested() throws Exception {
        instrumentClass(PhilipJFryI.class);

        // verify that proper method info is requested
        verify(classMetadataReaderMock).createMethodInfo(argThat(argument ->
                argument.getInternalName().equals("<init>")
                        && argument.getDeclaringType().getTypeName().equals("testies.PhilipJFryI")
        ));

        verify(classMetadataReaderMock).createMethodInfo(argThat(argument ->
                argument.getInternalName().equals("getQuote")
                        && argument.getDeclaringType().getTypeName().equals("testies.PhilipJFryI")
        ));

        verify(classMetadataReaderMock).createMethodInfo(argThat(argument ->
                argument.getInternalName().equals("deliverPizza")
                        && argument.getDeclaringType().getTypeName().equals("testies.PhilipJFryI")
        ));

        // every method should ask for a signature
        verify(classMetadataReaderMock, times(3)).getMethodSignature(any());

        verifyNoMoreInteractions(classMetadataReaderMock);

        // verify that recorders are requested for each of instrumented methods
        verify(pluginsManagerMock, times(3)).getPluginRecordersReference(any());
        verifyNoMoreInteractions(pluginsManagerMock);
    }

    @Test
    public void testPluginRecordersArePropagated() throws Exception {
        when(pluginsManagerMock.getPluginRecordersReference(any()))
                .thenReturn(42L);

        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);

        Object fry = instrumentedClass.newInstance();

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>")
                        .thisInMethod(fry)
                        .pluginRecordersReference(42L)
        );
    }

    @Test
    public void testExcludeClassesConfigurationIsConsidered() throws Exception {
        when(hoopoeConfigurationMock.getExcludedClassesPatterns())
                .thenReturn(Collections.singleton(".*"));

        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        Object fry = instrumentedClass.newInstance();
        instrumentedClass.getMethod("deliverPizza", String.class).invoke(fry, "Fox Network");

        assertInvocations(NO_INVOCATIONS);
    }

    @Test
    public void testIncludeClassesConfigurationHasHigherPriorityThanExcludeClasses() throws Exception {
        when(hoopoeConfigurationMock.getExcludedClassesPatterns())
                .thenReturn(Collections.singleton(".*"));
        when(hoopoeConfigurationMock.getIncludedClassesPatterns())
                .thenReturn(Collections.singleton("testies\\.PhilipJFryI"));

        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        Object fry = instrumentedClass.newInstance();

        instrumentedClass = instrumentClass(TurangaLeela.class);
        // force class initialization
        instrumentedClass.getField("yearBorn").set(null, 2975);

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>")
                        .thisInMethod(fry)

                // static initializer of TurangaLeela should not be instrumented
        );
    }

    @Test
    public void testNoInvocationsRecordedWhenFacadeIsDisabled() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);

        HoopoeProfilerFacade.enabled = false;

        Object fry = instrumentedClass.newInstance();
        instrumentedClass.getMethod("deliverPizza", String.class).invoke(fry, "Fox Network");

        assertInvocations(NO_INVOCATIONS);
    }

    @Test
    public void testNoInvocationsRecordedIfMethodHadEnteredBeforeProfilingStarted() throws Exception {
        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);

        // "method enter" time is before profiling start time
        HoopoeProfilerFacade.profilingStartTime = System.nanoTime() * 2;

        Object fry = instrumentedClass.newInstance();
        instrumentedClass.getMethod("deliverPizza", String.class).invoke(fry, "Fox Network");

        assertInvocations(NO_INVOCATIONS);
    }

    @Test
    public void testMinimumTrackedTimeConfigurationIsRespected() throws Exception {
        when(hoopoeConfigurationMock.getMinimumTrackedInvocationTimeInNs())
                .thenReturn(Long.MAX_VALUE);

        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        Object fry = instrumentedClass.newInstance();
        instrumentedClass.getMethod("deliverPizza", String.class).invoke(fry, "Fox Network");

        assertInvocations(NO_INVOCATIONS);
    }

    @Test
    public void testEvenShortInvocationRecorderWhenPluginRecordersAreReferenced() throws Exception {
        when(hoopoeConfigurationMock.getMinimumTrackedInvocationTimeInNs())
                .thenReturn(Long.MAX_VALUE);
        when(pluginsManagerMock.getPluginRecordersReference(any()))
                .thenReturn(42L);

        Class<?> instrumentedClass = instrumentClass(PhilipJFryI.class);
        Object fry = instrumentedClass.newInstance();

        assertInvocations(
                defaultInvocationBuilder()
                        .className("testies.PhilipJFryI")
                        .methodSignature("<init>")
                        .thisInMethod(fry)
                        .pluginRecordersReference(42L)
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

            assertThat("Plugin recorders reference for invocation #" + invocationNo + " do not match expected one",
                    actualInvocation.pluginRecordersReference, equalTo(expectedInvocation.pluginRecordersReference));

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
            Class<?> sourceClass,
            Class<?>... additionalClassesToInstrument) throws IOException, ClassNotFoundException {

        CodeInstrumentation codeInstrumentation = new CodeInstrumentation(
                pluginsManagerMock, classMetadataReaderMock, hoopoeConfigurationMock);

        // warmup instrumentation to initialize all the internal classes and reset the transformations
        codeInstrumentation.createClassFileTransformer(instrumentation);
        codeInstrumentation.reset();

        // load target class and disable further transformations
        CodeInstrumentationClassLoader classLoader = new CodeInstrumentationClassLoader(
                ArrayUtils.add(additionalClassesToInstrument, sourceClass));
        codeInstrumentation.createClassFileTransformer(instrumentation);
        Class<?> instrumentedClass = classLoader.loadClass(sourceClass.getCanonicalName(), true);
        codeInstrumentation.unload();

        return instrumentedClass;
    }

    private MethodInvocation.MethodInvocationBuilder defaultInvocationBuilder() {
        return MethodInvocation.builder()
                .args(new Object[] {});
    }

    @Builder
    @ToString
    private static class MethodInvocation {
        private String className;
        private String methodSignature;
        private long pluginRecordersReference;
        private Object[] args;
        private Object returnValue;
        private Object thisInMethod;
    }

}