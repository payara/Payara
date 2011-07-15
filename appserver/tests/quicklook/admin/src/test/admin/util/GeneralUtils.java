/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package test.admin.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.testng.Reporter;

/** Provides several utilities. Please see v3/core/kernel/../AdminAdapter to see
 *  what it does when finally the command invocation returns.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish v3 Prelude
 */
public final class GeneralUtils {
    
    public enum AsadminManifestKeyType {
        EXIT_CODE("exit-code"),
        CHILDREN ("children"),
        MESSAGE  ("message"),
        CAUSE    ("cause");
        private final String name;
        
        AsadminManifestKeyType(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    }
    
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    
    /* These can't change. They are buried in CLI code on server side! */
    
    /** Creates the final asadmin URL with command's bells and whistles.
     * 
     * @param adminUrl
     * @param cmd
     * @param options
     * @param operand
     * @return
     */
    public static String toFinalURL(String adminUrl, String cmd, Map<String, String>options, String operand) {
        if (adminUrl == null || cmd == null)
            throw new IllegalArgumentException("null adminURL/cmd not allowed");
        StringBuffer buffer = new StringBuffer(adminUrl);
        if (!adminUrl.endsWith("/"))
            buffer.append("/");
        buffer.append(cmd);
        boolean optionsPresent = (options != null && !options.isEmpty());
        boolean operandPresent = (operand != null);
        if (optionsPresent || operandPresent)
            buffer.append("?");
        if(optionsPresent) {
            Set<String> names = options.keySet();
            for (String name : names) {
                String value = options.get(name);
                String encoded = encodePair(name, value);
                buffer.append(encoded);
                buffer.append("&");
            }
        }
        if (operandPresent) {
            buffer.append(encodePair("DEFAULT", operand));
        }
        int len = buffer.length();
        if(buffer.charAt(len-1) == '?' || buffer.charAt(len-1) == '&') { //remove last '&'/'?' if there is no operand
            buffer.delete(len-1, len);
        }
        return ( buffer.toString());
    }
    
    public static String getValueForTypeFromManifest(Manifest man, AsadminManifestKeyType key) {
        if (man == null)
            throw new IllegalArgumentException("null manifest received");
        if (key == null)
            key = AsadminManifestKeyType.EXIT_CODE;
        Attributes ma = man.getMainAttributes();
        Set<Object> names = ma.keySet();
        for (Object name : names) {
            Object value = ma.get(name);
            if(key.toString().equals(name.toString())) { //we got the key
                Reporter.log("Attribute exists, name: " + name + " value: " + value);
                return ( value.toString() );
            }
        }
        Reporter.log("Atrribute does not exist: " + key.toString() + " returning null");
        return ( null );  //given key does not exist amongst manifest attributes
    }

    public static void handleManifestFailure(Manifest man) {
        String ec = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.EXIT_CODE);
        if (ec != null && GeneralUtils.FAILURE.equalsIgnoreCase(ec.trim())) {
            //we have a failure
            String cause = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CAUSE);
            Reporter.log("Cause: " + cause);
            throw new RuntimeException("" + cause);
        }        
    }
    
    ///// private methods /////
    private static String encodePair(String name, String value) {
        try {
            String en = URLEncoder.encode(name, "UTF-8");
            String ev = URLEncoder.encode(value, "UTF-8");
            return ( new StringBuffer(en).append("=").append(ev).toString() );
        } catch(UnsupportedEncodingException ue) {
            throw new RuntimeException(ue);
        }
    }    
}
