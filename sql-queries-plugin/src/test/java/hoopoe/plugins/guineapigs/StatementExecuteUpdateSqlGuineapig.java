package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExecuteUpdateSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("create table executeUpdateTable");
    }
}
