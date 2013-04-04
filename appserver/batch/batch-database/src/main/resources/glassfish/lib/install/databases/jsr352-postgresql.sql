
DROP TABLE JOBSTATUS;

DROP TABLE STEPSTATUS;

DROP TABLE CHECKPOINTDATA;

DROP TABLE JOBINSTANCEDATA;

DROP TABLE EXECUTIONINSTANCEDATA;

DROP TABLE STEPEXECUTIONINSTANCEDATA;

CREATE TABLE JOBINSTANCEDATA(
  jobinstanceid		serial not null PRIMARY KEY,
  name		character varying (512), 
  apptag VARCHAR(512)
);

CREATE TABLE EXECUTIONINSTANCEDATA(
  jobexecid		serial not null PRIMARY KEY,
  jobinstanceid	bigint not null REFERENCES JOBINSTANCEDATA (jobinstanceid),
  createtime	timestamp,
  starttime		timestamp,
  endtime		timestamp,
  updatetime	timestamp,
  parameters	bytea,
  batchstatus		character varying (512),
  exitstatus		character varying (512)
);
  
CREATE TABLE STEPEXECUTIONINSTANCEDATA(
	stepexecid			serial not null PRIMARY KEY,
	jobexecid			bigint not null REFERENCES EXECUTIONINSTANCEDATA (jobexecid),
	batchstatus         character varying (512),
    exitstatus			character varying (512),
    stepname			character varying (512),
	readcount			integer,
	writecount			integer,
	commitcount         integer,
	rollbackcount		integer,
	readskipcount		integer,
	processskipcount	integer,
	filtercount			integer,
	writeskipcount		integer,
	startTime           timestamp,
	endTime             timestamp,
	persistentData		bytea
); 

CREATE TABLE JOBSTATUS (
  id		bigint not null REFERENCES JOBINSTANCEDATA (jobinstanceid),
  obj		bytea
);

CREATE TABLE STEPSTATUS(
  id		bigint not null REFERENCES STEPEXECUTIONINSTANCEDATA (stepexecid),
  obj		bytea
);

CREATE TABLE CHECKPOINTDATA(
  id		character varying (512),
  obj		bytea
);

 
