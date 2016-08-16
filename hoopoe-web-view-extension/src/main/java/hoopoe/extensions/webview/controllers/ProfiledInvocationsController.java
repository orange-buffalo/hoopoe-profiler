package hoopoe.extensions.webview.controllers;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Singleton
public class ProfiledInvocationsController {

    @Inject
    private HoopoeProfiler profiler;

    @Path("invocations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<HoopoeProfiledResult> getInvocations() {
//        return storage.getProfiledInvocationSummaries();
        return null;
    }

    @Path("invocations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HoopoeProfiledInvocation getInvocation(@PathParam("id") String id) {
//        return storage.getProfiledInvocation(id);
        return null;
    }

}
