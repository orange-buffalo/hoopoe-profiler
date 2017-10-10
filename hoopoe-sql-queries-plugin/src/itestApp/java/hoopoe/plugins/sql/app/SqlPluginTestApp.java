package hoopoe.plugins.sql.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan
@EnableAsync
public class SqlPluginTestApp {

    public static void main(String args[]) {
        SpringApplication.run(SqlPluginTestApp.class, args);
    }

}
