/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.paas.gfplugin;


import org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner;
import org.glassfish.paas.gfplugin.ClientRuntimeWrapper;
import org.glassfish.paas.orchestrator.provisioning.util.RemoteCommandExecutor;
import org.glassfish.embeddable.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.*;

/**
 * @author Jagadish Ramu
 */
@Service
public class GlassFishProvisioner implements ApplicationServerProvisioner {


    @Inject
    private RemoteCommandExecutor remoteCommandExecutor;

    private org.glassfish.embeddable.GlassFish glassFish;
    private CommandRunner commandRunner;
    private Deployer deployer;

    public static final String CLUSTER = "gf-cluster";
    public static final String NODE_PREFIX = "node-";
    public static final String INSTANCE_PREFIX = "instance-";

    private String userName;
    private String host;
    private String port;
    private String target;

    private String glassFishInstallDir;

    public static final String GF_PORT = "GF_PORT";
    public static final String GF_TARGET = "GF_TARGET";
    public static final String GF_HOST = "GF_HOST";

    public static final String AWS_KEYPAIR = "AWS_KEYPAIR";
    public static final String AWS_INSTANCE_USERNAME = "AWS_INSTANCE_USERNAME";
    public static final String AWS_LOCAL_KEYPAIR_LOCATION = "AWS_LOCAL_KEYPAIR_LOCATION";

    public static final String APPLICATION_SERVER_PROVIDER = "APPLICATION_SERVER_PROVIDER";
    public static final String GLASSFISH = "GLASSFISH";
    public static final String GF_INSTALL_DIR = "GF_INSTALL_DIR";

    //database/JDBC related constants
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String DATABASENAME = "databasename";
    public static final String PORTNUMBER = "portnumber";
    public static final String URL = "url";
    public static final String RESOURCE_TYPE = "resourcetype";
    public static final String CLASSNAME = "classname";

    private String instanceUserName;
    private String keyPair;
    private String awsLocalKeyPairLocation;

    public CommandRunner getCommandRunner() {
        if (commandRunner != null) {
            return commandRunner;
        }
        try {
            commandRunner = getGlassFish().getCommandRunner();
        } catch (GlassFishException e) {
            e.printStackTrace();
            throw new RuntimeException(e); //TODO exception handling

        }
        return commandRunner;
    }

    public Deployer getDeployer() {
        if (deployer != null) {
            return deployer;
        }
        try {
            deployer = getGlassFish().getDeployer();
        } catch (GlassFishException e) {
            e.printStackTrace();
            throw new RuntimeException(e); //TODO exception handling

        }
        return deployer;
    }

