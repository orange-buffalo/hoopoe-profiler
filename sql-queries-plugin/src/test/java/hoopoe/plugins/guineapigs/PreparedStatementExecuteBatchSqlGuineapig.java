package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatementExecuteBatchSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "select 'prepared statement execute batch' from dual");
        statement.executeBatch();
    }
}
