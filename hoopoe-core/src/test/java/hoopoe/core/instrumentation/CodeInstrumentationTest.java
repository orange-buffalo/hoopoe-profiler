package hoopoe.core.instrumentation;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import testies.JohnSnow;

@RunWith(Parameterized.class)
public class CodeInstrumentationTest {

    private CodeInstrumentationTestCase instrumentationTestCase;

    private MockitoSession mockitoSession;

    public CodeInstrumentationTest(CodeInstrumentationTestCase instrumentationTestCase) {
        this.instrumentationTestCase = instrumentationTestCase;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<?> instrumentationTestData() {
        return Arrays.asList(
                CodeInstrumentationTestCase.start("testing test case")
                        .forClass(JohnSnow.class)
                        .executeWithInstrumentedClass(instrumentedClass -> {
                            Object testy = instrumentedClass.newInstance();
                            instrumentedClass.getMethod("goBehindTheWall").invoke(testy);
                            return testy;
                        })
                        .expectMethodInvocation((testy, builder) -> builder
                                .className("testies.JohnSnow")
                                .methodSignature("<init>()")
                                .thisInMethod(testy)
                                .returnValue(null)
                                .pluginActionIndicies(null)
                        )
                        .expectMethodInvocation((testy, builder) -> builder
                                .className("testies.JohnSnow")
                                .methodSignature("goBehindTheWall()")
                                .thisInMethod(testy)
                                .returnValue(null)
                                .pluginActionIndicies(null)
                        )
        );
    }

    @Before
    public void setup() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(instrumentationTestCase)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();
        instrumentationTestCase.setup();
    }

    @After
    public void tearDown() {
        instrumentationTestCase.tearDown();
        mockitoSession.finishMocking();
    }

    @Test
    public void testInstrumentation() throws Exception {
        instrumentationTestCase.execute();
    }

}