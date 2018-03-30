/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import fish.payara.micro.boot.PayaraMicroBoot;
import fish.payara.micro.boot.PayaraMicroLauncher;
import fish.payara.micro.boot.loader.ExplodedURLClassloader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class PayaraMicro {
    
    
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

    public PayaraMicro addDeployFromGAV(String GAV) {
        wrappee.addDeployFromGAV(GAV);
        return this;
    }

    public PayaraMicro addDeployment(String pathToWar) {
        wrappee.addDeployment(pathToWar);
        return this;
    }

    public PayaraMicro addDeploymentFile(File file) {
        wrappee.addDeploymentFile(file);
        return this;
    }

    public PayaraMicro addRepoUrl(String... URLs) {
        wrappee.addRepoUrl(URLs);
        return this;
    }

    public PayaraMicroRuntime bootStrap() throws BootstrapException {
        if (wrappee == null) {
            throw new BootstrapException("Could not create Runtime instance");
        }
        return wrappee.bootStrap();
    }

    public File getAlternateDomainXML() {
        return wrappee.getAlternateDomainXML();
    }

    public int getAutoBindRange() {
        return wrappee.getAutoBindRange();
    }

    public String getClusterMulticastGroup() {
        return wrappee.getClusterMulticastGroup();
    }

    public int getClusterPort() {
        return wrappee.getClusterPort();
    }

    public int getClusterStartPort() {
        return wrappee.getClusterStartPort();
    }

    public File getDeploymentDir() {
        return wrappee.getDeploymentDir();
    }

    public boolean getHttpAutoBind() {
        return wrappee.getHttpAutoBind();
    }

    public int getHttpPort() {
        return wrappee.getHttpPort();
    }

    public String getHzClusterName() {
        return wrappee.getHzClusterName();
    }

    public String getHzClusterPassword() {
        return wrappee.getHzClusterPassword();
    }

    public String getInstanceName() {
        return wrappee.getInstanceName();
    }

    public int getMaxHttpThreads() {
        return wrappee.getMaxHttpThreads();
    }

    public int getMinHttpThreads() {
        return wrappee.getMinHttpThreads();
    }

    public File getRootDir() {
        return wrappee.getRootDir();
    }

    public PayaraMicroRuntime getRuntime() throws IllegalStateException {
        return wrappee.getRuntime();
    }

    public boolean getSslAutoBind() {
        return wrappee.getSslAutoBind();
    }

    public int getSslPort() {
        return wrappee.getSslPort();
    }
    
    public String getSslCert() {
        return wrappee.getSslCert();
    }

    public File getUberJar() {
        return wrappee.getUberJar();
    }

    public boolean isLite() {
        return wrappee.isLite();
    }

    public boolean isNoCluster() {
        return wrappee.isNoCluster();
    }

    public PayaraMicro setAccessLogDir(String filePath) {
        wrappee.setAccessLogDir(filePath);
        return this;
    }

    public PayaraMicro setAccessLogFormat(String format) {
        wrappee.setAccessLogFormat(format);
        return this;
    }

    public PayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        wrappee.setAlternateDomainXML(alternateDomainXML);
        return this;
    }

    public PayaraMicro setApplicationDomainXML(String domainXml) {
        wrappee.setApplicationDomainXML(domainXml);
        return this;
    }

    public PayaraMicro setAutoBindRange(int autoBindRange) {
        wrappee.setAutoBindRange(autoBindRange);
        return this;
    }

    public PayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        wrappee.setClusterMulticastGroup(hzMulticastGroup);
        return this;
    }

    public PayaraMicro setClusterPort(int hzPort) {
        wrappee.setClusterPort(hzPort);
        return this;
    }

    public PayaraMicro setClusterStartPort(int hzStartPort) {
        wrappee.setClusterStartPort(hzStartPort);
        return this;
    }

    public PayaraMicro setDeploymentDir(File deploymentRoot) {
        wrappee.setDeploymentDir(deploymentRoot);
        return this;
    }

    public PayaraMicro setHttpAutoBind(boolean httpAutoBind) {
        wrappee.setHttpAutoBind(httpAutoBind);
        return this;
    }

    public PayaraMicro setHttpPort(int httpPort) {
        wrappee.setHttpPort(httpPort);
        return this;
    }

    public PayaraMicro setHzClusterName(String hzClusterName) {
        wrappee.setHzClusterName(hzClusterName);
        return this;
    }

    public PayaraMicro setHzClusterPassword(String hzClusterPassword) {
        wrappee.setHzClusterPassword(hzClusterPassword);
        return this;
    }
    
    /**
     * Gets the name of the instance group
     *
     * @return The name of the instance group
     */
    public String getInstanceGroup() {
        return wrappee.getInstanceGroup();
    }

    /**
     * Sets the instance group name
     *
     * @param instanceGroup The instance group name
     * @return
     */
    public PayaraMicro setInstanceGroup(String instanceGroup) {
        wrappee.setInstanceGroup(instanceGroup);
        return this;
    }

    public PayaraMicro setInstanceName(String instanceName) {
        wrappee.setInstanceName(instanceName);
        return this;
    }

    public PayaraMicro setLite(boolean liteMember) {
        wrappee.setLite(liteMember);
        return this;
    }

    public PayaraMicro setLogPropertiesFile(File fileName) {
        wrappee.setLogPropertiesFile(fileName);
        return this;
    }

    public PayaraMicro setLogoFile(String filePath) {
        wrappee.setLogoFile(filePath);
        return this;
    }

    public PayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        wrappee.setMaxHttpThreads(maxHttpThreads);
        return this;
    }

    public PayaraMicro setMinHttpThreads(int minHttpThreads) {
        wrappee.setMinHttpThreads(minHttpThreads);
        return this;
    }

    public PayaraMicro setNoCluster(boolean noCluster) {
        wrappee.setNoCluster(noCluster);
        return this;        
    }

    public PayaraMicro setPrintLogo(boolean generate) {
        wrappee.setPrintLogo(generate);
        return this;
    }

    public PayaraMicro setRootDir(File rootDir) {
        wrappee.setRootDir(rootDir);
        return this;
    }

    public PayaraMicro setSslAutoBind(boolean sslAutoBind) {
        wrappee.setSslAutoBind(sslAutoBind);
        return this;
    }

    public PayaraMicro setSslPort(int sslPort) {
        wrappee.setSslPort(sslPort);
        return this;
    }
    
    public PayaraMicro setSslCert(String alias) {
        wrappee.setSslCert(alias);
        return this;
    }

    public PayaraMicro setUserLogFile(String fileName) {
        wrappee.setUserLogFile(fileName);
        return this;
    }
    
    public PayaraMicro setSniEnabled(boolean value) {
        wrappee.setSniEnabled(value);
        return this;
    }

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
