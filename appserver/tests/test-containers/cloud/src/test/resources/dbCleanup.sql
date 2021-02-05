CREATE TABLE IF NOT EXISTS `JEEVersion` (
  id int not null,
  name varchar(32),
  year int,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `Brand` (
  id int not null,
  name varchar(255),
  PRIMARY KEY (id)
);

delete from `JEEVersion` where 1=1;
delete from `Brand` where 1=1;