    public GlassFish getGlassFish() {
        if (glassFish == null) {
            // TODO :: If it is local DAS, get it from Habitat. Don't bootstrap a GlassFish Client.
            BootstrapProperties bootstrapProperties = new BootstrapProperties();
            bootstrapProperties.setProperty("GlassFish_Platform", "GlassFishClient");

            try {
                GlassFishRuntime glassFishRuntime = ClientRuntimeWrapper.bootstrap(
                        bootstrapProperties);
                System.out.println("GlassFishRuntime = [" + glassFishRuntime + "]");

                GlassFishProperties gfProperties = new GlassFishProperties();
                gfProperties.setProperty("host", host);
                gfProperties.setProperty("port", port);

                glassFish = glassFishRuntime.newGlassFish(gfProperties);
                System.out.println("GlassFish = [" + glassFish + "]");


            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e); //TODO exception handling
            }
        }
        return glassFish;
    }

    public void initialize(Properties properties) {
        userName = (String) properties.get(AWS_INSTANCE_USERNAME);

        port = (String) properties.get(GF_PORT);
        target = (String) properties.get(GF_TARGET);
        host = (String) properties.get(GF_HOST);

        instanceUserName = (String) properties.get(AWS_INSTANCE_USERNAME);
        keyPair = (String) properties.get(AWS_KEYPAIR);
        glassFishInstallDir = (String) properties.get(GF_INSTALL_DIR);
        awsLocalKeyPairLocation = (String) properties.get(AWS_LOCAL_KEYPAIR_LOCATION);

    }

    public void provisionCluster(int instancesCount, List<String> instanceIPs, String masterInstanceIP) {
        throw new RuntimeException("not supported");
/*
        createCluster(masterInstanceIP, CLUSTER);
        for (int i = 1; i <= instancesCount; i++) {
            //sleep(30); //Need this delay so that elastic ip is attached to the newly created instance.
            //TODO need to find a way (API) in ec-2 by which elastic ip is provided during creation/start of instance itself.
            provisionNode(masterInstanceIP, instanceIPs.get(i-1), CLUSTER, i);
        }
*/
    }

    public void unProvisionCluster(int instancesCount, List<String> remoteInstanceIPs, String masterInstanceIP) {
        throw new RuntimeException("not supported");

/*
        stopCluster(masterInstanceIP, CLUSTER); // stops all instances.
        for (int i = 1; i <= instancesCount; i++) {
            //String instanceIP = getIPAddress(remoteInstanceIPs.get(i - 1));
            deleteInstance(masterInstanceIP, INSTANCE_PREFIX + i);
            deleteNodeSSH(masterInstanceIP, remoteInstanceIPs.get(i-1), NODE_PREFIX + i, false);
        }

        deleteCluster(masterInstanceIP, CLUSTER, true);
*/
    }

    public int scaleUp(int count, String clusterName) {
        return -1;
    }

    public int scaleDown(int count, String clusterName) {
        return -1;
    }


    public String provisionNode(String dasIP, String instanceIP, String clusterName, String nodeName, String instanceName) {
        setupSSH(dasIP, instanceIP);
        createNodeSSH(dasIP, instanceIP, nodeName);
        createInstance(dasIP, clusterName, nodeName, instanceName);
        return instanceName;
    }

    public void unProvisionNode(String dasIP, String instanceIP, String nodeName, String instanceName) {
        stopInstance(dasIP, instanceName);
        deleteInstance(dasIP, instanceName);
        deleteNodeSSH(dasIP, instanceIP, nodeName, true);
    }


    public void stopCluster(String masterInstanceIP, String cluster) {
        String args[] = new String[]
                {
                        cluster
                };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("stop-cluster", args);
        logCommandResult(commandResult);
    }

    public void deleteJdbcConnectionPool(String masterInstanceIP, String poolName) {
        ArrayList<String> params = new ArrayList<String>();
        params.add(poolName);

        String[] parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        CommandResult commandResult = getCommandRunner().run("delete-jdbc-connection-pool", parameters);
        logCommandResult(commandResult);
    }

    public void deleteJdbcResource(String masterInstanceIP, String target, String resourceName) {
        ArrayList<String> params = new ArrayList<String>();
        params.add("--target="+target);
        params.add(resourceName);

        String[] parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        CommandResult commandResult = getCommandRunner().run("delete-jdbc-resource", parameters);
        logCommandResult(commandResult);
    }
    public void createJdbcConnectionPool(String masterInstanceIP, String target, Properties props, String poolName) {

        ArrayList<String> params = new ArrayList<String>();

        Properties properties = (Properties) props.clone();

        if (properties.get(RESOURCE_TYPE) != null) {
            params.add("--resType=" + (String) properties.get(RESOURCE_TYPE));

            properties.remove(RESOURCE_TYPE);
        }

        if (properties.get(CLASSNAME) != null) {
            params.add("--datasourceClassname=" + (String) properties.get(CLASSNAME));
            properties.remove(CLASSNAME);
        } else {
            throw new RuntimeException("classname is mandatory for creating jdbc-connection-pool");
        }

        StringBuffer poolPropertiesArgument = generatePoolProperties(properties);
        if (poolPropertiesArgument != null && poolPropertiesArgument.length() > 0) {
            params.add("--property=" + poolPropertiesArgument.toString());
        }


        if (target != null) {
            params.add("--target=" + target);
        }

        params.add(poolName);

        String[] parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        System.out.println("create-jdbc-connection-pool command params : " + Arrays.toString(parameters));
        CommandResult commandResult = getCommandRunner().run("create-jdbc-connection-pool", parameters);
        logCommandResult(commandResult);
    }

    public void createJdbcResource(String masterInstanceIP, String target, String poolName, String resourceName) {

        ArrayList<String> params = new ArrayList<String>();

        if (poolName == null) {
            throw new RuntimeException("pool-name cannot be null for create-jdbc-resource");
        }

        if (resourceName == null) {
            throw new RuntimeException("resource-name cannot be null for create-jdbc-resource");
        }

        if (target != null) {
            params.add("--target");
            params.add(target);
        }

        params.add("--connectionpoolid");
        params.add(poolName);

        params.add(resourceName);

        String[] parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        System.out.println("create-jdbc-resource command params : " + Arrays.toString(parameters));
        CommandResult commandResult = getCommandRunner().run("create-jdbc-resource", parameters);
        logCommandResult(commandResult);
    }

    public void refreshLBConfiguration(String masterInstanceIP, String lbServiceName) {

        //apply http-lb-changes to that initial setup is done.
        String command = "apply-http-lb-changes";
        String[] options = new String[]{
                lbServiceName
        };
        executeRemoteCommand(command, options);
        //TODO HACK : executing it twice makes the command succeed.
        executeRemoteCommand(command, options);

    }

    public void associateLBWithApplicationServer(String masterInstanceIP, String targetName, String lbIPAddress,
                                                 String lbServiceName) {
        String deviceHost = lbIPAddress;
        String command = "create-http-lb";
        String[] options = new String[]{
                "--target", targetName,
                "--devicehost", deviceHost,
                "--deviceport", "50443",
                lbServiceName};

        executeRemoteCommand(command, options);

        refreshLBConfiguration(masterInstanceIP, lbServiceName);
    }

    private StringBuffer generatePoolProperties(Properties properties) {

        StringBuffer propertiesString = new StringBuffer();

        for (Object key : properties.keySet()) {
            if (propertiesString.length() > 0) {
                propertiesString.append(":");
            }
            propertiesString.append(key.toString() + "=" + properties.get(key).toString());
        }

        return propertiesString;
    }

    public void enableSecureAdmin(String ipAddress) {
        //not needed (can't be used ? ) as we need to have DAS having secure-admin
        //enabled by default for the first remote communication to happen.
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "enable-secure-admin";
        String args[] = new String[]{userName, ipAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);

    }

    public void startDomain(String ipAddress, String domainName) {
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "start-domain " + domainName;
        String args[] = new String[]{userName, ipAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
    }

    public void createDomain(String domainName, String ipAddress, String... options) {
//        String args = options != null ?  "--domainproperties " + options + " " : "";
        String params = "";
        if (options != null && options.length > 0) {
            for (String option : options) {
                params = params + option.trim() + " ";
            }
        }
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "create-domain " +
                params + domainName;
        String args[] = new String[]{userName, ipAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
    }

    public void deleteDomain(String domainName, String ipAddress) {
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "delete-domain " + domainName;
        String args[] = new String[]{userName, ipAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
    }

    public void stopDomain(String ipAddress, String domainName) {
        String command = glassFishInstallDir + File.separator +
                "glassfish" + File.separator + "bin" + File.separator + "asadmin " + "stop-domain " + domainName;
        String args[] = new String[]{userName, ipAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
    }

    public void deleteCluster(String masterInstanceIP, String cluster, boolean cascade) {
        //TODO cascade should delete all instances.
        String args[] = new String[]
                {
                        cluster
                };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("delete-cluster", args);
        logCommandResult(commandResult);
    }

    public String deploy(String masterInstanceIP, String appLocation, String... options) {
        try {
            String result = getDeployer().deploy(new File(appLocation), options);
            System.out.println("deployed : " + result);
            return result;
        } catch (GlassFishException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void startCluster(String masterInstanceIP, String cluster) {
        String args[] = new String[]{
                cluster
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("start-cluster", args);
        logCommandResult(commandResult);
    }

    public void createInstance(String masterInstanceIP, String cluster, String node, String instance) {
        String args[] = null;
        if (cluster != null) {
            args = new String[]
                    {
                            "--cluster", cluster,
                            "--checkports", "false",
                            "--node", node,
                            instance
                    };
        } else {
            args = new String[]
                    {
                            "--checkports", "false",
                            "--node", node,
                            instance
                    };
        }
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("create-instance", args);
        logCommandResult(commandResult);
    }

    public void createCluster(String masterInstanceIP, String cluster) {
        String args[] = new String[]
                {
                        cluster
                };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("create-cluster", args);
        logCommandResult(commandResult);
    }

    public void createNodeSSH(String dasIp, String instanceIp, String nodeName) {
        String args[] = new String[]{
                "--sshuser", userName,
                "--sshkeyfile", keyPair,
                "--nodehost", instanceIp,
                "--installdir", glassFishInstallDir,
                nodeName
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("create-node-ssh", args);
        logCommandResult(commandResult);
    }

    public void deleteNodeSSH(String dasIp, String instanceIp, String nodeName, boolean cascade) {
        //TODO cascade should delete all instances.
        String args[] = new String[]{
                nodeName
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("delete-node-ssh", args);
        logCommandResult(commandResult);
    }

    public void deleteInstance(String dasIp, String instanceName) {
        String args[] = new String[]{
                instanceName
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("delete-instance", args);
        logCommandResult(commandResult);
    }

    public void stopInstance(String dasIp, String instanceName) {
        String args[] = new String[]{
                instanceName
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("stop-instance", args);
        logCommandResult(commandResult);
    }

    public CommandResult executeRemoteCommand(String command, String... options) {
        CommandResult commandResult = getCommandRunner().run(command, options);
        logCommandResult(commandResult);
        return commandResult;
    }

    public boolean handles(Properties metaData) {
        String value = (String) metaData.get(APPLICATION_SERVER_PROVIDER);
        if (value != null && value.equals(GLASSFISH)) {
            return true;
        }
        return false;
    }


    public void startInstance(String dasIp, String instanceName) {
        String args[] = new String[]{
                "--debug",
                instanceName
        };
        logCommand(args);
        CommandResult commandResult = getCommandRunner().run("start-instance", args);
        logCommandResult(commandResult);
    }

    private void logCommandResult(CommandResult commandResult) {
        System.out.println(commandResult.getOutput());
        if (commandResult.getFailureCause() != null) {
            System.out.println(commandResult.getFailureCause());
        }
    }

    public void setupSSH(String dasIPAddress, String instanceIPAddress) {

        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "setup-ssh " +
                " --secure=false " + "--sshuser " + userName +
                " --sshkeyfile " + keyPair + " " + instanceIPAddress;
        String args[] = new String[]{userName, dasIPAddress, awsLocalKeyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
    }

    private void logCommand(String[] args) {
/*
        if (args != null) {
            System.out.println("");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println("");
        }
*/
    }

}
