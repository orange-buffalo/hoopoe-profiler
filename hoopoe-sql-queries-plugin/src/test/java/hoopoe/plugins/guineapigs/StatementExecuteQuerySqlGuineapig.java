package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExecuteQuerySqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeQuery("select 'executeQuery statement' from dual");
    }
}
