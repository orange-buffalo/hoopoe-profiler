package hoopoe.core;

import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeInvocationAttribute;
import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.core.components.ExtensionsManager;
import hoopoe.core.components.PluginsManager;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.instrumentation.CodeInstrumentation;
import hoopoe.core.tracer.ThreadTracer;
import hoopoe.core.tracer.TraceNode;
import hoopoe.core.tracer.TraceNormalizer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HoopoeProfilerImplTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private Configuration configurationMock;

    @Mock
    private CodeInstrumentation codeInstrumentationMock;

    @Mock
    private TraceNormalizer traceNormalizerMock;

    @Mock
    private PluginsManager pluginsManagerMock;

    @Mock
    private ExtensionsManager extensionsManagerMock;

    @InjectMocks
    private HoopoeProfilerImpl profiler;

    @After
    public void tearDown() {
        HoopoeProfilerFacade.profilingStartTime = 0;
        HoopoeProfilerFacade.enabled = false;
        HoopoeProfilerFacade.methodInvocationProfiler = null;
    }

    @Test
    public void testExtensionsAreInitialized() {
        verify(extensionsManagerMock).initExtensions(eq(profiler));
    }

    @Test
    public void testInvocationProfilerIsFilledOnHoopoeFacade() {
        assertThat("methodInvocationProfiler should be populated after profiler is created",
                HoopoeProfilerFacade.methodInvocationProfiler, notNullValue());
    }

    @Test
    public void testCodeInstrumentationIsTriggered() {
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        profiler.instrument(instrumentation);

        verify(codeInstrumentationMock).createClassFileTransformer(eq(instrumentation));
        verifyNoMoreInteractions(codeInstrumentationMock);
    }

    @Test
    public void testStartProfiling() {
        profiler.startProfiling();

        assertTrue("Facade should be enabled", HoopoeProfilerFacade.enabled);
        assertThat("profilingStartTime should be updated on facade",
                HoopoeProfilerFacade.profilingStartTime, greaterThan(0L));
    }

    @Test
    public void testStopProfiling() {
        HoopoeProfilerFacade.enabled = true;

        HoopoeProfiledResult profiledResult = new HoopoeProfiledResult(Collections.emptyList());
        when(traceNormalizerMock.calculateProfiledResult(anyCollection())).thenReturn(profiledResult);

        HoopoeProfiledResult actualProfiledResult = profiler.stopProfiling();

        assertFalse("Facade should be disabled after stopping profiling", HoopoeProfilerFacade.enabled);
        assertThat("Calculation of profiled result should be delegated to TraceNormalizer",
                actualProfiledResult, is(profiledResult));
    }

    @Test
    public void testIsProfilingWithDisabledFacade() {
        assertFalse("Should be false when facade is disabled", profiler.isProfiling());
    }

    @Test
    public void testIsProfilingWithEnabledFacade() {
        HoopoeProfilerFacade.enabled = true;
        assertTrue("Should be true when facade is enabled", profiler.isProfiling());
    }

    @Test
    public void testMethodInvocationProfilingWithNoPluginRecorderReferences() throws InterruptedException {
        long startTimeInNs = 42;
        long endTimeInNs = 43;
        String className = "class";
        String methodSignature = "method";
        long pluginRecordersReference = 0;
        Object[] args = {"arg"};
        String returnValue = "return";
        String thisInMethod = "this";
        String threadName = "test-thread";

        Thread thread = new Thread(() -> HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                startTimeInNs,
                endTimeInNs,
                className,
                methodSignature,
                pluginRecordersReference,
                args,
                returnValue,
                thisInMethod
        ), threadName);
        thread.start();
        thread.join();

        // need to copy collection as it will be cleared immediately after mock call
        AtomicReference<Collection<ThreadTracer>> threadTracersCaptor = new AtomicReference<>();
        when(traceNormalizerMock.calculateProfiledResult(anyCollection())).thenAnswer(invocation -> {
            threadTracersCaptor.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        });

        profiler.stopProfiling();

        // plugin manager should not be requested in pluginRecordersReference is empty (0)
        verifyNoMoreInteractions(pluginsManagerMock);

        Collection<ThreadTracer> actualThreadTracers = threadTracersCaptor.get();

        assertThat("ThreadTracers must not be null", actualThreadTracers, notNullValue());
        assertThat("ThreadTracers must contain single element", actualThreadTracers.size(), equalTo(1));

        ThreadTracer actualThreadTracer = actualThreadTracers.iterator().next();
        assertThat("Proper thread name must be captured",
                actualThreadTracer.getThreadName(), equalTo(threadName));

        List<TraceNode> traceNodes = actualThreadTracer.getTraceNodes();
        assertThat("Trace nodes may not be null", traceNodes, notNullValue());
        assertThat("Single trace node is expected", traceNodes.size(), equalTo(1));

        TraceNode traceNode = traceNodes.get(0);
        assertThat("Start time should be properly propagated",
                traceNode.getStartTimeInNs(), equalTo(startTimeInNs));

        assertThat("End time should be properly propagated",
                traceNode.getEndTimeInNs(), equalTo(endTimeInNs));

        assertThat("Class name should be properly propagated",
                traceNode.getClassName(), equalTo(className));

        assertThat("Method signature should be properly propagated",
                traceNode.getMethodSignature(), equalTo(methodSignature));

        assertThat("Attributes should be null for invocation without plugin recorders",
                traceNode.getAttributes(), nullValue());

        assertThat("Child nods should not be initialized",
                traceNode.getChildren(), nullValue());
    }

    @Test
    public void testMethodInvocationProfilingWithPluginRecorderReferences() throws InterruptedException {
        long startTimeInNs = 42;
        long endTimeInNs = 43;
        String className = "class";
        String methodSignature = "method";
        long pluginRecordersReference = 3;
        Object[] args = {"arg"};
        String returnValue = "return";
        String thisInMethod = "this";
        String threadName = "test-thread";

        HoopoeInvocationRecorder recorderMock = Mockito.mock(HoopoeInvocationRecorder.class);
        Collection<HoopoeInvocationAttribute> attributes = new ArrayList<>();
        when(recorderMock.getAttributes(args, returnValue, thisInMethod)).thenReturn(attributes);

        Collection<HoopoeInvocationRecorder> recorders = Collections.singleton(recorderMock);
        when(pluginsManagerMock.getRecorders(eq(pluginRecordersReference))).thenReturn(recorders);

        Thread thread = new Thread(() -> HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                startTimeInNs,
                endTimeInNs,
                className,
                methodSignature,
                pluginRecordersReference,
                args,
                returnValue,
                thisInMethod
        ), threadName);
        thread.start();
        thread.join();

        // need to copy collection as it will be cleared immediately after mock call
        AtomicReference<Collection<ThreadTracer>> threadTracersCaptor = new AtomicReference<>();
        when(traceNormalizerMock.calculateProfiledResult(anyCollection())).thenAnswer(invocation -> {
            threadTracersCaptor.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        });

        profiler.stopProfiling();

        Collection<ThreadTracer> actualThreadTracers = threadTracersCaptor.get();

        assertThat("ThreadTracers must not be null", actualThreadTracers, notNullValue());
        assertThat("ThreadTracers must contain single element", actualThreadTracers.size(), equalTo(1));

        ThreadTracer actualThreadTracer = actualThreadTracers.iterator().next();
        assertThat("Proper thread name must be captured",
                actualThreadTracer.getThreadName(), equalTo(threadName));

        List<TraceNode> traceNodes = actualThreadTracer.getTraceNodes();
        assertThat("Trace nodes may not be null", traceNodes, notNullValue());
        assertThat("Single trace node is expected", traceNodes.size(), equalTo(1));

        TraceNode traceNode = traceNodes.get(0);
        assertThat("Start time should be properly propagated",
                traceNode.getStartTimeInNs(), equalTo(startTimeInNs));

        assertThat("End time should be properly propagated",
                traceNode.getEndTimeInNs(), equalTo(endTimeInNs));

        assertThat("Class name should be properly propagated",
                traceNode.getClassName(), equalTo(className));

        assertThat("Method signature should be properly propagated",
                traceNode.getMethodSignature(), equalTo(methodSignature));

        assertThat("Attributes should properly propagated",
                traceNode.getAttributes(), equalTo(attributes));

        assertThat("Child nods should not be initialized",
                traceNode.getChildren(), nullValue());
    }

    @Test
    public void testMethodInvocationProfilingWithMultipleThreads() throws InterruptedException {
        String firstThreadClassName = "class1";
        String secondThreadClassName = "class2";
        String firstThreadName = "test-thread-1";
        String secondThreadName = "test-thread-2";

        Thread thread = new Thread(() -> HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                11,
                22,
                firstThreadClassName,
                "",
                0,
                new Object[] {},
                null,
                null
        ), firstThreadName);
        thread.start();
        thread.join();

        thread = new Thread(() -> HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                11,
                22,
                secondThreadClassName,
                "",
                0,
                new Object[] {},
                null,
                null
        ), secondThreadName);
        thread.start();
        thread.join();

        // need to copy collection as it will be cleared immediately after mock call
        AtomicReference<Collection<ThreadTracer>> threadTracersCaptor = new AtomicReference<>();
        when(traceNormalizerMock.calculateProfiledResult(anyCollection())).thenAnswer(invocation -> {
            threadTracersCaptor.set(new ArrayList<>(invocation.getArgument(0)));
            return null;
        });

        profiler.stopProfiling();

        Collection<ThreadTracer> actualThreadTracers = threadTracersCaptor.get();

        assertThat("ThreadTracers must not be null", actualThreadTracers, notNullValue());
        assertThat("ThreadTracers must contain two elements, one per thread", actualThreadTracers.size(), equalTo(2));

        boolean firstThreadFound = false;
        boolean secondThreadFound = false;
        for (ThreadTracer actualThreadTracer : actualThreadTracers) {
            if (firstThreadName.equals(actualThreadTracer.getThreadName())) {
                firstThreadFound = true;
                TraceNode actualTraceNode = actualThreadTracer.getTraceNodes().get(0);
                assertThat("Proper invocation should be reported in each thread",
                        actualTraceNode.getClassName(), equalTo(firstThreadClassName));

            } else if (secondThreadName.equals(actualThreadTracer.getThreadName())) {
                secondThreadFound = true;
                TraceNode actualTraceNode = actualThreadTracer.getTraceNodes().get(0);
                assertThat("Proper invocation should be reported in each thread",
                        actualTraceNode.getClassName(), equalTo(secondThreadClassName));
            } else {
                fail(actualThreadTracer.getThreadName() + " is not expected to be recorded");
            }
        }
        assertTrue("Both threads should be reported", firstThreadFound && secondThreadFound);
    }

    @Test
    public void testGetConfiguration() {
        assertThat("Proper configuration should be returned", profiler.getConfiguration(), equalTo(configurationMock));
    }
}