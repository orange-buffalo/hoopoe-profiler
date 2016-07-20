package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExecuteSqlGuineapig extends AbstractSqlGuineapig {
    @Override
    protected void doCustomTestCode(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute("select 'execute statement' from dual");
    }
}
