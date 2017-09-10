package hoopoe.test.supplements;

import hoopoe.api.configuration.HoopoeConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import org.mockito.MockitoAnnotations;

public class TestConfiguration implements HoopoeConfiguration {

//    @Mock
//    @Getter
//    private static HoopoePluginsProvider pluginsProviderMock;
//
//    @Mock
//    @Getter
//    private static HoopoeProfilerExtensionsProvider extensionsProviderMock;

    @Getter
    @Setter
    private static long minimumTrackedInvocationTimeInNs;

    @Getter
    @Setter
    private static Collection<String> includeClassesPatterns;

    @Getter
    @Setter
    private static Collection<String> excludeClassesPatterns;

//    @Override
//    public HoopoePluginsProvider createPluginsProvider() {
//        return pluginsProviderMock;
//    }
//
//    @Override
//    public HoopoeProfilerExtensionsProvider createProfilerExtensionProvider() {
//        return extensionsProviderMock;
//    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return minimumTrackedInvocationTimeInNs;
    }

//    @Override
//    public Collection<String> getEnabledPlugins() {
//        return Collections.emptyList();
//    }

    @Override
    public Collection<String> getIncludedClassesPatterns() {
        return includeClassesPatterns;
    }

    @Override
    public Collection<String> getExcludedClassesPatterns() {
        return excludeClassesPatterns;
    }

    public static void resetMocks() {
        MockitoAnnotations.initMocks(new TestConfiguration());
        minimumTrackedInvocationTimeInNs = 0;
        includeClassesPatterns = new ArrayList<>();
        excludeClassesPatterns =  Arrays.asList(
                "hoopoe\\.core\\..*",
                "org\\.mockito\\..*",
                "org\\.hamcrest\\..*"
        );
    }

}
