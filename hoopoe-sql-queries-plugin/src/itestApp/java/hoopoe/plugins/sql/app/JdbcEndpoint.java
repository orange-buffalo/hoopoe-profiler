package hoopoe.plugins.sql.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JdbcEndpoint {

    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executor;

    @Autowired
    public JdbcEndpoint(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        // will use it to filter profiled result bu thread name
        this.executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "sql-thread"));
    }

    @RequestMapping("/heart-beat")
    public String heartBeat() {
        return "--";
    }

    @RequestMapping("/statement-execute-query")
    public String statementExecuteQuery() throws ExecutionException, InterruptedException {
        return executor.submit(() ->
                jdbcTemplate.query(
                        "SELECT last_name FROM emp WHERE first_name = 'Turanga'",
                        rs -> {
                            if (rs.next()) {
                                return rs.getString(1);
                            }
                            throw new IllegalStateException("Leela is not found!");
                        }
                )
        ).get();
    }

    @RequestMapping("/statement-execute")
    public String statementExecute() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.execute("INSERT INTO company (name) VALUES ('Planet Express')")
        ).get();
        return "statement-execute-done";
    }

    @RequestMapping("/statement-execute-update")
    public String statementExecuteUpdate() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.update("INSERT INTO company (name) VALUES ('MomCorp')")
        ).get();
        return "statement-execute-update-done";
    }

    @RequestMapping("/statement-execute-batch")
    public String statementExecuteBatch() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.batchUpdate(
                        "INSERT INTO company (name) VALUES ('Planet Express')",
                        "INSERT INTO company (name) VALUES ('MomCorp')")
        ).get();
        return "statement-execute-batch-done";
    }

    @RequestMapping("/prepared-statement-execute-query")
    public String preparedStatementExecuteQuery() throws ExecutionException, InterruptedException {
        return executor.submit(() ->
                jdbcTemplate.query(
                        "SELECT last_name FROM emp WHERE first_name = ?",
                        ps -> ps.setString(1, "Philip J."),
                        rs -> {
                            if (rs.next()) {
                                return rs.getString(1);
                            }
                            throw new IllegalStateException("Fry is not found!");
                        }
                )
        ).get();
    }

    @RequestMapping("/prepared-statement-execute")
    public String preparedStatementExecute() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.execute(
                        "INSERT INTO company (name) VALUES (?)",
                        (PreparedStatementCallback<Boolean>) ps -> {
                            ps.setString(1, "MomCorp");
                            return ps.execute();
                        }
                )
        ).get();
        return "prepared-statement-execute-done";
    }

    @RequestMapping("/prepared-statement-execute-update")
    public String preparedStatementExecuteUpdate() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.update(
                        "DELETE FROM company",
                        ps -> {
                        }
                )
        ).get();
        return "prepared-statement-execute-update-done";
    }

    @RequestMapping("/prepared-statement-execute-batch")
    public String preparedStatementExecuteBatch() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.batchUpdate(
                        "UPDATE company SET name = ? WHERE name = ?",
                        Arrays.asList(
                                new Object[] {"Planet Express", "MomCorp"},
                                new Object[] {"MomCorp", "Planet Express"}
                        )
                )
        ).get();
        return "prepared-statement-execute-batch-done";
    }

    @RequestMapping("/callable-statement-execute")
    public String callableStatementExecute() throws ExecutionException, InterruptedException {
        executor.submit(() ->
                jdbcTemplate.call(
                        con -> con.prepareCall("{call get_emps()}"),
                        Collections.emptyList()
                )
        ).get();
        return "callable-statement-execute-done";
    }

}
