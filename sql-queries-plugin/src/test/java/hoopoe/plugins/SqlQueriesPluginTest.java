package hoopoe.plugins;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.plugins.guineapigs.AbstractSqlGuineapig;
import hoopoe.plugins.guineapigs.PreparedStatementExecuteBatchSqlGuineapig;
import hoopoe.plugins.guineapigs.PreparedStatementExecuteQuerySqlGuineapig;
import hoopoe.plugins.guineapigs.PreparedStatementExecuteSqlGuineapig;
import hoopoe.plugins.guineapigs.PreparedStatementExecuteUpdateSqlGuineapig;
import hoopoe.plugins.guineapigs.StatementExecuteBatchSqlGuineapig;
import hoopoe.plugins.guineapigs.StatementExecuteQuerySqlGuineapig;
import hoopoe.plugins.guineapigs.StatementExecuteSqlGuineapig;
import hoopoe.plugins.guineapigs.StatementExecuteUpdateSqlGuineapig;
import hoopoe.test.supplements.HoopoeTestHelper;
import hoopoe.test.supplements.TestClassLoader;
import hoopoe.test.supplements.TestConfiguration;
import hoopoe.test.supplements.TestConfigurationRule;
import hoopoe.test.supplements.TestItem;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class SqlQueriesPluginTest {

    @Rule
    public TestConfigurationRule configurationRule = new TestConfigurationRule();

    @DataProvider
    public static Object[][] dataForTestSqlPlugin() {
        return HoopoeTestHelper.transform(
                new SqlTestItem("Statement.execute(String)",
                        StatementExecuteSqlGuineapig.class,
                        "org.h2.jdbc.JdbcStatement.execute(java.lang.String)",
                        "select 'execute statement' from dual"),

                new SqlTestItem("Statement.executeQuery(String)",
                        StatementExecuteQuerySqlGuineapig.class,
                        "org.h2.jdbc.JdbcStatement.executeQuery(java.lang.String)",
                        "select 'executeQuery statement' from dual"),

                new SqlTestItem("Statement.executeUpdate(String)",
                        StatementExecuteUpdateSqlGuineapig.class,
                        "org.h2.jdbc.JdbcStatement.executeUpdate(java.lang.String)",
                        "create table executeUpdateTable"),

                new SqlTestItem("Statement.executeBatch()",
                        StatementExecuteBatchSqlGuineapig.class) {{
                    Set<String> queries = new HashSet<>(Arrays.asList(
                            "create table firstTable()",
                            "create table secondTable()"
                    ));
                    expectedQueries.put("org.h2.jdbc.JdbcStatement.executeBatch()", queries);
                }},

                new SqlTestItem("PreparedStatement.execute()",
                        PreparedStatementExecuteSqlGuineapig.class,
                        "org.h2.jdbc.JdbcPreparedStatement.execute()",
                        "select 'prepared statement execute' from dual"),

                new SqlTestItem("PreparedStatement.executeQuery()",
                        PreparedStatementExecuteQuerySqlGuineapig.class,
                        "org.h2.jdbc.JdbcPreparedStatement.executeQuery()",
                        "select 'prepared statement execute query' from dual"),

                new SqlTestItem("PreparedStatement.executeUpdate()",
                        PreparedStatementExecuteUpdateSqlGuineapig.class,
                        "org.h2.jdbc.JdbcPreparedStatement.executeUpdate()",
                        "create table executePreparedStatementUpdateTable"),

                new SqlTestItem("PreparedStatement.executeBatch()",
                        PreparedStatementExecuteBatchSqlGuineapig.class,
                        "org.h2.jdbc.JdbcPreparedStatement.executeBatch()",
                        "select 'prepared statement execute batch' from dual")
        );
    }

    @UseDataProvider("dataForTestSqlPlugin")
    @Test
    public void testSqlPlugin(SqlTestItem testItem) throws Exception {
        when(TestConfiguration.getPluginsProviderMock().createPlugins())
                .thenReturn(Collections.singleton(new SqlQueriesPlugin()));

        TestClassLoader classLoader = new TestClassLoader("hoopoe.plugins.guineapigs", "org.h2");

        HoopoeProfiledInvocation profiledInvocation = HoopoeTestHelper.getSingleProfiledInvocationWithAgentLoaded(() -> {
            Class instrumentedClass = classLoader.loadClass(testItem.guineapigClass.getCanonicalName());
            Object guineapig = instrumentedClass.newInstance();
            Method method = instrumentedClass.getMethod("executeCodeUnderTest");
            method.invoke(guineapig);
        });

        Map<String, Set<String>> capturedData = new HashMap<>();
        profiledInvocation.flattened()
                .forEach(invocation ->
                        invocation.getAttributes().stream()
                                .filter(attribute -> attribute.getName().equals(SqlQueriesPlugin.ATTRIBUTE_NAME))
                                .forEach(attribute -> {
                                    String callee = invocation.getClassName() + "." + invocation.getMethodSignature();
                                    Set<String> queries = capturedData.get(callee);
                                    if (queries == null) {
                                        queries = new HashSet<>();
                                        capturedData.put(callee, queries);
                                    }
                                    queries.add(attribute.getDetails());
                                }));

        assertThat(capturedData, equalTo(testItem.expectedQueries));
    }

    public static class SqlTestItem extends TestItem {

        protected Map<String, Set<String>> expectedQueries = new HashMap<>();
        protected Class<? extends AbstractSqlGuineapig> guineapigClass;

        public SqlTestItem(String description, Class<? extends AbstractSqlGuineapig> guineapigClass) {
            super(description);
            this.guineapigClass = guineapigClass;
        }

        public SqlTestItem(String description, Class<? extends AbstractSqlGuineapig> guineapigClass,
                           String expectedCallee, String expectedQuery) {
            this(description, guineapigClass);
            expectedQueries.put(expectedCallee, Collections.singleton(expectedQuery));
        }
    }

}