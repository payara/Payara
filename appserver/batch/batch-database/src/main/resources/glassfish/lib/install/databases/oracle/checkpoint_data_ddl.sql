SET SERVEROUTPUT ON;

DECLARE
 vCtr     Number;
 vSQL     VARCHAR2(1000); 
 vcurrSchema VARCHAR2(256);
BEGIN

  SELECT sys_context( 'userenv', 'current_schema' ) into vcurrSchema from dual;
  dbms_output.put_line('Current Schema: '||vcurrSchema);

  SELECT COUNT(*)  
    INTO vCtr  
    FROM user_tables  
    WHERE table_name = 'CHECKPOINTDATA';
 
  IF vCtr = 0 THEN
    dbms_output.put_line('Creating CHECKPOINTDATA table');
    vSQL := 'CREATE TABLE CHECKPOINTDATA
    (	
         id            VARCHAR2(512),
         obj           BLOB
    )';  
   EXECUTE IMMEDIATE vSQL;   
  END IF;

END;
/
