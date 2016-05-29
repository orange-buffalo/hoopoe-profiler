package hoopoe.api;

public interface HoopoePlugin {

    HoopoePluginAction createActionIfSupported(HoopoeMethodInfo methodInfo);
}
