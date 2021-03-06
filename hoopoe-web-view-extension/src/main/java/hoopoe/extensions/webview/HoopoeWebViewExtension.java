package hoopoe.extensions.webview;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.configuration.HoopoeConfigurableComponent;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.extensions.webview.controllers.JsonRpcServletHolder;
import hoopoe.extensions.webview.controllers.ProfilerService;
import hoopoe.extensions.webview.controllers.ProfilerServiceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.util.resource.Resource;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeWebViewExtension
        implements HoopoeProfilerExtension, HoopoeConfigurableComponent<Configuration> {

    static {
        Log.setLog(new Slf4jLog(log.getName()));
    }

    private HoopoeProfiler profiler;

    private Configuration configuration = new Configuration();

    @Override
    public void init(HoopoeProfiler profiler) {
        this.profiler = profiler;

        Integer port = configuration.getPort();
        try {
            log.info("starting web view at port {}", port);

            Server server = new Server(port);

            GzipHandler gzipHandler = new GzipHandler();
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] {
                    setupResourcesHandler(),
                    setupRpcHandler(server),
                    setupFallbackHandler()
            });
            gzipHandler.setHandler(handlers);

            server.setHandler(gzipHandler);

            server.start();

        } catch (Exception e) {
            throw new IllegalStateException("Cannot start web view at port " + port, e);
        }
    }

    private Handler setupFallbackHandler() {
        return new AbstractHandler() {

            @Override
            public void handle(
                    String target,
                    Request baseRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                if (response.isCommitted() || baseRequest.isHandled()) {
                    return;
                }
                baseRequest.setHandled(true);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(MimeTypes.Type.TEXT_HTML.toString());
                try (InputStream defaultPageStream =
                             HoopoeWebViewExtension.class.getResourceAsStream("/static/index.html")) {
                    String defaultPageContent = IOUtils.toString(defaultPageStream, StandardCharsets.UTF_8);
                    response.getWriter().write(defaultPageContent);
                }
            }

        };
    }

    private ContextHandler setupResourcesHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/static", false, false));

        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/*");
        contextHandler.setHandler(resourceHandler);
        contextHandler.setClassLoader(HoopoeWebViewExtension.class.getClassLoader());

        return contextHandler;
    }

    private ServletContextHandler setupRpcHandler(Server server) {
        ServletContextHandler servletContextHandler =
                new ServletContextHandler(server, "/rpc", ServletContextHandler.NO_SESSIONS);

        ObjectMapper objectMapper = createObjectMapper();

        servletContextHandler.addServlet(
                new JsonRpcServletHolder(objectMapper, new ProfilerServiceImpl(profiler), ProfilerService.class),
                "/profiler");
        servletContextHandler.setClassLoader(HoopoeWebViewExtension.class.getClassLoader());

        return servletContextHandler;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        SimpleModule hoopoeJacksonModule = new SimpleModule("HoopoeModule", new Version(1, 0, 0, null, null, null));

        hoopoeJacksonModule.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
            @Override
            public void serialize(
                    ZonedDateTime value,
                    JsonGenerator gen,
                    SerializerProvider serializers) throws IOException {
                gen.writeString(String.valueOf(1000 * value.toEpochSecond()));
            }
        });

        objectMapper.registerModule(hoopoeJacksonModule);

        return objectMapper;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

}
