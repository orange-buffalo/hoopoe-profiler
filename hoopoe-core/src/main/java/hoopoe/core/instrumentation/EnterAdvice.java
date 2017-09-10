package hoopoe.core.instrumentation;

import net.bytebuddy.asm.Advice;

class EnterAdvice {

    @Advice.OnMethodEnter
    public static long before() {
        return System.nanoTime();
    }
}
