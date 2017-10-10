package hoopoe.core.instrumentation;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

@Slf4j(topic = "hoopoe.profiler")
class InstrumentationListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onIgnored(
            TypeDescription typeDescription,
            ClassLoader classLoader,
            JavaModule module,
            boolean loaded) {

        if (log.isTraceEnabled()) {
            log.trace("{} is skipped", typeDescription.getName());
        }
    }

    @Override
    public void onError(
            String typeName,
            ClassLoader classLoader,
            JavaModule module,
            boolean loaded,
            Throwable throwable) {

        log.debug("error while transforming {}: {}", typeName, throwable.getMessage());
    }

    @Override
    public void onComplete(
            String typeName,
            ClassLoader classLoader,
            JavaModule module,
            boolean loaded) {

        log.trace("{} is instrumented", typeName);
    }
}
