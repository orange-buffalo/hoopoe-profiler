CREATE TABLE emp (
  first_name CHAR(30),
  last_name  CHAR(30)
); -- boot-init-statement-end

CREATE TABLE company (
  name CHAR(100)
); -- boot-init-statement-end

CREATE PROCEDURE get_emps()
  BEGIN
    SELECT *
    FROM emp;
  END; -- boot-init-statement-end