package hoopoe.extensions.webview.controllers;

import hoopoe.api.HoopoeProfiledInvocation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfiledInvocationsController {

    public HoopoeProfiledInvocation getInvocation() {
        return null;
    }

}
