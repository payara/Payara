#!/bin/bash
set -e
set -o pipefail

databases="ANIMALS,PLANTS,VIRUSES"
passwordfile="./passwordfile.txt"
Field_Separator=$IFS
IFS=','

# don't forget to install the JDBC driver!
# asadmin --user admin --passwordfile "${passwordfile}" add-library "./mysql-connector-java-8.0.18.jar";

for dbName in ${databases};
do
    echo "Creating pool and resource to ${dbName}";
    poolName="pool-${dbName}";
    dsName="jdbc/ds_${dbName}";
    
    asadmin --user admin --passwordfile "${passwordfile}" create-jdbc-connection-pool --ping --restype javax.sql.DataSource --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource --steadypoolsize 5 --maxpoolsize 20 --validationmethod auto-commit --property user=mysql:password=mysqlpassword:DatabaseName=${dbName}:ServerName=tc-mysql:port=3306:useSSL=false:zeroDateTimeBehavior=CONVERT_TO_NULL:useUnicode=true:serverTimezone=UTC:characterEncoding=UTF-8:useInformationSchema=true:nullCatalogMeansCurrent=true:nullNamePatternMatchesAll=false "${poolName}";
    
    asadmin --user admin --passwordfile "${passwordfile}" create-jdbc-resource --connectionpoolid "${poolName}" "${dsName}";
done
 
IFS=$Field_Separator
