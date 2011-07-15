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

package com.sun.enterprise.deployment.util.webservice;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.HashMap;

/**
 * This interface is used by the deploytool to generate webservice artifacts. 
 * A client is expected to set the options and features using the add* methods before calling the generate* method
 *
 * Here is a code sample
 *
 *   SEIConfig cfg = new SEIConfig("WeatherWebService", "WeatherWebService", "endpoint", 
 *                                    "endpoint.WeatherService", "endpoint.WeatherServiceImpl");
 *   WsCompileInvoker inv = WsCompileInvoker.getWsCompileInvoker(System.out);
 *   inv.addWsCompileOption(inv.TARGET_DIR, "/home/user/codesamples/weatherinfo/test");
 *   inv.addWsCompileOption(inv.MAP_FILE, "/home/user/codesamples/weatherinfo/test/map109.xml");
 *   inv.addWsCompileOption(inv.CLASS_PATH, "/home/user/codesamples/weatherinfo/service/build/classes");
 *   inv.addWsCompileFeature("wsi");
 *   inv.addWsCompileFeature("strict");
 *   inv.generateWSDL(cfg);
 *
 * If the client uses the same instance of WsCompileInvoker for multiple invocations of wscompile, then it is 
 * the client's responsibility to empty the options (using the clearWsCompileOptionsAndFeatures() method) that 
 * are present in this Map before using this Map for the next wscompile invocation
 */

@Contract
public abstract class WsCompileInvoker {

/**
 * This Map holds all the options to be used while invoking the wscompile tool; the options are set by the client
 * of this interface using the setter methods available in this interface. 
 **/
    protected HashMap wsCompileOptions = null;

/**
 * This specifies the classpath to be used by the wscompile tool - ideally this should at least be set to
 * directory where the SEI package is present.
 */
    public static final String CLASS_PATH = "-classpath";

/**
 * This specifies the target directory to be used by the wscompile tool to create the service artifacts - if
 * this is not specified, then the current directory will be used by the wscompile tool
 */
    public static final String TARGET_DIR = "-d";

/**
 * This specifies the file name to be used by the wscompile tool for creating the 109 mapping file. 
 */
    public static final String MAP_FILE = "-mapping";
    
/**
 * This is used to generate WSDL and mapping files given information on SEI config; the caller sends in all the
 * required info in SEIConfig (info like name of webservice, interface name, package name etc) and this method
 * creates the equivalent jaxrpc-config.xml, invokes wscompile with -define option which will generate the WSDL
 * and the mapping file.
 *
 * @param SEIConfig containing webservice name, package name, namespace, SEI and its implementation
*/
    public abstract void generateWSDL(SEIConfig config) throws WsCompileInvokerException, IOException;

/**
 * This is used to generate SEI and mapping files given information on WSDL file location, require package name.
 * The caller sends the required info on WSDL location, package name etc in WSDLConfig argument and this method
 * creates the equivalent jaxrpc-config,xml, invokes wscompile with -import option which will generate the SEI 
 * and a template implementation of the SEI.
 * @param WSDLConfig containing webservice name, package name, and WSDL location
*/

    public abstract void generateSEI(WSDLConfig config) throws WsCompileInvokerException, IOException;

/**
 * This is used to generate the non-portable client side artifacts given information on WSDL file location and the
 * package name; The caller sends the required info on WSDL location, package name etc in WSDLConfig argument and
 * this method creates the equivalent jaxrpc-config.xml, invokes wscompile with -gen:client option which will
 * generate all required non-portable client-side artifacts like JAXRPC stubs etc.
 * @param WSDLConfig containing webservice name, package name, and WSDL location
 */
    public abstract void generateClientStubs(WSDLConfig config) throws WsCompileInvokerException, IOException;
    
/**
 * This is used to set an option to be used while invoking the wscompile tool; for example to use the -classpath 
 * option for wscompile, the call will be setWsCompileOption(WsCompileInvoker.CLASS_PATH, "the_path"); 
 * For using wscompile options that are not defined in WsCompileInvoker, the 'option' argument of this call will be
 * the actual argument to be used ; for example, for using the '-s' option of wscompile, the call
 * to set the option will be setWsCompileOption("-s", "the_path_to_be_used");
 * For options that dont have an operand, the second argument can be null; for example, to invoke the wscompile tool
 * with verbose option, the -verbose option will be set with this call setWsCompileOption("-verbose", null);
 * @param String option  the wscompile option to be used
 * @param String operand the operand for the option; null if none;
 */
    public void addWsCompileOption(String option, String operand) {
        if(wsCompileOptions == null)
            wsCompileOptions = new HashMap();
        wsCompileOptions.put(option, operand);
    }

/**
 * This is used to remove an option that was already set
 * @param String The option that has to be removed
 * @returns true If the option was found and removed
 * @returns false If the option was not found
 */
    public boolean removeWsCompileOption(String option) {
        if( (wsCompileOptions != null) && wsCompileOptions.containsKey(option) ) {
            wsCompileOptions.remove(option);
            return true;
        }
        return false;
    }
            
/**
 * This is used to set a feature to be used while invoking the wscompile tool; for example to use the -f:wsi feature,
 * the call will be addWsCompileFeature("wsi")
 * This will prefix -f: to the feature and set it as part of the options Map with operand being null
 * @param String the feature to be set
 */
    public void addWsCompileFeature(String feature) {
        addWsCompileOption("-f:"+feature, null);
    }
    
/**
 * This is used to remove a feature that was set; for example to remove "-f:strict" feature that was set before the
 * call will be removeWsCompileFeature("strict")
 * @param String the feature to be removed
 * @returns true if the feature was found and removed
 * @false false if the feature was not found
 */

    public boolean removeWsCompileFeature(String feature) {
        return(removeWsCompileOption("-f:"+feature));
    }
    
/**
 * This is used to clear all options that have been set
 */
    public void clearWsCompileOptionsAndFeatures() {
        if(wsCompileOptions != null)
            wsCompileOptions.clear();
    }
}
