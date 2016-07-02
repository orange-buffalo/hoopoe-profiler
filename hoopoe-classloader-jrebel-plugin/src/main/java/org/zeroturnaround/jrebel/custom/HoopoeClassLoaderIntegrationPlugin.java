package org.zeroturnaround.jrebel.custom;

import org.zeroturnaround.javarebel.ClassResourceSource;
import org.zeroturnaround.javarebel.Integration;
import org.zeroturnaround.javarebel.IntegrationFactory;
import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.Plugin;

public class HoopoeClassLoaderIntegrationPlugin implements Plugin {

    public static Logger logger = LoggerFactory.getLogger("Hiberante-Validator-Custom");

    public void preinit() {
        Integration integration = IntegrationFactory.getInstance();
        ClassLoader cl = HoopoeClassLoaderIntegrationPlugin.class.getClassLoader();

        integration.addIntegrationProcessor(cl,
                "hoopoe.utils.HoopoeClassLoader",
                new HoopoeClassLoaderCBP());
    }

    public boolean checkDependencies(ClassLoader cl, ClassResourceSource crs) {
        return crs.getClassResource("hoopoe.utils.HoopoeClassLoader") != null;
    }

    public String getAuthor() {
        return "veiko.kaap@zeroturnaround.com";
    }

    public String getDescription() {
        return "";
    }

    public String getId() {
        return "hoopoe_classloader";
    }

    public String getName() {
        return "hoopoe_classloader for reloading classes with a custom classloader";
    }

    public String getWebsite() {
        return null;
    }

    public String getSupportedVersions() {
        return "";
    }

    public String getTestedVersions() {
        return "";
    }

}
