package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatementExecuteQuerySqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "select 'prepared statement execute query' from dual");
        statement.executeQuery();
    }
}
