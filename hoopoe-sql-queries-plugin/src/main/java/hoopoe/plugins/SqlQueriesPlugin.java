package hoopoe.plugins;

import hoopoe.api.plugins.HoopoeInvocationAttribute;
import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.api.plugins.HoopoeMethodInfo;
import hoopoe.api.plugins.HoopoePlugin;
import java.util.Collections;

public class SqlQueriesPlugin implements HoopoePlugin {

    public static final String ATTRIBUTE_NAME = "SQL Query";

    @Override
    public HoopoeInvocationRecorder createActionIfSupported(HoopoeMethodInfo methodInfo) {
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
                    return (arguments, returnValue, thisInMethod) -> {
                        //todo use one instance
                        // todo vendor-specific code
                        // todo javadoc on interface about minimum objects or thing about design
//                        cache.set(returnValue, arguments[0]);
                        return Collections.emptyList();
                    };
            }
        }

        if (methodInfo.isInstanceOf("java.sql.PreparedStatement")) {
            switch (methodInfo.getMethodSignature()) {
                case "execute()":
                case "executeQuery()":
                case "executeUpdate()":
                case "executeBatch()":
                    return (arguments, returnValue, thisInMethod) -> {
                        //todo see above
//                        String query = cache.get(thisInMethod);
//                        query = query == null ? "unknown query" : query;
                        //todo query
                        return Collections.singleton(HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, ""));
                    };
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
                    return (arguments, returnValue, thisInMethod) -> {
                        String query = (String) arguments[0];
                        return Collections.singleton(HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, query));
                    };

                case "addBatch(java.lang.String)":
                    return (arguments, returnValue, thisInMethod) -> {
                        // todo see above
//                        Collection<String> queriesInBatch = cache.get(thisInMethod);
//                        if (queriesInBatch == null) {
//                            queriesInBatch = new ArrayList<>();
//                            cache.set(thisInMethod, queriesInBatch);
//                        }
//                        queriesInBatch.add((String) arguments[0]);
                        return Collections.emptyList();
                    };

                // works in conjunction with addBatch(java.lang.String)
                case "executeBatch()":
                    return (arguments, returnValue, thisInMethod) -> {
                        // todo see above
//                        Collection<String> queriesInBatch = cache.get(thisInMethod);
//                        if (queriesInBatch == null) {
                            return Collections.singleton(HoopoeInvocationAttribute.withTimeContribution(ATTRIBUTE_NAME, "unknown query"));
//                        }
//                        else {
//                            cache.remove(thisInMethod);
//                            return queriesInBatch.stream()
//                                    .map(query -> new HoopoeInvocationAttribute(ATTRIBUTE_NAME, query, true))
//                                    .collect(Collectors.toList());
//                        }
                    };
            }
        }

        return null;
    }
}
