package hoopoe.plugins;

import hoopoe.api.HoopoeInvocationAttribute;
import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.api.plugins.HoopoeMethodInfo;
import hoopoe.api.plugins.HoopoePlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler.sql")
public class SqlQueriesPlugin implements HoopoePlugin {

    public static final String ATTRIBUTE_NAME = "SQL Query";

    private static final ThreadLocal<Collection<String>> queryCache = ThreadLocal.withInitial(ArrayList::new);

    private static final HoopoeInvocationRecorder WRITE_QUERY_CACHE_RECORDER = (arguments, returnValue, thisInMethod) -> {
        String query = (String) arguments[0];
        queryCache.get().add(query);

        log.debug("registering query: {}", query);
        return Collections.emptyList();
    };

    private static final HoopoeInvocationRecorder READ_QUERY_CACHE_RECORDER = (arguments, returnValue, thisInMethod) -> {
        Collection<String> queries = queryCache.get();
        log.debug("recording registered queries: {}", queries);

        if (queries.isEmpty()) {
            return Collections.singleton(
                    HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, "unknown query"));
        } else {
            queryCache.remove();
            return queries.stream()
                    .map(query -> HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, query))
                    .collect(Collectors.toList());
        }
    };

    private static final HoopoeInvocationRecorder SIMPLE_QUERY_RECORDER = (arguments, returnValue, thisInMethod) -> {
        String query = (String) arguments[0];

        log.debug("recording query: {}", query);
        return Collections.singleton(HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, query));
    };

    @Override
    public HoopoeInvocationRecorder createRecorderIfSupported(HoopoeMethodInfo methodInfo) {
        if (methodInfo.isInstanceOf("java.sql.Connection")) {
            switch (methodInfo.getMethodSignature()) {
                case "prepareCall(java.lang.String)":
                case "prepareCall(java.lang.String,int,int)":
                case "prepareCall(java.lang.String,int,int,int)":
                case "prepareStatement(java.lang.String)":
                case "prepareStatement(java.lang.String,int)":
                case "prepareStatement(java.lang.String,int[])":
                case "prepareStatement(java.lang.String,int,int)":
                case "prepareStatement(java.lang.String,int,int,int)":
                case "prepareStatement(java.lang.String,java.lang.String[])":
                    return WRITE_QUERY_CACHE_RECORDER;
            }
        }

        if (methodInfo.isInstanceOf("java.sql.PreparedStatement")) {
            switch (methodInfo.getMethodSignature()) {
                case "execute()":
                case "executeQuery()":
                case "executeUpdate()":
                case "executeBatch()":
                    return READ_QUERY_CACHE_RECORDER;
            }
        }

        if (methodInfo.isInstanceOf("java.sql.Statement")) {
            switch (methodInfo.getMethodSignature()) {
                case "execute(java.lang.String)":
                case "execute(java.lang.String,int)":
                case "execute(java.lang.String,int[])":
                case "execute(java.lang.String,java.lang.String[])":
                case "executeQuery(java.lang.String)":
                case "executeUpdate(java.lang.String)":
                case "executeUpdate(java.lang.String,int)":
                case "executeUpdate(java.lang.String,int[])":
                case "executeUpdate(java.lang.String,java.lang.String[])":
                    return SIMPLE_QUERY_RECORDER;

                case "addBatch(java.lang.String)":
                    return WRITE_QUERY_CACHE_RECORDER;

                // works in conjunction with addBatch(java.lang.String)
                case "executeBatch()":
                    return READ_QUERY_CACHE_RECORDER;
            }
        }

        return null;
    }
}
