package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatementExecuteUpdateSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "create table executePreparedStatementUpdateTable");
        statement.executeUpdate();
    }
}
