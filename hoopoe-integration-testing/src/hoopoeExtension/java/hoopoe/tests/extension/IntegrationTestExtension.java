package hoopoe.tests.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.configuration.HoopoeConfigurableComponent;
import hoopoe.api.configuration.HoopoeConfigurationProperty;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

@Slf4j(topic = "hoopoe.profiler")
public class IntegrationTestExtension
        implements HoopoeProfilerExtension, HoopoeConfigurableComponent<IntegrationTestExtension> {

    private static final int DEFAULT_PORT = 9271;

    @Setter
    private Integer port;

    @HoopoeConfigurationProperty(key = "port")
    public Integer getPort() {
        return port;
    }

    @Override
    public void init(HoopoeProfiler profiler) {
        log.info("initializing jetty, configured port is {}", port);
        port = (port == null) ? DEFAULT_PORT : port;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectWriter profilesResultWriter = objectMapper.writerFor(HoopoeProfiledResult.class);

            ContextHandler contextHandler = new ContextHandler();
            contextHandler.setContextPath("/hoopoe-tests");
            contextHandler.setHandler(new AbstractHandler() {
                @Override
                public void handle(
                        String target,
                        Request baseRequest,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException, ServletException {

                    if (target.contains("start-profiling")) {
                        profiler.startProfiling();

                    } else if (target.contains("stop-profiling")) {
                        HoopoeProfiledResult profiledResult = profiler.stopProfiling();
                        profilesResultWriter.writeValue(response.getWriter(), profiledResult);

                    }
                }
            });
            contextHandler.setClassLoader(IntegrationTestExtension.class.getClassLoader());

            Server server = new Server(port);
            server.setHandler(contextHandler);
            server.start();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public IntegrationTestExtension getConfiguration() {
        return this;
    }
}
