package hoopoe.tests.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

public class IntegrationTestExtension implements HoopoeProfilerExtension {

    @Override
    public void init(HoopoeProfiler profiler) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectWriter profilesResultWriter = objectMapper.writerFor(HoopoeProfiledResult.class);

            ContextHandler contextHandler = new ContextHandler();
            contextHandler.setContextPath("/*");
            contextHandler.setHandler(new AbstractHandler() {
                @Override
                public void handle(
                        String target,
                        Request baseRequest,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException, ServletException {

                    if (target.contains("startProfiling")) {
                        profiler.startProfiling();

                    } else if (target.contains("stopProfiling")) {
                        HoopoeProfiledResult profiledResult = profiler.stopProfiling();
                        profilesResultWriter.writeValue(response.getWriter(), profiledResult);

                    } else if (target.contains("heartbeat")) {
                        response.getWriter().write("alive");
                    }
                }
            });
            contextHandler.setClassLoader(IntegrationTestExtension.class.getClassLoader());

            Server server = new Server(getServerPort());
            server.setHandler(contextHandler);
            server.start();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int getServerPort() {
        String portStr = System.getenv("HOOPOE_IT_EXT_PORT");
        if (portStr != null) {
            return Integer.valueOf(portStr);
        }
        return 1234342;
    }

}
