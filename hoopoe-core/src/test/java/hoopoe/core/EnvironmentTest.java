package hoopoe.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.net.URL;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(DataProviderRunner.class)
public class EnvironmentTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @DataProvider
    public static Object[][] testEnvironment() {
        return new Object[][] {
                new EnvironmentTestData() {{
                    expectedConfigProfile = Environment.DEFAULT_CONFIG_PROFILE;
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "";
                    expectedConfigProfile = Environment.DEFAULT_CONFIG_PROFILE;
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "config.profile=myProfile";
                    expectedConfigProfile = "myProfile";
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "config.profile=";
                    expectedExceptionMessage = "Invalid arguments provided: config.profile=";
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "=value";
                    expectedExceptionMessage = "Invalid arguments provided: =value";
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "config.profile=myProfile=what";
                    expectedExceptionMessage = "Invalid arguments provided: config.profile=myProfile=what";
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "not.known=hello,another=world";
                    expectedConfigProfile = Environment.DEFAULT_CONFIG_PROFILE;
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "config.profile=myProfile,unknown=42";
                    expectedConfigProfile = "myProfile";
                }}.build(),

                new EnvironmentTestData() {{
                    agentArgs = "config.profile=myProfile,config.file=/i/do/not/exist";
                    expectedExceptionMessage = "Invalid path supplied for custom config file: /i/do/not/exist";
                }}.build(),

                new EnvironmentTestData() {{
                    initializer = (temporaryFolder) -> {
                        File file = temporaryFolder.newFile();
                        agentArgs = "config.profile=myProfile,config.file=" + file.getAbsolutePath();
                        expectedCustomConfigFile = file.toURI().toURL();
                    };
                    expectedConfigProfile = "myProfile";
                }}.build(),

                new EnvironmentTestData() {{
                    initializer = (temporaryFolder) -> {
                        System.setProperty("user.home", temporaryFolder.getRoot().getAbsolutePath());
                        File file = new File(temporaryFolder.newFolder(".hoopoe"), "hoopoe-config.yml");
                        file.createNewFile();
                        expectedCustomConfigFile = file.toURI().toURL();
                    };
                    expectedConfigProfile = Environment.DEFAULT_CONFIG_PROFILE;
                }}.build(),
        };
    }

    @Test
    @UseDataProvider
    public void testEnvironment(EnvironmentTestData testData) throws Exception {
        if (testData.expectedExceptionMessage != null) {
            expectedException.expectMessage(testData.expectedExceptionMessage);
        }

        testData.initializer.init(temporaryFolder);

        Environment environment = new Environment(testData.agentArgs);
        assertThat(environment.getConfigurationProfileName(), equalTo(testData.expectedConfigProfile));
        assertThat(environment.getCustomConfigFile(), equalTo(testData.expectedCustomConfigFile));
        assertThat(environment.getDefaultConfigFile(), notNullValue());
        assertThat(environment.getDefaultConfigFile(), equalTo(this.getClass().getResource("/hoopoe-config.yml")));
    }

    protected static class EnvironmentTestData {
        protected String agentArgs;
        protected String expectedExceptionMessage;
        protected String expectedConfigProfile;
        protected URL expectedCustomConfigFile;
        protected Initializer initializer = (temporaryFolder) -> {
        };

        public Object[] build() {
            return new Object[] {this};
        }

        public interface Initializer {
            void init(TemporaryFolder temporaryFolder) throws Exception;
        }
    }

}