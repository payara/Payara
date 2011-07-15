/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.cluster.ssh.connect;

import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.SSHCommandExecutionException;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;

import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import java.io.ByteArrayOutputStream;

public class RemoteConnectHelper  {

    private Habitat habitat;

    private Node node;

    private HashMap<String, Node> nodeMap;

    private Logger logger;

    private String dasHost = null;

    private int dasPort = -1;
    
    int commandStatus;

    String fullCommand=null;
    SSHLauncher sshL=null;


    public RemoteConnectHelper(Habitat habitat, Node[] nodes, Logger logger, String dasHost, int dasPort) {
        this.logger = logger;
        this.habitat = habitat;
        nodeMap = new HashMap<String, Node>();

        for (int i=0;i<nodes.length;i++) {
            Node n  =  nodes[i];
            nodeMap.put(n.getName(), n);
        }        
        this.dasHost=dasHost;
        this.dasPort=dasPort;

    }
        public RemoteConnectHelper(Habitat habitat, Node node, Logger logger, String dasHost, int dasPort) {
        this.logger = logger;
        this.habitat = habitat;
        this.node = node;
        this.dasHost=dasHost;
        this.dasPort=dasPort;

    }

    public boolean isLocalhost() {
        return NetUtils.isThisHostLocal(node.getNodeHost());
    }

    public boolean isRemoteConnectRequired() {

        String t = node.getType();
        if (t != null){
            if (t.equals("SSH"))
                return true;
            else
                return false;
        } else {
            return false;
        }

    }

    public String getLastCommandRun() {
        return fullCommand;
    }

    // need to get the command options that were specified too

    public int runCommand(String cmd, final ParameterMap parameters,
            StringBuilder outputString )throws SSHCommandExecutionException {

        //get the node ref and see if ssh connection is setup if so use it
        try{
 /*
            Node node = nodeMap.get(noderef);
            if (node == null){
                logger.severe("Could not find node "+ noderef);
                return 1;
            }
            */
            String nodeHome = node.getInstallDir() + File.separator + "glassfish";
            if( nodeHome == null) {  
                logger.severe("Invalid installdir "+nodeHome +" for node "+node.getName());
                return 1;
            }
            SshConnector connector = node.getSshConnector();
            if ( connector != null)  {
                sshL=habitat.getComponent(SSHLauncher.class);
                sshL.init(node, logger);

                // create command and params
                String command =new String();

                // We always pass the DAS host and port to the asadmin
                // command we are running because some local commands like
                String prefix = nodeHome +File.separator+ "bin"+ File.separator+"asadmin " +
                        " --host " + dasHost + " --port " + dasPort +
                        " " + cmd;
                String unixStyleSlash = prefix.replaceAll("\\\\","/");     
                 //get the params for the command
                // we don't validate since called by other commands directly
                String instanceName = new String();

                for (Map.Entry<String,List<String>> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    String value = parameters.getOne(key);
                    
                    if (key.equals("DEFAULT") ) {
                        instanceName = value;
                        continue;
                    }

                    // help and Xhelp are meta-options that are handled specially
                    if (key.equals("help")) {
                        continue;
                    }

                    command = command + " "+ key + " " +value;
                }
                fullCommand = unixStyleSlash + command + " " +  instanceName;

                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                commandStatus = sshL.runCommand(fullCommand, outStream);
                String results = outStream.toString();
                outputString.append(results);
                return commandStatus;              
            } 

        }catch (IOException ex) {
            String m1 = " Command execution failed. " +ex.getMessage();
            String m2 = "";
            Throwable e2 = ex.getCause();
            if(e2 != null) {
                m2 = e2.getMessage();
            }
            logger.severe("Command execution failed for "+ cmd);
            SSHCommandExecutionException cee = new SSHCommandExecutionException(StringUtils.cat(":",
                                            m1));
            cee.setSSHSettings(sshL.toString());
            cee.setCommandRun(fullCommand);
            throw cee;
            
        } catch (java.lang.InterruptedException ei){
            ei.printStackTrace();
            String m1 = ei.getMessage();
            String m2 = "";
            Throwable e2 = ei.getCause();
            if(e2 != null) {
                m2 = e2.getMessage();
            }
            logger.severe("Command interrupted "+ cmd);
            SSHCommandExecutionException cee = new SSHCommandExecutionException(StringUtils.cat(":",
                                             m1, m2));
            cee.setSSHSettings(sshL.toString());
            cee.setCommandRun(fullCommand);
            throw cee;
        }

        return commandStatus;
    }
}

