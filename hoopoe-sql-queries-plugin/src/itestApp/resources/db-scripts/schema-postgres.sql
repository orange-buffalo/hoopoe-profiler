CREATE TABLE emp (
  first_name VARCHAR(30),
  last_name  VARCHAR(30)
); -- boot-init-statement-end

CREATE TABLE company (
  name VARCHAR(100)
); -- boot-init-statement-end

CREATE FUNCTION get_emps()
  RETURNS VOID AS $$
BEGIN
  PERFORM *
  FROM emp;
END;
$$ LANGUAGE plpgsql; -- boot-init-statement-end