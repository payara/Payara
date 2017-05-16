/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.boot;

import fish.payara.micro.boot.loader.ExecutableArchiveLauncher;
import fish.payara.micro.boot.loader.archive.Archive;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * This class boots a Payara Micro Executable jar It establishes the Payara
 * Micro Executable ClassLoader onto the main thread and then boots standard
 * Payara Micro.
 *
 * @author steve
 */
public class PayaraMicroLauncher extends ExecutableArchiveLauncher {

    private static final String JAR_MODULES_DIR = "MICRO-INF/runtime";
    private static final String JAR_CLASSES_DIR = "MICRO-INF/classes";
    private static final String JAR_LIB_DIR = "MICRO-INF/lib";
    private static final String MICRO_JAR_PROPERTY = "fish.payara.micro.BootJar";
    private static PayaraMicroBoot bootInstance;
    private static boolean mainBoot = false;

    /**
     * Boot method via java -jar
     * @param args
     * @throws Exception 
     */
    public static void main(String args[]) throws Exception {
        PayaraMicroLauncher launcher = new PayaraMicroLauncher();
        // set system property for our jar file
        ProtectionDomain protectionDomain = PayaraMicroLauncher.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
        System.setProperty(MICRO_JAR_PROPERTY, location.toString());
        mainBoot = true;
        launcher.launch(args);
    }
    
    /**
     * Boot method via Micro.getInstance()
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws Exception 
     */
    public static PayaraMicroBoot getBootClass() throws InstantiationException, IllegalAccessException, ClassNotFoundException, Exception {
        
        if (bootInstance == null) {
            
            if (mainBoot) {
                Class<?> mainClass = Thread.currentThread().getContextClassLoader()
                                        .loadClass("fish.payara.micro.impl.PayaraMicroImpl");
                Method instanceMethod = mainClass.getDeclaredMethod("getInstance");
                bootInstance = (PayaraMicroBoot) instanceMethod.invoke(null);                
            } else {
                PayaraMicroLauncher launcher = new PayaraMicroLauncher();

                // set system property for our jar file
                ProtectionDomain protectionDomain = PayaraMicroLauncher.class.getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
                System.setProperty(MICRO_JAR_PROPERTY, location.toString());

                ClassLoader loader = launcher.createClassLoader(launcher.getClassPathArchives());
                fish.payara.micro.boot.loader.jar.JarFile.registerUrlProtocolHandler();
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> mainClass = Thread.currentThread().getContextClassLoader()
                                        .loadClass("fish.payara.micro.impl.PayaraMicroImpl");
                Method instanceMethod = mainClass.getDeclaredMethod("getInstance");
                bootInstance = (PayaraMicroBoot) instanceMethod.invoke(null);
            }
        }
	return bootInstance; 
    }

    @Override
    protected boolean isNestedArchive(Archive.Entry entry) {
        boolean result = false;
        if (entry.isDirectory() && entry.getName().equals(JAR_CLASSES_DIR)) {
            result = true;
        } else if ((entry.getName().startsWith(JAR_LIB_DIR) || entry.getName().startsWith(JAR_MODULES_DIR)) && !entry.getName().endsWith(".gitkeep")) {
            result = true;
        }
        return result;
    }

    @Override
    protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
        archives.add(0, getArchive());
    }

}
