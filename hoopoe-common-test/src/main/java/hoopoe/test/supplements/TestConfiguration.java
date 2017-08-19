package hoopoe.test.supplements;

import hoopoe.api.configuration.HoopoeConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
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
    private static Collection<Pattern> includeClassesPatterns;

    @Getter
    @Setter
    private static Collection<Pattern> excludeClassesPatterns;

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
    public Collection<Pattern> getIncludedClassesPatterns() {
        return includeClassesPatterns;
    }

    @Override
    public Collection<Pattern> getExcludedClassesPatterns() {
        return excludeClassesPatterns;
    }

    public static void resetMocks() {
        MockitoAnnotations.initMocks(new TestConfiguration());
        minimumTrackedInvocationTimeInNs = 0;
        includeClassesPatterns = new ArrayList<>();
        excludeClassesPatterns =  Arrays.asList(
                Pattern.compile("hoopoe\\.core\\..*"),
                Pattern.compile("org\\.mockito\\..*"),
                Pattern.compile("org\\.hamcrest\\..*")
        );
    }

}
