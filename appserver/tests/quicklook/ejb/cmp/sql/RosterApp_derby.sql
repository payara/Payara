DROP TABLE TeamPlayer ;
DROP TABLE Player ;
DROP TABLE Team ;
DROP TABLE League ;

CREATE TABLE Player 
(
	player_Id VARCHAR(255) PRIMARY KEY NOT NULL, 
	name VARCHAR(255), 
	position VARCHAR(255), 
	salary DOUBLE PRECISION NOT NULL
);

CREATE TABLE League 
(
	league_Id VARCHAR(255) PRIMARY KEY NOT NULL, 
	name VARCHAR(255), 
	sport VARCHAR(255)
);

CREATE TABLE Team 
(
	team_Id VARCHAR(255) PRIMARY KEY NOT NULL, 
	city VARCHAR(255), 
	name VARCHAR(255),
	league_Id VARCHAR(255),
	FOREIGN KEY (league_Id)   REFERENCES League (league_Id)
);

CREATE TABLE TeamPlayer 
(
	player_Id VARCHAR(255) NOT NULL, 
        team_Id VARCHAR(255) NOT NULL, 
        CONSTRAINT pk_TeamPlayer PRIMARY KEY (player_Id, team_Id),
	FOREIGN KEY (team_Id)   REFERENCES Team (team_Id),
	FOREIGN KEY (player_Id)   REFERENCES Player (player_Id)
);
