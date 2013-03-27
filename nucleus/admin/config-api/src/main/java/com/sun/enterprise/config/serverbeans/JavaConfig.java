/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.*;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;

/**
 * Java Runtime environment configuration
 */

/* @XmlType(name = "", propOrder = {
    "profiler",
    "jvmOptionsOrProperty"
}) */

@Configured
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-profiler"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-profiler")
})
public interface JavaConfig extends ConfigBeanProxy, PropertyBag, JvmOptionBag {

    /**
     * Gets the value of the javaHome property.
     *
     * Specifies the installation directory for Java runtime. 
     * JDK 1.4 or higher is supported.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="${com.sun.aas.javaRoot}")
    String getJavaHome();

    /**
     * Sets the value of the javaHome property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setJavaHome(String value) throws PropertyVetoException;

    /**
     * Gets the value of the debugEnabled property.
     *
     * If set to true, the server starts up in debug mode ready for attaching
     * with a JPDA based debugger
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getDebugEnabled();

    /**
     * Sets the value of the debugEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDebugEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the debugOptions property.
     *
     * JPDA based debugging options string
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n")
    String getDebugOptions();

    /**
     * Sets the value of the debugOptions property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDebugOptions(String value) throws PropertyVetoException;

    /**
     * Gets the value of the rmicOptions property.
     *
     * Options string passed to RMI compiler, at application deployment time.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="-iiop -poa -alwaysgenerate -keepgenerated -g")
    String getRmicOptions();

    /**
     * Sets the value of the rmicOptions property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setRmicOptions(String value) throws PropertyVetoException;

    /**
     * Gets the value of the javacOptions property.
     *
     * Options string passed to Java compiler, at application deployment time.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="-g")
    String getJavacOptions();

    /**
     * Sets the value of the javacOptions property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setJavacOptions(String value) throws PropertyVetoException;

    /**
     * Gets the value of the classpathPrefix property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getClasspathPrefix();

    /**
     * Sets the value of the classpathPrefix property.
     *
     * A java classpath string that is prefixed to server-classpath
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setClasspathPrefix(String value) throws PropertyVetoException;

    /**
     * Gets the value of the classpathSuffix property.
     *
     * A java classpath string that is appended to server-classpath
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getClasspathSuffix();

    /**
     * Sets the value of the classpathSuffix property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setClasspathSuffix(String value) throws PropertyVetoException;

    /**
     * Gets the value of the serverClasspath property.
     *
     * A java classpath string that specifies the classes needed by the
     * Application server. Do not expect users to change this under normal
     * conditions. The shared application server classloader forms the final
     * classpath by concatenating classpath-prefix, ${INSTALL_DIR}/lib,
     * server-classpath, and classpath-suffix
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getServerClasspath();

    /**
     * Sets the value of the serverClasspath property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setServerClasspath(String value) throws PropertyVetoException;

    /**
     * Gets the value of the systemClasspath property.
     *
     * This classpath string supplied to the jvm at server startup.
     * Contains appserv-launch.jar by default. Users may add to this classpath.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSystemClasspath();

    /**
     * Sets the value of the systemClasspath property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSystemClasspath(String value) throws PropertyVetoException;

    /**
     * Gets the value of the nativeLibraryPathPrefix property.
     *
     * Prepended to the native library path, which is constructed internally
     *
     * Internally, the native library path is automatically constructed to be a
     * concatenation of Application Server installation relative path for its
     * native shared libraries, standard JRE native library path, the shell
     * environment setting (LD-LIBRARY-PATH on Unix) and any path that may be
     * specified in the profile element.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getNativeLibraryPathPrefix();

    /**
     * Sets the value of the nativeLibraryPathPrefix property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setNativeLibraryPathPrefix(String value) throws PropertyVetoException;

    /**
     * Gets the value of the nativeLibraryPathSuffix property.
     *
     * Appended to the native library path, which is constructed
     * as described above
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getNativeLibraryPathSuffix();

    /**
     * Sets the value of the nativeLibraryPathSuffix property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setNativeLibraryPathSuffix(String value) throws PropertyVetoException;

    /**
     * Gets the value of the bytecodePreprocessors property.
     *
     * A comma separated list of classnames, each of which must implement the
     * com.sun.appserv.BytecodePreprocessor interface. Each of the specified
     * preprocessor class will be called in the order specified. At the moment
     * the comelling use is for a 3rd party Performance Profiling tool.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getBytecodePreprocessors();

    /**
     * Sets the value of the bytecodePreprocessors property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setBytecodePreprocessors(String value) throws PropertyVetoException;

    /**
     * Gets the value of the envClasspathIgnored property.
     *
     * If set to false, the CLASSPATH environment variable will be read and
     * appended to the Application Server classpath, which is constructed as
     * described above. The CLASSPATH environment variable will be added after
     * the classpath-suffix, at the very end
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getEnvClasspathIgnored();

    /**
     * Sets the value of the envClasspathIgnored property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnvClasspathIgnored(String value) throws PropertyVetoException;

    /**
     * Gets the value of the profiler property.
     *
     * @return possible object is
     *         {@link Profiler }
     */
    @Element
    Profiler getProfiler();

    /**
     * Sets the value of the profiler property.
     *
     * @param value allowed object is
     *              {@link Profiler }
     */
    void setProfiler(Profiler value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

     /**
     * Returns the javac options for deployment. The options can be anything
     * except "-d",  "-classpath" and "-cp".
     * It tokenizes the options by blank space between them. It does
     * not to detect options like "-g -g -g" since javac handles it.
     *
     * @return javac options as of a list of java.lang.String
     */
    @DuckTyped
    List<String> getJavacOptionsAsList();

    class Duck {
        public static List<String> getJavacOptionsAsList(JavaConfig me) {

            List<String> javacOptions = new ArrayList<String>();

            String options = me.getJavacOptions();

            StringTokenizer st = new StringTokenizer(options, " ");
            while (st.hasMoreTokens()) {
                String op = st.nextToken();
                if ( !(op.startsWith("-d")
                   || op.startsWith("-cp") || op.startsWith("-classpath")) ) {
                    javacOptions.add(op);
                }
            }

            return Collections.unmodifiableList(javacOptions);
        }
    }

}
