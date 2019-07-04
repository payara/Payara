package fish.payara.docker;

public class DockerConstants {

    public static final String DOCKER_IMAGE_KEY = "Image";
    public static final String DOCKER_NAME_KEY = "name";
    public static final String PAYARA_DAS_HOST = "PAYARA_DAS_HOST";
    public static final String PAYARA_DAS_PORT = "PAYARA_DAS_PORT";
    public static final String PAYARA_NODE_NAME = "PAYARA_NODE_NAME";
    public static final String PAYARA_INSTALL_DIR = "/opt/payara/payara5";
    public static final String DOCKER_HOST_CONFIG_KEY = "HostConfig";
    public static final String DOCKER_MOUNTS_KEY = "Mounts";
    public static final String DOCKER_MOUNTS_TYPE_KEY = "Type";
    public static final String DOCKER_MOUNTS_SOURCE_KEY = "Source";
    public static final String DOCKER_MOUNTS_TARGET_KEY = "Target";
    public static final String DOCKER_MOUNTS_READONLY_KEY = "ReadOnly";
    public static final String DOCKER_NETWORK_MODE_KEY = "NetworkMode";
    public static final String PAYARA_PASSWORD_FILE = "/opt/payara/passwords/passwordfile.txt";
    public static final String INSTANCE_NAME = "PAYARA_INSTANCE_NAME";
    public static final String DOCKER_CONTAINER_ENV = "Env";
    public static final String DEFAULT_IMAGE_NAME = "payara/server-node";
}
