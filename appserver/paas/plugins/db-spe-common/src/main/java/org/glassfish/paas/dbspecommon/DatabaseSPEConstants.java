package org.glassfish.paas.dbspecommon;


/**
 * Common Constants used by the Database Service Provisioning Engines
 *
 * @author Shalini M
 */
public interface DatabaseSPEConstants {

    /**
     * Service Configuration related properties
     */
    static final String INIT_SQL_SVC_CONFIG = "database.init.sql";

    static final String DATABASE_NAME_SVC_CONFIG = "database.name";

    /**
     * Service Characteristics related properties
     */
    static final String SERVICE_TYPE = "service-type";

    static final String RDBMS_ServiceType = "Database";

    static final String RESOURCE_TYPE = "resourcetype";

    static final String SERVICE_INIT_TYPE_LAZY = "lazy";

    /**
     * Provisioned service related constants
     */
    static final String VIRTUAL_MACHINE_ID = "vm-id";

    static final String VIRTUAL_MACHINE_IP_ADDRESS = "ip-address";

    /**
     * Database connectivity related constants
     */
    static final String USER = "user";

    static final String PASSWORD = "password";

    static final String DATABASENAME = "databasename";

    static final String PORT = "port";

    static final String HOST = "host";

    static final String URL = "URL";

    static final String CLASSNAME = "classname";

    static final String DATASOURCE = "javax.sql.DataSource";

}
