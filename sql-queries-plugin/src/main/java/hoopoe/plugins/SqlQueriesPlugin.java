package hoopoe.plugins;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import java.util.Collections;

public class SqlQueriesPlugin implements HoopoePlugin {

    private static final String ATTRIBUTE_NAME = "SQL Query";

    @Override
    public HoopoePluginAction createActionIfSupported(HoopoeMethodInfo methodInfo) {
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
                if (methodInfo.instanceOf("java.sql.Statement")) {

                    return (arguments, returnValue, thisInMethod) -> {
                        String query = (String) arguments[0];
                        return Collections.singleton(new HoopoeAttribute(ATTRIBUTE_NAME, query, true));
                    };
                }
                break;

            case "executeBatch()":
                break;// todo addBatch(java.lang.String)

        }
        return null;
    }
}
