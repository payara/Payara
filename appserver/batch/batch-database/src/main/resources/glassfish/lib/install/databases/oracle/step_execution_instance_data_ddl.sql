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
    WHERE table_name = 'STEPEXECUTIONINSTANCEDATA';
 
  IF vCtr = 0 THEN
    dbms_output.put_line('Creating STEPEXECUTIONINSTANCEDATA table');
    vSQL := 'CREATE TABLE STEPEXECUTIONINSTANCEDATA
    (	
        stepexecid                      NUMBER(19,0) PRIMARY KEY,
        jobexecid                       NUMBER(19,0),
        batchstatus                     VARCHAR2(512),
        exitstatus                      VARCHAR2(512),
        stepname                        VARCHAR2(512),
        readcount                       NUMBER(11, 0),
        writecount                      NUMBER(11, 0),
        commitcount                     NUMBER(11, 0),
        rollbackcount                   NUMBER(11, 0),
        readskipcount                   NUMBER(11, 0),
        processskipcount                NUMBER(11, 0),
        filtercount                     NUMBER(11, 0),
        writeskipcount                  NUMBER(11, 0),
        startTime                       TIMESTAMP,
        endTime                         TIMESTAMP,
        persistentData                  BLOB,
        CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES EXECUTIONINSTANCEDATA (jobexecid)
    )';  
   EXECUTE IMMEDIATE vSQL;   
  END IF;

  -- create the sequence if necessary
  SELECT COUNT(*) INTO vCtr FROM user_sequences
  WHERE sequence_name = 'STEPEXECUTIONINSTANCEDATA_SEQ';
  IF vCtr = 0 THEN
    vSQL := 'CREATE SEQUENCE STEPEXECUTIONINSTANCEDATA_SEQ';
    EXECUTE IMMEDIATE vSQL;
  END IF;

  -- create index trigger if necessary 
  SELECT COUNT(*) INTO vCtr FROM user_triggers
  WHERE table_name = 'STEPEXECUTIONINSTANCEDATA_TRG';
  IF vCtr = 0 THEN  
    vSQL := 'CREATE OR REPLACE TRIGGER STEPEXECUTIONINSTANCEDATA_TRG
                 BEFORE INSERT ON STEPEXECUTIONINSTANCEDATA
                 FOR EACH ROW
                 BEGIN
                   SELECT STEPEXECUTIONINSTANCEDATA_SEQ.nextval INTO :new.stepexecid FROM dual;
                 END;';
    EXECUTE IMMEDIATE vSQL;    
  END IF;  
    
END;
/
