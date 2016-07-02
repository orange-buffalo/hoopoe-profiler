package hoopoe.extensions.webview;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import org.springframework.boot.SpringApplication;

public class HoopoeWebViewExtension implements HoopoeProfilerExtension {

    private HoopoeProfiler profiler;

    @Override
    public void init() {
        Thread thread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(HoopoeWebViewExtension.class.getClassLoader());
            SpringApplication.run(HoopoeWebViewApplication.class);
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
