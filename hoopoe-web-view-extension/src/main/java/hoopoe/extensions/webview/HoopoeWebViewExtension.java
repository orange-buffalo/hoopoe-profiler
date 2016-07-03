package hoopoe.extensions.webview;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import java.util.Collections;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

public class HoopoeWebViewExtension implements HoopoeProfilerExtension {

    private HoopoeProfiler profiler;

    @Override
    public void init() {
        Thread thread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(HoopoeWebViewExtension.class.getClassLoader());

            SpringApplication application = new SpringApplication();
            application.setBannerMode(Banner.Mode.OFF);
            application.setLogStartupInfo(false);
            application.addInitializers(applicationContext ->
                    applicationContext.getBeanFactory().registerSingleton("hoopoeStorage", profiler.getStorage()));
            application.setSources(Collections.singleton(HoopoeWebViewApplication.class));

            //todo setup port here
//            application.setDefaultProperties(Collections.singletonMap(""));

            application.run();
        });
        thread.setDaemon(false);
        thread.start();
    }

    // todo common code should be reusable
    @Override
    public void setupProfiler(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }

}
