package hoopoe.api;

// todo javadoc: constructor is not pluginable
public interface HoopoePlugin {

    HoopoePluginAction createActionIfSupported(HoopoeMethodInfo methodInfo);
}
