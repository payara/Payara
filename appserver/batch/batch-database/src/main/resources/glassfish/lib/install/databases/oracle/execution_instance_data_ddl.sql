SET SERVEROUTPUT ON;

DECLARE
 vCtr     Number;
 vSQL     VARCHAR2(2000); 
 vcurrSchema VARCHAR2(256);
BEGIN

  SELECT sys_context( 'userenv', 'current_schema' ) into vcurrSchema from dual;
  dbms_output.put_line('Current Schema: '||vcurrSchema);

  SELECT COUNT(*)  
    INTO vCtr  
    FROM user_tables  
    WHERE table_name = 'EXECUTIONINSTANCEDATA';
 
  IF vCtr = 0 THEN
    dbms_output.put_line('Creating EXECUTIONINSTANCEDATA table');
    vSQL := 'CREATE TABLE EXECUTIONINSTANCEDATA
    (	
         jobexecid                     NUMBER(19,0) PRIMARY KEY,
         jobinstanceid                 NUMBER(19,0),
         createtime                    TIMESTAMP,
         starttime                     TIMESTAMP,
         endtime                       TIMESTAMP,
         updatetime                    TIMESTAMP,
         parameters                    BLOB,
         batchstatus                   VARCHAR2(512),
         exitstatus                    VARCHAR2(512),
         CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES JOBINSTANCEDATA (jobinstanceid)
    )';  
   EXECUTE IMMEDIATE vSQL;   
  END IF;

  -- create the sequence if necessary
  SELECT COUNT(*) INTO vCtr FROM user_sequences
  WHERE sequence_name = 'EXECUTIONINSTANCEDATA_SEQ';
  IF vCtr = 0 THEN
    vSQL := 'CREATE SEQUENCE EXECUTIONINSTANCEDATA_SEQ';
    EXECUTE IMMEDIATE vSQL;
  END IF;

  -- create index trigger if necessary 
  SELECT COUNT(*) INTO vCtr FROM user_triggers
  WHERE table_name = 'JOBINSTANCEDATA_TRG';
  IF vCtr = 0 THEN  
    vSQL := 'CREATE OR REPLACE TRIGGER EXECUTIONINSTANCEDATA_TRG
                 BEFORE INSERT ON EXECUTIONINSTANCEDATA
                 FOR EACH ROW
                 BEGIN
                   SELECT EXECUTIONINSTANCEDATA_SEQ.nextval INTO :new.jobexecid FROM dual;
                 END;';
    EXECUTE IMMEDIATE vSQL;    
  END IF;  
    
END;
/
