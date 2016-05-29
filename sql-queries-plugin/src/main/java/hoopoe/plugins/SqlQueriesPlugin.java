package hoopoe.plugins;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class SqlQueriesPlugin implements HoopoePlugin {

    private static final String ATTRIBUTE_NAME = "SQL Query";

    @Override
    public HoopoePluginAction createActionIfSupported(HoopoeMethodInfo methodInfo) {
        if (methodInfo.instanceOf("java.sql.Connection")) {
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
                    return (arguments, returnValue, thisInMethod, cache) -> {
                        cache.set(returnValue, arguments[0]);
                        return Collections.emptyList();
                    };
            }
        }

        if (methodInfo.instanceOf("java.sql.PreparedStatement")) {
            switch (methodInfo.getMethodSignature()) {
                case "execute()":
                case "executeQuery()":
                case "executeUpdate()":
                case "executeBatch()":
                    return (arguments, returnValue, thisInMethod, cache) -> {
                        String query = cache.get(thisInMethod);
                        query = query == null ? "unknown query" : query;
                        return Collections.singleton(new HoopoeAttribute(ATTRIBUTE_NAME, query, true));
                    };
            }
        }

        if (methodInfo.instanceOf("java.sql.Statement")) {
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
                    return (arguments, returnValue, thisInMethod, cache) -> {
                        String query = (String) arguments[0];
                        return Collections.singleton(new HoopoeAttribute(ATTRIBUTE_NAME, query, true));
                    };

                case "addBatch(java.lang.String)":
                    return (arguments, returnValue, thisInMethod, cache) -> {
                        Collection<String> queriesInBatch = cache.get(thisInMethod);
                        if (queriesInBatch == null) {
                            queriesInBatch = new ArrayList<>();
                            cache.set(thisInMethod, queriesInBatch);
                        }
                        queriesInBatch.add((String) arguments[0]);
                        return Collections.emptyList();
                    };

                // works in conjunction with addBatch(java.lang.String)
                case "executeBatch()":
                    return (arguments, returnValue, thisInMethod, cache) -> {
                        Collection<String> queriesInBatch = cache.get(thisInMethod);
                        if (queriesInBatch == null) {
                            return Collections.singleton(new HoopoeAttribute(ATTRIBUTE_NAME, "unknown query", true));
                        }
                        else {
                            cache.remove(thisInMethod);
                            return queriesInBatch.stream()
                                    .map(query -> new HoopoeAttribute(ATTRIBUTE_NAME, query, true))
                                    .collect(Collectors.toList());
                        }
                    };
            }
        }

        return null;
    }
}
