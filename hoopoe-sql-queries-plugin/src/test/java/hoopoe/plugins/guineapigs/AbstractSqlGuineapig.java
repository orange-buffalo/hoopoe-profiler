package hoopoe.plugins.guineapigs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractSqlGuineapig {

    public void executeCodeUnderTest() throws SQLException, ClassNotFoundException {
        String jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        Class.forName("org.h2.Driver", true, AbstractSqlGuineapig.class.getClassLoader());
        Connection connection = DriverManager.getConnection(jdbcUrl, "", "");
        try {
            doCustomTestCode(connection);
        }
        finally {
            connection.close();
        }
    }

    protected abstract void doCustomTestCode(Connection connection) throws SQLException;

}
