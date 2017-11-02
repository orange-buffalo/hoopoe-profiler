package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import hoopoe.core.Environment;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ConfigurationDataReaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Environment environmentMock;

    @Mock
    private YamlDocumentsReader yamlDocumentsReaderMock;

    @Mock
    private MapsMerger mapsMergerMock;

    @InjectMocks
    private ConfigurationDataReader configurationDataReader;

    private URL defaultConfigFile;

    private URL customConfigFile;

    @Before
    public void beforeEachTest() throws IOException {
        defaultConfigFile = temporaryFolder.newFile().toURI().toURL();
        customConfigFile = temporaryFolder.newFile().toURI().toURL();
        when(environmentMock.getConfigurationProfileName()).thenReturn(Environment.DEFAULT_CONFIG_PROFILE);
    }

    @Test
    public void testReadValueFromDefaultConfigurationOnly() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        Map<String, Object> defaultProfile = new HashMap<>(ImmutableMap.of("test", 1));
        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Collections.singletonList(defaultProfile));

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        assertThat(actualConfiguration, equalTo(defaultProfile));

        // verify that both custom and default config urls were requested
        verify(environmentMock).getCustomConfigFile();
        verify(environmentMock).getDefaultConfigFile();

        // no merge expected as only default config, only default profile is used
        verifyNoMoreInteractions(mapsMergerMock);
    }

    @Test
    public void testCustomConfigOverridesDefaultConfig() throws IOException {
        mockConfigFiles(defaultConfigFile, customConfigFile);

        Map<String, Object> defaultProfileInDefaultConfiguration = new HashMap<>(ImmutableMap.of("test", 1));
        Map<String, Object> defaultProfileInCustomConfiguration = new HashMap<>(ImmutableMap.of("test", 42));
        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Collections.singletonList(defaultProfileInDefaultConfiguration))
                .thenReturn(Collections.singletonList(defaultProfileInCustomConfiguration));

        when(mapsMergerMock.mergeMaps(defaultProfileInDefaultConfiguration, defaultProfileInCustomConfiguration))
                .thenReturn(defaultProfileInCustomConfiguration);

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        assertThat(actualConfiguration, equalTo(defaultProfileInCustomConfiguration));

        // only default profile is used in both custom and default config, thus one merge request
        verify(mapsMergerMock).mergeMaps(defaultProfileInDefaultConfiguration, defaultProfileInCustomConfiguration);
        verifyNoMoreInteractions(mapsMergerMock);
    }

    @Test
    public void testExceptionWhenDefaultConfigurationIsMissing() throws IOException {
        mockConfigFiles(null, null);
        expectedException.expectMessage("Default configuration is not defined. Possibly agent is assembled wrongly.");

        configurationDataReader.readConfiguration();
    }

    @Test
    public void testExceptionWhileReadingUrl() throws IOException {
        URL notExistingConfigFile = new URL("file:/i-do-not-exist");
        mockConfigFiles(notExistingConfigFile, null);

        expectedException.expectMessage("Cannot read " + notExistingConfigFile);

        configurationDataReader.readConfiguration();
    }

    @Test
    public void testExceptionWhenDefaultProfileIsDuplicated() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Arrays.asList(new HashMap<>(), new HashMap<>()));

        expectedException.expectMessage("Default profile was defined multiple times in " + defaultConfigFile);

        configurationDataReader.readConfiguration();
    }

    @Test
    public void testExceptionWhenCustomProfileIsDuplicated() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Arrays.asList(
                        new HashMap<>(ImmutableMap.of("core", ImmutableMap.of("profile", "my-profile"))),
                        new HashMap<>(ImmutableMap.of("core", ImmutableMap.of("profile", "my-profile")))
                ));

        expectedException.expectMessage("Profile my-profile was defined multiple times in " + defaultConfigFile);

        configurationDataReader.readConfiguration();
    }

    @Test
    public void testCustomProfileInDefaultConfiguration() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        when(environmentMock.getConfigurationProfileName()).thenReturn("test-profile");

        HashMap<String, Object> customProfileInDefaultConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "test-profile"),
                "test", "42"
        ));
        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Collections.singletonList(customProfileInDefaultConfiguration));

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        assertThat(actualConfiguration, equalTo(customProfileInDefaultConfiguration));

        // only custom profile is used in default configuration, no need to merge
        verifyNoMoreInteractions(mapsMergerMock);
    }

    @Test
    public void testConfigurationFileHasDifferentProfile() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        when(environmentMock.getConfigurationProfileName()).thenReturn("test-profile");

        HashMap<String, Object> customProfileInDefaultConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "prod-profile"),
                "test", "42"
        ));
        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Collections.singletonList(customProfileInDefaultConfiguration));

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        // as profile in file does not match selected profile, no data must be loaded
        assertThat(actualConfiguration, equalTo(new HashMap<>()));

        // only custom profile is used in default configuration, no need to merge
        verifyNoMoreInteractions(mapsMergerMock);
    }

    @Test
    public void testCustomProfileMergeWithDefaultProfile() throws IOException {
        mockConfigFiles(defaultConfigFile, null);

        when(environmentMock.getConfigurationProfileName()).thenReturn("test-profile");

        HashMap<String, Object> customProfile = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "test-profile"),
                "test", "42"
        ));
        HashMap<String, Object> defaultProfile = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "default"),
                "test", "i am default"
        ));
        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Arrays.asList(defaultProfile, customProfile));

        when(mapsMergerMock.mergeMaps(defaultProfile, customProfile))
                .thenReturn(customProfile);

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        assertThat(actualConfiguration, equalTo(customProfile));

        // only default profile is used in both custom and default config, thus one merge request
        verify(mapsMergerMock).mergeMaps(defaultProfile, customProfile);
        verifyNoMoreInteractions(mapsMergerMock);
    }

    @Test
    public void testAllWayMerge() throws IOException {
        mockConfigFiles(defaultConfigFile, customConfigFile);

        when(environmentMock.getConfigurationProfileName()).thenReturn("test-profile");

        HashMap<String, Object> customProfileInDefaultConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "test-profile"),
                "test", "custom in default"
        ));
        HashMap<String, Object> defaultProfileInDefaultConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "default"),
                "test", "default in default"
        ));
        HashMap<String, Object> customProfileInCustomConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "test-profile"),
                "test", "custom in custom"
        ));
        HashMap<String, Object> defaultProfileInCustomConfiguration = new HashMap<>(ImmutableMap.of(
                "core", ImmutableMap.of("profile", "default"),
                "test", "default in custom"
        ));

        when(yamlDocumentsReaderMock.readDocuments(any()))
                .thenReturn(Arrays.asList(defaultProfileInDefaultConfiguration, customProfileInDefaultConfiguration))
                .thenReturn(Arrays.asList(defaultProfileInCustomConfiguration, customProfileInCustomConfiguration));

        when(mapsMergerMock.mergeMaps(defaultProfileInDefaultConfiguration, customProfileInDefaultConfiguration))
                .thenReturn(customProfileInDefaultConfiguration);

        when(mapsMergerMock.mergeMaps(defaultProfileInCustomConfiguration, customProfileInCustomConfiguration))
                .thenReturn(customProfileInCustomConfiguration);

        when(mapsMergerMock.mergeMaps(customProfileInDefaultConfiguration, customProfileInCustomConfiguration))
                .thenReturn(customProfileInCustomConfiguration);

        Map<String, Object> actualConfiguration = configurationDataReader.readConfiguration();
        assertThat(actualConfiguration, equalTo(customProfileInCustomConfiguration));

        // 3 merges: profiles in both configs, then results
        verify(mapsMergerMock).mergeMaps(defaultProfileInDefaultConfiguration, customProfileInDefaultConfiguration);
        verify(mapsMergerMock).mergeMaps(defaultProfileInCustomConfiguration, customProfileInCustomConfiguration);
        verify(mapsMergerMock).mergeMaps(customProfileInDefaultConfiguration, customProfileInCustomConfiguration);
        verifyNoMoreInteractions(mapsMergerMock);
    }

    private void mockConfigFiles(
            URL defaultFile,
            URL customFile) {

        when(environmentMock.getDefaultConfigFile()).thenReturn(defaultFile);
        when(environmentMock.getCustomConfigFile()).thenReturn(customFile);
    }

}