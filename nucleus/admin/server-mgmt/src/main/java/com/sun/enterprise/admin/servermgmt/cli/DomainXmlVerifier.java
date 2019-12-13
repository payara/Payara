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
 *
 * Portions Copyright [2018-2019] Payara Foundation and/or affiliates
 */
package com.sun.enterprise.admin.servermgmt.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

// config imports
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.Result;

import org.glassfish.hk2.api.MultiException;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

/**
 * Does basic level verification of domain.xml. This is helpful as there is no
 * DTD to validate the domain's config i.e. domain.xml
 * 
 * @author Nandini Ektare
 */
public class DomainXmlVerifier {

    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(DomainXmlVerifier.class);

    private final PrintStream out;
    private final PrintStream err;

    private final ConfigParser parser;
    private Domain domain;
    private boolean error;

    public DomainXmlVerifier(URL domainXml, PrintStream out, PrintStream err) throws Exception {
        this(out, err);
        DomDocument<?> doc = parser.parse(domainXml);
        this.domain = createDomainDom(doc);
    }

    public DomainXmlVerifier(InputStream domainXml, PrintStream out, PrintStream err) throws Exception {
        this(out, err);
        XMLStreamReader domainXmlReader = XMLInputFactory.newInstance().createXMLStreamReader(domainXml);
        DomDocument<?> doc = parser.parse(domainXmlReader);
        this.domain = createDomainDom(doc);
    }

    private DomainXmlVerifier(PrintStream out, PrintStream err) throws MalformedURLException {
        this.out = out;
        this.err = err;
        this.parser = new ConfigParser(Globals.getStaticHabitatWithModules(), false);
    }

    /*
     * Returns true if there is an error in the domain.xml or some other problem.
     */
    public boolean invokeConfigValidator() {
        boolean failed = false;
        try {
            validate();
        } catch (Exception e) {
            failed = true;
            if (err != null) {
                e.printStackTrace(err);
            }
        }
        return failed;
    }

    public void validate() {
        checkUnique(Dom.unwrap(domain));
        checkParseErrors();
        if (out != null && !error) {
            out.println(STRINGS.get("VerifySuccess"));
        }
    }

    private void checkParseErrors() {
        if (parser != null) {
            List<String> errors = parser.getErrors();
            if (!errors.isEmpty()) {
                List<Throwable> exceptions = new ArrayList<>();
                for (String error : errors) {
                    exceptions.add(new Exception(error));
                }
                throw new MultiException(exceptions);
            }
        }
    }

    private void checkUnique(Dom d) {

        try {
            Set<String> eltnames = d.getElementNames();
            Set<String> leafeltnames = d.model.getLeafElementNames();
            for (String elt : eltnames) {
                if (leafeltnames.contains(elt))
                    continue;
                List<Dom> eltlist;
                synchronized (d) {
                    eltlist = d.nodeElements(elt);
                }
                checkDuplicate(eltlist);
                for (Dom subelt : eltlist) {
                    checkUnique(subelt);
                }
            }
        } catch (Exception e) {
            error = true;
            if (err != null) {
                e.printStackTrace(err);
            }
        }
    }

    private void output(Result result) {
        if (out != null) {
            out.println(STRINGS.get("VerifyError", result.result()));
        }
    }

    private void checkDuplicate(Collection<? extends Dom> beans) {
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

        WeakHashMap<String, Class<ConfigBeanProxy>> errorKeyBeanMap = new WeakHashMap<String, Class<ConfigBeanProxy>>();
        String[] strKeys = keys.toArray(new String[beans.size()]);
        for (int i = 0; i < strKeys.length; i++) {
            for (int j = i + 1; j < strKeys.length; j++) {
                // If the keys are same and if the indexes don't match
                // we have a duplicate. So output that error
                if ((strKeys[i].equals(strKeys[j]))) {
                    errorKeyBeanMap.put(strKeys[i], ((Dom) keyBeanMap.get(strKeys[i])).getProxyType());
                    error = true;
                    break;
                }
            }
        }

        for (Map.Entry e : errorKeyBeanMap.entrySet()) {
            Result result = new Result(STRINGS.get("VerifyDupKey", e.getKey(), e.getValue()));
            output(result);
        }
    }

    private static Domain createDomainDom(DomDocument<?> domainXmlDocument) {
        Dom domDomain = domainXmlDocument.getRoot();
        return domDomain.createProxy(Domain.class);
    }

}
