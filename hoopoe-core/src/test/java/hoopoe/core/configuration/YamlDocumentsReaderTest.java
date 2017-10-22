package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.ToString;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@RunWith(DataProviderRunner.class)
public class YamlDocumentsReaderTest {

    @DataProvider
    public static Object[][] testReadDocuments() {
        return new Object[][] {
                new YamlDocumentsReaderTestData("Simple single document")
                        .withInput("simple-single-document.yml")
                        .expectDocument(ImmutableMap.of(
                                "first", ImmutableMap.of(
                                        "second", ImmutableMap.of(
                                                "third", ImmutableMap.of(
                                                        "fourth", "test"
                                                )
                                        )
                                ),
                                "bool", true,
                                "integer", 42
                        ))
                        .forDataProvider(),

                new YamlDocumentsReaderTestData("Dash-style keys should be transformed into camel-style keys")
                        .withInput("dash-style-keys.yml")
                        .expectDocument(ImmutableMap.of(
                                "myProperty", ImmutableMap.of(
                                        "anotherProperty", "42"
                                ),
                                "camelStyleProperty", true
                        ))
                        .forDataProvider(),

                new YamlDocumentsReaderTestData("Multiple documents in input file")
                        .withInput("multiple-documents.yml")
                        .expectDocument(ImmutableMap.of(
                                "myProperty", 42
                        ))
                        .expectDocument(ImmutableMap.of(
                                "anotherDocument", "test"
                        ))
                        .forDataProvider(),

                new YamlDocumentsReaderTestData("Dot-separated properties must be parsed into nested maps")
                        .withInput("nested-properties.yml")
                        .expectDocument(ImmutableMap.of(
                                "firstProperty", ImmutableMap.of(
                                        "one", "one",
                                        "two", 2
                                ),
                                "secondProperty", ImmutableMap.of(
                                        "alpha", ImmutableMap.of(
                                                "one", "a1",
                                                "two", "a2"
                                        )
                                )
                        ))
                        .forDataProvider(),

                new YamlDocumentsReaderTestData("Dot-separated properties must be parsed into nested maps")
                        .withInput("empty-property.yml")
                        .expectDocument(ImmutableMap.of(
                                "firstProperty", ImmutableMap.of(
                                        "one", "one"
                                )
                        ))
                        .forDataProvider()
        };
    }

    @Test
    @UseDataProvider
    public void testReadDocuments(YamlDocumentsReaderTestData testData) throws IOException {
        YamlDocumentsReader reader = new YamlDocumentsReader();

        Collection<Map<String, Object>> actualDocuments;
        try (InputStream inputStream = YamlDocumentsReaderTest.class.getResourceAsStream(testData.inputFileName)) {
            actualDocuments = reader.readDocuments(inputStream);
        }
        assertThat(actualDocuments, containsInAnyOrder(testData.expectedDocuments.toArray()));
    }

    @ToString(of = "caseDescription")
    public static class YamlDocumentsReaderTestData {
        private String inputFileName;
        private String caseDescription;
        private Collection<Map<String, Object>> expectedDocuments = new ArrayList<>();

        YamlDocumentsReaderTestData(String caseDescription) {
            this.caseDescription = caseDescription;
        }

        YamlDocumentsReaderTestData withInput(String fileName) {
            this.inputFileName = fileName;
            return this;
        }

        YamlDocumentsReaderTestData expectDocument(Map<String, Object> document) {
            this.expectedDocuments.add(document);
            return this;
        }

        Object[] forDataProvider() {
            return new Object[] {this};
        }
    }

}