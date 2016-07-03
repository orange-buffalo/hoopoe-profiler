package hoopoe.extensions.webview.controllers;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationSummary;
import hoopoe.api.HoopoeProfilerStorage;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfiledInvocationsController {

    @Autowired
    private HoopoeProfilerStorage storage;

    @RequestMapping("invocations")
    public Collection<HoopoeProfiledInvocationSummary> getInvocations() {
        return storage.getProfiledInvocationSummaries();
    }

    @RequestMapping("invocations/{id}")
    public HoopoeProfiledInvocation getInvocation(@PathVariable String id) {
        return storage.getProfiledInvocation(id);
    }

}
