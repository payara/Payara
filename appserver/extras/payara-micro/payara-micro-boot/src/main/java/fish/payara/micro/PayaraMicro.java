/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.micro;

import fish.payara.micro.boot.AdminCommandRunner;
import fish.payara.micro.boot.PayaraMicroBoot;
import fish.payara.micro.boot.PayaraMicroLauncher;
import fish.payara.micro.boot.loader.ExplodedURLClassloader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class PayaraMicro implements PayaraMicroBoot {
    
    
    private PayaraMicroBoot wrappee;
    private ClassLoader nestedLoader;
    
    private static PayaraMicro instance;
    private static boolean explodedJars;
    private static File explodedDir;
    
    /**
     * Tells the runtime to unpack the jars before booting
     * This must be called before getInstance
     * If you require JSP compilation you MUST call this method
     */
    public static void unpackJars() {
        explodedJars = true;
    }
    
    /**
     * Sets the directory where unpacked jars should live
     * And tells the runtime to unpack the jars
     * This must be called before getInstance
     * @param file 
     */
    public static void setUpackedJarDir(File file) {
        explodedDir = file;
        explodedJars = true;
    }

    /**
     * Obtains the static singleton instance of the Payara Micro Server. If it
     * does not exist it will be created.
     *
     * @return The singleton instance
     */
    public static PayaraMicro getInstance() {
        return getInstance(true);
    }

    /**
     * Bootstraps the PayaraMicroRuntime with all defaults and no additional
     * configuration. Functionally equivalent to
     * PayaraMicro.getInstance().bootstrap();
     *
     * @return
     */
    public static PayaraMicroRuntime bootstrap() throws BootstrapException {
        return getInstance().bootStrap();
    }

    /**
     *
     * @param create If false the instance won't be created if it has not been
     * initialised
     * @return null if no instance exists and create is false. Otherwise returns
     * the singleton instance
     */
    public static PayaraMicro getInstance(boolean create) {
        if (instance == null && create) {
            instance = new PayaraMicro();
        }
        return instance;
    }  

    @Override
    public PayaraMicro addDeployFromGAV(String GAV) {
        wrappee.addDeployFromGAV(GAV);
        return this;
    }

    @Override
    public PayaraMicro addDeployment(String pathToWar) {
        wrappee.addDeployment(pathToWar);
        return this;
    }

    @Override
    public PayaraMicro addDeploymentFile(File file) {
        wrappee.addDeploymentFile(file);
        return this;
    }

    @Override
    public PayaraMicro addRepoUrl(String... URLs) {
        wrappee.addRepoUrl(URLs);
        return this;
    }

    @Override
    public PayaraMicroRuntime bootStrap() throws BootstrapException {
        if (wrappee == null) {
            throw new BootstrapException("Could not create Runtime instance");
        }
        return wrappee.bootStrap();
    }

    @Override
    public File getAlternateDomainXML() {
        return wrappee.getAlternateDomainXML();
    }

    @Override
    public int getAutoBindRange() {
        return wrappee.getAutoBindRange();
    }

    @Override
    public String getClusterMulticastGroup() {
        return wrappee.getClusterMulticastGroup();
    }

    @Override
    public int getClusterPort() {
        return wrappee.getClusterPort();
    }

    @Override
    public int getClusterStartPort() {
        return wrappee.getClusterStartPort();
    }

    @Override
    public File getDeploymentDir() {
        return wrappee.getDeploymentDir();
    }

    @Override
    public boolean getHttpAutoBind() {
        return wrappee.getHttpAutoBind();
    }

    @Override
    public int getHttpPort() {
        return wrappee.getHttpPort();
    }

    @Override
    public String getHzClusterName() {
        return wrappee.getHzClusterName();
    }

    @Override
    public String getInstanceName() {
        return wrappee.getInstanceName();
    }

    @Override
    public int getMaxHttpThreads() {
        return wrappee.getMaxHttpThreads();
    }

    @Override
    public int getMinHttpThreads() {
        return wrappee.getMinHttpThreads();
    }

    @Override
    public File getRootDir() {
        return wrappee.getRootDir();
    }

    @Override
    public PayaraMicroRuntime getRuntime() throws IllegalStateException {
        return wrappee.getRuntime();
    }

    @Override
    public boolean getSslAutoBind() {
        return wrappee.getSslAutoBind();
    }

    @Override
    public int getSslPort() {
        return wrappee.getSslPort();
    }
    
    @Override
    public String getSslCert() {
        return wrappee.getSslCert();
    }

    @Override
    public File getUberJar() {
        return wrappee.getUberJar();
    }

    @Override
    public boolean isLite() {
        return wrappee.isLite();
    }

    @Override
    public boolean isNoCluster() {
        return wrappee.isNoCluster();
    }

    @Override
    public boolean isNoHazelcast() {
        return wrappee.isNoHazelcast();
    }

    @Override
    public PayaraMicroBoot setNoHazelcast(boolean noHazelcast) {
        wrappee.setNoHazelcast(noHazelcast);
        return this;
    }

    @Override
    public PayaraMicro setAccessLogDir(String filePath) {
        wrappee.setAccessLogDir(filePath);
        return this;
    }

    @Override
    public PayaraMicro setAccessLogFormat(String format) {
        wrappee.setAccessLogFormat(format);
        return this;
    }

    @Override
    public PayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        wrappee.setAlternateDomainXML(alternateDomainXML);
        return this;
    }

    @Override
    public PayaraMicro setApplicationDomainXML(String domainXml) {
        wrappee.setApplicationDomainXML(domainXml);
        return this;
    }

    @Override
    public PayaraMicroBoot setPreBootHandler(Consumer<AdminCommandRunner> handler) {
        wrappee.setPreBootHandler(handler);
        return this;
    }

    @Override
    public PayaraMicroBoot setPostBootHandler(Consumer<AdminCommandRunner> handler) {
        wrappee.setPostBootHandler(handler);
        return this;
    }

    @Override
    public PayaraMicro setAutoBindRange(int autoBindRange) {
        wrappee.setAutoBindRange(autoBindRange);
        return this;
    }

    @Override
    public PayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        wrappee.setClusterMulticastGroup(hzMulticastGroup);
        return this;
    }

    @Override
    public PayaraMicro setClusterPort(int hzPort) {
        wrappee.setClusterPort(hzPort);
        return this;
    }

    @Override
    public PayaraMicro setClusterStartPort(int hzStartPort) {
        wrappee.setClusterStartPort(hzStartPort);
        return this;
    }

    @Override
    public PayaraMicro setDeploymentDir(File deploymentRoot) {
        wrappee.setDeploymentDir(deploymentRoot);
        return this;
    }

    @Override
    public PayaraMicro setHttpAutoBind(boolean httpAutoBind) {
        wrappee.setHttpAutoBind(httpAutoBind);
        return this;
    }

    @Override
    public PayaraMicro setHttpPort(int httpPort) {
        wrappee.setHttpPort(httpPort);
        return this;
    }

    @Override
    public PayaraMicro setHzClusterName(String hzClusterName) {
        wrappee.setHzClusterName(hzClusterName);
        return this;
    }

    /**
     * Gets the name of the instance group
     *
     * @return The name of the instance group
     */
    @Override
    public String getInstanceGroup() {
        return wrappee.getInstanceGroup();
    }

    /**
     * Sets the instance group name
     *
     * @param instanceGroup The instance group name
     * @return
     */
    @Override
    public PayaraMicro setInstanceGroup(String instanceGroup) {
        wrappee.setInstanceGroup(instanceGroup);
        return this;
    }

    @Override
    public PayaraMicro setInstanceName(String instanceName) {
        wrappee.setInstanceName(instanceName);
        return this;
    }

    @Override
    public PayaraMicro setLite(boolean liteMember) {
        wrappee.setLite(liteMember);
        return this;
    }

    @Override
    public PayaraMicro setLogPropertiesFile(File fileName) {
        wrappee.setLogPropertiesFile(fileName);
        return this;
    }

    @Override
    public PayaraMicro setLogoFile(String filePath) {
        wrappee.setLogoFile(filePath);
        return this;
    }

    @Override
    public PayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        wrappee.setMaxHttpThreads(maxHttpThreads);
        return this;
    }

    @Override
    public PayaraMicro setMinHttpThreads(int minHttpThreads) {
        wrappee.setMinHttpThreads(minHttpThreads);
        return this;
    }

    @Override
    public PayaraMicro setNoCluster(boolean noCluster) {
        wrappee.setNoCluster(noCluster);
        return this;        
    }

    @Override
    public PayaraMicro setPrintLogo(boolean generate) {
        wrappee.setPrintLogo(generate);
        return this;
    }

    @Override
    public PayaraMicro setRootDir(File rootDir) {
        wrappee.setRootDir(rootDir);
        return this;
    }

    @Override
    public PayaraMicro setSslAutoBind(boolean sslAutoBind) {
        wrappee.setSslAutoBind(sslAutoBind);
        return this;
    }

    @Override
    public PayaraMicro setSslPort(int sslPort) {
        wrappee.setSslPort(sslPort);
        return this;
    }
    
    @Override
    public PayaraMicro setSslCert(String alias) {
        wrappee.setSslCert(alias);
        return this;
    }

    @Override
    public PayaraMicro setUserLogFile(String fileName) {
        wrappee.setUserLogFile(fileName);
        return this;
    }
    
    @Override
    public PayaraMicro setSniEnabled(boolean value) {
        wrappee.setSniEnabled(value);
        return this;
    }

    @Override
    public void shutdown() throws BootstrapException {
        wrappee.shutdown();
    }
    
    public ClassLoader setThreadBootstrapLoader() {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(nestedLoader);
        return result;
    }
    
    public static void main(String ... args) {
        try {
            PayaraMicroLauncher.main(args);
        } catch (Exception ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private PayaraMicro() {
        try {
            if (explodedJars) {
                if (explodedDir != null) {
                    nestedLoader = new ExplodedURLClassloader(explodedDir);
                } else {
                    nestedLoader = new ExplodedURLClassloader();
                }
                setThreadBootstrapLoader();
                Class<?> mainClass = Thread.currentThread().getContextClassLoader()
                        .loadClass("fish.payara.micro.impl.PayaraMicroImpl");
                Method instanceMethod = mainClass.getDeclaredMethod("getInstance");
                wrappee = (PayaraMicroBoot) instanceMethod.invoke(null); 
            } else {
                wrappee = PayaraMicroLauncher.getBootClass();
                nestedLoader = wrappee.getClass().getClassLoader();
            }
        } catch (Exception ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, "Unable to create implementation class", ex);
        }
    }
    
    /**
     * Adds the library to the classloader and loads it
     * 
     * @param lib The URL or filepath of the library to add
     * @return 
     * @since 4.1.2.173
     */
    public PayaraMicro addLibrary(File lib){  
        wrappee.addLibrary(lib);
        return this;
    }
    
}
