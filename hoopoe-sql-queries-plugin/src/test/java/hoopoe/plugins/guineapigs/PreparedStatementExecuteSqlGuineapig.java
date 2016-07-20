package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatementExecuteSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "select 'prepared statement execute' from dual");
        statement.execute();
    }
}
