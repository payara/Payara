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

/*
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *             :set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *             1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *             2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *             3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 */

/* 
 * @author byron.nevins@sun.com
 */

package org.glassfish.web.jsp;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.apache.jasper.JspC;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.deployment.runtime.JspConfig;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.runtime.WebProperty;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JSPCompiler {

	public static void compile(File inWebDir, File outWebDir,
                               WebBundleDescriptor wbd, ServerContext serverContext)
            throws DeploymentException {
            //to resolve ambiguity
        final String amb = null;
		compile(inWebDir, outWebDir, wbd, amb, serverContext);
	}

    public static void compile(File inWebDir, File outWebDir,
                               WebBundleDescriptor wbd, List classpathList,
                               ServerContext serverContext)
        throws DeploymentException {
        String classpath = null;
        if (classpathList != null) {
			classpath = getClasspath(classpathList);
		}
        compile(inWebDir, outWebDir, wbd, classpath, serverContext);
    }
    

	////////////////////////////////////////////////////////////////////////////
	
	public static void compile(File inWebDir, File outWebDir,
                               WebBundleDescriptor wbd, String classpath,
                               ServerContext serverContext)
            throws DeploymentException {
		JspC jspc = new JspC();

        if (classpath != null && classpath.length() >0) {
		    jspc.setClassPath(classpath);
        }
        
        // START SJSAS 6311155
        String appName = wbd.getApplication().getName();

        // so far, this is not segragated per web bundle, all web-bundles will get the
        // same sysClassPath
        String sysClassPath = ASClassLoaderUtil.getModuleClassPath(
            serverContext.getDefaultServices(), appName, null);
        jspc.setSystemClassPath(sysClassPath);
        // END SJSAS 6311155

		verify(inWebDir, outWebDir);

		configureJspc(jspc, wbd);
		jspc.setOutputDir(outWebDir.getAbsolutePath());
		jspc.setUriroot(inWebDir.getAbsolutePath());
		jspc.setCompile(true);
		logger.log(Level.INFO, START_MESSAGE);

		try {
			jspc.execute();
		} 
		catch (Exception je) {
			throw new DeploymentException("JSP Compilation Error: " + je, je);
		}
		finally {
			// bnevins 9-9-03 -- There may be no jsp files in this web-module
			// in such a case the code above will create a useless, and possibly
			// problematic empty directory.	 If the directory is empty -- delete
			// the directory.
			
			String[] files = outWebDir.list();
			

            if(files == null || files.length <= 0) {
                if (!outWebDir.delete()) {
                    logger.log(Level.FINE, CANNOT_DELETE_FILE, outWebDir);
                }
            }

            logger.log(Level.INFO, FINISH_MESSAGE);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	
	private static void verify(File inWebDir, File outWebDir) throws DeploymentException {
		// inWebDir must exist, outWebDir must either exist or be creatable
		if (!FileUtils.safeIsDirectory(inWebDir)) {
			throw new DeploymentException("inWebDir is not a directory: " + inWebDir);
		}
	 
        if (!FileUtils.safeIsDirectory(outWebDir)) {
            if (!outWebDir.mkdirs()) {
                logger.log(Level.FINE, CANNOT_DELETE_FILE, outWebDir);
            }
		
			if (!FileUtils.safeIsDirectory(outWebDir)) {
				throw new DeploymentException("outWebDir is not a directory, and it can't be created: " + outWebDir);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	
	private static String getClasspath(List paths) {
		if(paths == null)
			return null;
		
		String classpath = null;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		for (Iterator it = paths.iterator(); it.hasNext(); ) {
			String path = (String)it.next();

			if (first) 
				first = false;
			else 
				sb.append(File.pathSeparatorChar);

			sb.append(path);
		}

		if (sb.length() > 0) 
			classpath = sb.toString();

		return classpath;	
	}	

	////////////////////////////////////////////////////////////////////////////

    /*
     * Configures the given JspC instance with the jsp-config properties
	 * specified in the sun-web.xml of the web module represented by the
	 * given WebBundleDescriptor.
	 *
	 * @param jspc JspC instance to be configured
	 * @param wbd WebBundleDescriptor of the web module whose sun-web.xml
     * is used to configure the given JspC instance
	 */
        private static void configureJspc(JspC jspc, WebBundleDescriptor wbd) {

	        SunWebAppImpl sunWebApp = (SunWebAppImpl) wbd.getSunDescriptor();
	        if (sunWebApp == null) {
         	    return;
            }

            // START SJSAS 6384538
            if (sunWebApp.sizeWebProperty() > 0) {
                WebProperty[] props = sunWebApp.getWebProperty();
                for (int i = 0; i < props.length; i++) {
                    String pName = props[i].getAttributeValue("name");
                    String pValue = props[i].getAttributeValue("value");
                    if (pName == null || pValue == null) {
                        throw new IllegalArgumentException(
                            "Missing sun-web-app property name or value");
                    }
                    if ("enableTldValidation".equals(pName)) {
                        jspc.setIsValidationEnabled(
                            Boolean.valueOf(pValue).booleanValue());
                    }
                }
            }
            // END SJSAS 6384538

            // START SJSAS 6170435
            /*
             * Configure JspC with the init params of the JspServlet
             */
            Set<WebComponentDescriptor> set = wbd.getWebComponentDescriptors();
            if (!set.isEmpty()) {
                Iterator<WebComponentDescriptor> iterator = set.iterator();
                while (iterator.hasNext()) {
                    WebComponentDescriptor webComponentDesc = iterator.next();
                    if ("jsp".equals(webComponentDesc.getCanonicalName())) {
                        Enumeration<InitializationParameter> en
                            = webComponentDesc.getInitializationParameters();
                        if (en != null) {
                            while (en.hasMoreElements()) {
                                InitializationParameter initP = en.nextElement();
                                configureJspc(jspc,
                                              initP.getName(),
                                              initP.getValue());
                            }
                        }
                        break;
                    }
                }
            }
            // END SJSAS 6170435

            /*
             * Configure JspC with jsp-config properties from sun-web.xml,
             * which override JspServlet init params of the same name.
             */
            JspConfig jspConfig = sunWebApp.getJspConfig();
            if (jspConfig == null) {
                return;
            }
            WebProperty[] props = jspConfig.getWebProperty();
            for (int i=0; props!=null && i<props.length; i++) {
                configureJspc(jspc,
                              props[i].getAttributeValue("name"),
                              props[i].getAttributeValue("value"));
            }
        }


        /*
         * Configures the given JspC instance with the given property name
         * and value.
         *
         * @jspc The JspC instance to configure
         * @pName The property name
         * @pValue The property value
         */
        private static void configureJspc(JspC jspc, String pName,
                                          String pValue) {

            if (pName == null || pValue == null) {
                throw new IllegalArgumentException(
                    "Null property name or value");
            }

            if ("xpoweredBy".equals(pName)) {
                jspc.setXpoweredBy(Boolean.valueOf(pValue).booleanValue());
            } else if ("classdebuginfo".equals(pName)) {
                jspc.setClassDebugInfo(Boolean.valueOf(pValue).booleanValue());
            } else if ("enablePooling".equals(pName)) {
                jspc.setPoolingEnabled(Boolean.valueOf(pValue).booleanValue());
            } else if ("ieClassId".equals(pName)) {
                jspc.setIeClassId(pValue);
            } else if ("trimSpaces".equals(pName)) {
                jspc.setTrimSpaces(Boolean.valueOf(pValue).booleanValue());
            } else if ("genStrAsCharArray".equals(pName)) {
                jspc.setGenStringAsCharArray(
                    Boolean.valueOf(pValue).booleanValue());
            } else if ("errorOnUseBeanInvalidClassAttribute".equals(pName)) {
                jspc.setErrorOnUseBeanInvalidClassAttribute(
                    Boolean.valueOf(pValue).booleanValue());
            } else if ("ignoreJspFragmentErrors".equals(pName)) {
                jspc.setIgnoreJspFragmentErrors(
                    Boolean.valueOf(pValue).booleanValue());
            }
        }


	////////////////////////////////////////////////////////////////////////////

    private static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "Beginning JSP Precompile...",
            level = "INFO")
    public static final String START_MESSAGE = "AS-WEB-GLUE-00281";

    @LogMessageInfo(
            message = "Finished JSP Precompile...",
            level = "INFO")
    public static final String FINISH_MESSAGE = "AS-WEB-GLUE-00282";

    @LogMessageInfo(
            message = "Cannot delete file: {0}",
            level = "FINE")
    public static final String CANNOT_DELETE_FILE = "AS-WEB-GLUE-00283";

	////////////////////////////////////////////////////////////////////////////

}
