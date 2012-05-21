package org.glassfish.paas.mq;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.VirtualMachine;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileWriter;

/**
 * @author Jagadish Ramu
 */
@Service(name="Native-MQ")
public class MQNativeTemplateCustomizer implements TemplateCustomizer {

    @Inject
    private ServerContext serverContext;

    public void customize(VirtualCluster cluster, VirtualMachine virtualMachine) throws VirtException {
    }

    public void start(VirtualMachine virtualMachine, boolean firstStart) {
        FileWriter fw  = null;
        final String fileName = System.getProperty("java.io.tmpdir") + File.separator + "mq.plugin.broker.password.txt";
        generateMQAdminPasswordFile(fw, fileName);

        String[] startBrokerArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + ".." + File.separator + "mq"  + File.separator + "bin" + File.separator + "imqbrokerd" +
                (OS.isWindows() ? ".bat" : ""), "-passfile", fileName, "-port", Constants.MQ_PORT, "-force", "-name", Constants.MQ_BROKER_NAME};
        final ProcessExecutor startBroker = new ProcessExecutor(startBrokerArgs);
        try {

        Thread myThread = new Thread(){
            public void run(){
                try{
                    String[] output = startBroker.execute(true);
                    System.out.println(output);
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    deleteMQPasswordFile(fileName);
                }
            }
        };
            myThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteMQPasswordFile(String fileName) {
        try{
            File file = new File(fileName);
            file.delete();
        }catch(Exception e){}
    }

    public void stop(VirtualMachine virtualMachine) {
        FileWriter fw  = null;
        String fileName = System.getProperty("java.io.tmpdir") + File.separator + "mq.plugin.broker.password.txt";
        generateMQAdminPasswordFile(fw, fileName);

        String[] stopBrokerArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + ".." + File.separator + "mq"  + File.separator + "bin" + File.separator + "imqcmd" + (OS.isWindows() ? ".bat" : ""),
                "shutdown","bkr","-u","admin","-f","-passfile", fileName, "-b", "localhost:"+ Constants.MQ_PORT};
        ProcessExecutor stopBroker = new ProcessExecutor(stopBrokerArgs);

        try {
            stopBroker.execute();
        } catch (ExecException e) {
            e.printStackTrace();
        }finally{
            deleteMQPasswordFile(fileName);
        }
    }

    private void generateMQAdminPasswordFile(FileWriter fw, String fileName) {
        try {
            File file = new File(fileName);
            file.deleteOnExit();
            fw = new FileWriter(file);
            fw.write("imq.imqcmd.password=admin");
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            if(fw != null){
                try{
                    fw.close();
                }catch(Exception e){
                }
            }
        }
    }

    public void clean(VirtualMachine virtualMachine) {
    }
}

