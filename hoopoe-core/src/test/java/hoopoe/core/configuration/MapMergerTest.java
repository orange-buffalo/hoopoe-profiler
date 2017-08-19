package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import lombok.ToString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(DataProviderRunner.class)
public class MapMergerTest {

    @Rule
    public ExpectedException thrownException = ExpectedException.none();

    @DataProvider
    public static Object[][] testMergeMaps() {
        return new Object[][] {
                new MergeMapsTestData("Not overridden master values must be kept")
                        .withMaster(ImmutableMap.of(
                                "k1", 1
                        ))
                        .withCustomization(Collections.emptyMap())
                        .expect(ImmutableMap.of(
                                "k1", 1
                        ))
                        .build(),

                new MergeMapsTestData("Simple override")
                        .withMaster(ImmutableMap.of(
                                "k1", 1
                        ))
                        .withCustomization(ImmutableMap.of(
                                "k1", 2
                        ))
                        .expect(ImmutableMap.of(
                                "k1", 2
                        ))
                        .build(),

                new MergeMapsTestData("Overridden value must have the same type")
                        .withMaster(ImmutableMap.of(
                                "k1", 1
                        ))
                        .withCustomization(ImmutableMap.of(
                                "k1", "i am not integer"
                        ))
                        .expectException("Custom value of 'k1' has incompatible type")
                        .build(),

                new MergeMapsTestData("Nested maps should be handled recursively")
                        .withMaster(ImmutableMap.of(
                                "k1", ImmutableMap.of(
                                        "k1.1", "master",
                                        "k1.2", 'k'
                                )

                        ))
                        .withCustomization(ImmutableMap.of(
                                "k1", ImmutableMap.of(
                                        "k1.1", "custom"
                                )
                        ))
                        .expect(ImmutableMap.of(
                                "k1", ImmutableMap.of(
                                        "k1.1", "custom",
                                        "k1.2", 'k'
                                )
                        ))
                        .build(),

                new MergeMapsTestData("Collections should not be merged")
                        .withMaster(ImmutableMap.of(
                                "k1", Arrays.asList(42, 42)
                        ))
                        .withCustomization(ImmutableMap.of(
                                "k1", Arrays.asList(88, 56)
                        ))
                        .expect(ImmutableMap.of(
                                "k1", Arrays.asList(88, 56)
                        ))
                        .build(),

                new MergeMapsTestData("Mixed case")
                        .withMaster(ImmutableMap.of(
                                "k1", ImmutableMap.of(
                                        "k1.1", "master",
                                        "k1.2", 'k'
                                ),
                                "k2", "42",
                                "k4", true

                        ))
                        .withCustomization(ImmutableMap.of(
                                "k2", "I checked twice",
                                "k3", 777L
                        ))
                        .expect(ImmutableMap.of(
                                "k1", ImmutableMap.of(
                                        "k1.1", "master",
                                        "k1.2", 'k'
                                ),
                                "k2", "I checked twice",
                                "k3", 777L,
                                "k4", true
                        ))
                        .build(),
        };
    }

    @Test
    @UseDataProvider
    public void testMergeMaps(MergeMapsTestData testData) {
        if (testData.expectedExceptionMessage != null) {
            thrownException.expectMessage(equalTo(testData.expectedExceptionMessage));
        }

        MapsMerger merger = new MapsMerger();
        Map<String, Object> actualResult = merger.mergeMaps(testData.master, testData.customization);
        assertThat(actualResult, equalTo(testData.expectedResult));
    }

    @ToString(of = "caseDescription")
    public static class MergeMapsTestData {
        private Map<String, Object> master;
        private Map<String, Object> customization;
        private Map<String, Object> expectedResult;
        private String expectedExceptionMessage;
        private String caseDescription;

        public MergeMapsTestData(String caseDescription) {
            this.caseDescription = caseDescription;
        }

        MergeMapsTestData withMaster(Map<String, Object> master) {
            this.master = master;
            return this;
        }

        MergeMapsTestData withCustomization(Map<String, Object> customization) {
            this.customization = customization;
            return this;
        }

        MergeMapsTestData expect(Map<String, Object> expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        MergeMapsTestData expectException(String message) {
            this.expectedExceptionMessage = message;
            return this;
        }

        Object[] build() {
            return new Object[] {this};
        }
    }


}