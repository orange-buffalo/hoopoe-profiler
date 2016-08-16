package hoopoe.api;

// todo javadoc: constructor is not pluginable
// todo javadoc: equals and hashcode
public interface HoopoePlugin {

    HoopoePluginAction createActionIfSupported(HoopoeMethodInfo methodInfo);
}
