package hoopoe.extensions.webview;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import hoopoe.extensions.webview.controllers.RestApp;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.servlet.ServletContainer;

public class HoopoeWebViewExtension implements HoopoeProfilerExtension {

    private HoopoeProfiler profiler;

    @Override
    public void init() {
        try {
            Server server = new Server(9786);  //todo setup port here from config

            GzipHandler gzipHandler = new GzipHandler();
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] {
                    setupResourcesHandler(),
                    setupApiHandler(server),
                    setupFallbackHandler()
            });
            gzipHandler.setHandler(handlers);

            server.setHandler(gzipHandler);

            server.start();
        }
        catch (Exception e) {
            throw new IllegalStateException(e); //todo think about correct processing
        }
    }

    private Handler setupFallbackHandler() {
        return new AbstractHandler() {

            @Override
            public void handle(String target,
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

    private ServletContextHandler setupApiHandler(Server server) {
        ServletContainer servletContainer = new ServletContainer(new RestApp(profiler));

        ServletHolder servletHolder = new ServletHolder(servletContainer);

        ServletContextHandler servletContextHandler =
                new ServletContextHandler(server, "/api", ServletContextHandler.NO_SESSIONS);
        servletContextHandler.addServlet(servletHolder, "/*");
        servletContextHandler.setClassLoader(HoopoeWebViewExtension.class.getClassLoader());
        return servletContextHandler;
    }

    // todo common code should be reusable
    @Override
    public void setupProfiler(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }

}
