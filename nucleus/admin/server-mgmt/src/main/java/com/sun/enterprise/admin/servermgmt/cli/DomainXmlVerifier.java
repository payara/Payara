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

package com.sun.enterprise.admin.servermgmt.cli;

import java.util.ArrayList;
import java.util.WeakHashMap;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;

// config imports
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.Result;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does basic level verification of domain.xml. This is helpful as there
 * is no DTD to validate the domain's config i.e. domain.xml
 * 
 * @author Nandini Ektare
 */
public class DomainXmlVerifier {
    
    private Domain domain;
    public boolean error;
    PrintStream _out;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(DomainXmlVerifier.class);

    public DomainXmlVerifier(Domain domain) throws Exception {
        this(domain, System.out);
    }
    
    public DomainXmlVerifier(Domain domain, PrintStream out) throws Exception {
        this.domain = domain;
        _out = out;
        error = false;
    }

    /*
     * Returns true if there is an error in the domain.xml or some other problem.
     */
    public boolean invokeConfigValidator() {
        boolean failed = false;
        try {
            failed =  validate();
        } catch(Exception e) {
            failed = true;
            e.printStackTrace();
        }
        return failed;
    }

    public boolean validate() {
        try {
            checkUnique(Dom.unwrap(domain));
            if (!error)
               _out.println(strings.get("VerifySuccess"));
        } catch(Exception e) {
            error = true;
            e.printStackTrace();
        }
        return error;
    }
    
    private void checkUnique(Dom d) {

        try {
            Set<String> eltnames = d.getElementNames();
            Set<String> leafeltnames = d.model.getLeafElementNames();
            for (String elt : eltnames) {
                if (leafeltnames.contains(elt)) continue;
                List<Dom> eltlist = d.nodeElements(elt);
                checkDuplicate(eltlist);
                for (Dom subelt : eltlist) {
                    checkUnique(subelt);
                }
            }
         } catch(Exception e) {
            error = true;
            e.printStackTrace();
        }
    }
    
    private void output(Result result) {
        _out.println(strings.get("VerifyError", result.result()));
    }

    private void checkDuplicate(Collection <? extends Dom> beans) {
        if (beans == null || beans.size() <= 1) {
            return;
        }
        WeakHashMap keyBeanMap = new WeakHashMap();
        ArrayList<String> keys = new ArrayList<String>(beans.size());
        for (Dom b : beans) {
            String key = b.getKey();
            keyBeanMap.put(key, b);
            keys.add(key);
        }

        WeakHashMap<String, Class<ConfigBeanProxy>> errorKeyBeanMap = 
                new WeakHashMap<String, Class<ConfigBeanProxy>>();
        String[] strKeys = keys.toArray(new String[beans.size()]);
        for (int i = 0; i < strKeys.length; i++) {
            boolean foundDuplicate = false;
            for (int j = i + 1; j < strKeys.length; j++) {
                // If the keys are same and if the indexes don't match
                // we have a duplicate. So output that error
                if ( (strKeys[i].equals(strKeys[j]))) {
                    foundDuplicate = true;
                    errorKeyBeanMap.put(strKeys[i],
                        ((Dom)keyBeanMap.get(strKeys[i])).getProxyType());
                    error = true;
                    break;
                }
            }
        }

        for (Map.Entry e : errorKeyBeanMap.entrySet()) {
            Result result = new Result(strings.get("VerifyDupKey", e.getKey(), e.getValue()));
            output(result);
        }
    }    
}
