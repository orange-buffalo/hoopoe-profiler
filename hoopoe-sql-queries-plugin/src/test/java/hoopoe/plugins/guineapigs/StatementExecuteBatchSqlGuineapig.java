package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExecuteBatchSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.addBatch("create table firstTable()");
        statement.addBatch("create table secondTable()");
        statement.executeBatch();
    }
}
