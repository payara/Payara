/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.glassfish;

import com.sun.enterprise.universal.NameValue;
import com.sun.enterprise.universal.collections.ManifestUtils;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Wraps the Manifest object returned by the Server.  The Manifest object has
 * an internally defined format over and above the Manifest itself.  This is a
 * central place where we are aware of all the details so that callers don't
 * have to be.  If the format changes or the returned Object type changes then
 * this class will be the thing to change.
 * 
 * @author bnevins
 */
public class AdminCommandResponse {
    public static final String GENERATED_HELP = "GeneratedHelp";
    public static final String MANPAGE = "MANPAGE";
    public static final String SYNOPSIS = "SYNOPSIS";
    public static final String MESSAGE = "message";
    public static final String CHILDREN_TYPE = "children-type";
    public static final String EXITCODE = "exit-code";
    public static final String SUCCESS = "Success";
    public static final String WARNING = "Warning";
    public static final String FAILURE = "Failure";
    
    public AdminCommandResponse(InputStream inStream) throws IOException {
        Manifest m = new Manifest(inStream);
        m.read(inStream);
        allRaw = ManifestUtils.normalize(m);
        mainRaw = ManifestUtils.getMain(allRaw);
        makeMain();
    }

    public boolean isGeneratedHelp() {
        return isGeneratedHelp;
    }
    
    public String getMainMessage() {
        return mainMessage;
    }
    
    public boolean wasSuccess() {
        return exitCode == 0;
    }

    public boolean wasWarning() {
        return exitCode == 1;
    }
    
    public boolean wasFailure() {
        return exitCode == 2;
    }

    public String getCause() {
        return cause;
    }
    public Map<String,String> getMainAtts() {
        return mainRaw;
    }
    public List<NameValue<String,String>> getMainKeys() {
        return mainKeys;
    }
    
    public String getValue(String key) {
        for(NameValue<String,String> nv : mainKeys) {
            if(nv.getName().equals(key))
                return nv.getValue();
        }
        return null;
    }
    
    public List<NameValue<String,String>> getKeys(Map<String,String> map) {
        List<NameValue<String,String>> list = new LinkedList<NameValue<String,String>>();
        
        String keysString = map.get("keys");
        
        if(ok(keysString)) {
            String[] keys = keysString.split(";");

            for(String key : keys) {
                String name = map.get(key + "_name");
                String value = null;
                try {
                    value = map.get(key + java.net.URLDecoder.decode("_value", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    value = map.get(key + "_value");
                }

                if(!ok(name))
                    continue;
                list.add(new NameValue<String,String>(name, value));
            }
        }
        
        return list;
    }

    public Map<String,Map<String,String>> getChildren(Map<String,String> map) {
        // keep the child elements in order
        Map<String,Map<String,String>> children = new LinkedHashMap<String,Map<String,String>>();
        String kidsString = map.get("children");
        
        if(ok(kidsString)) {
            String[] kids = kidsString.split(";");

            for(String kid : kids) {
                // kid is the name of the Attributes
                Map<String,String> kidMap = allRaw.get(kid);
                if(kidMap != null)
                    children.put(kid, kidMap);
            }
        }
        if(children.isEmpty())
            return null;
        else
            return children;
    }

    private void makeMain() {
        mainMessage = mainRaw.get(MESSAGE);

        String exitCodeString = mainRaw.get(EXITCODE);
        if(SUCCESS.equalsIgnoreCase(exitCodeString))
            exitCode = 0;
        else if(WARNING.equalsIgnoreCase(exitCodeString))
            exitCode = 1;
        else
            exitCode = 2;
        cause = mainRaw.get("cause");
        makeMainKeys();
    }
    
    /**
     *  Format:
     * (1) Main Attributes usually have the bulk of the data.  Say you have 3 items
     * in there: a, b and c.  The Manifest main attributes will end up with this:
     * keys=a;b;c
     * a_name=xxx
     * a_value=xxx
     * b_name=xxx
     * b_value=xxx
     * c_name=xxx
     * c_value=xxx
     */
    private void makeMainKeys() {
        mainKeys = getKeys(mainRaw);
        
        for(NameValue<String,String> nv : mainKeys) {
            if(nv.getName().equals(GENERATED_HELP)) {
                isGeneratedHelp = Boolean.parseBoolean(nv.getValue());
                mainKeys.remove(nv);
                break;
            }
        }
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0 && !s.equals("null"); 
    }

    private Map<String, Map<String, String>> allRaw;
    private Map<String, String> mainRaw;
    private List<NameValue<String,String>> mainKeys;
    private String  mainMessage;
    private String  cause;
    private int exitCode = 0;   // 0=success, 1=failure
    private boolean isGeneratedHelp;
}
