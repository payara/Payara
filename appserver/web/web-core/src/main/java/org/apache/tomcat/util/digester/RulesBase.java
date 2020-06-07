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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.tomcat.util.digester;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * <p>Default implementation of the <code>Rules</code> interface that supports
 * the standard rule matching behavior.  This class can also be used as a
 * base class for specialized <code>Rules</code> implementations.</p>
 *
 * <p>The matching policies implemented by this class support two different
 * types of pattern matching rules:</p>
 * <ul>
 * <li><em>Exact Match</em> - A pattern "a/b/c" exactly matches a
 *     <code>&lt;c&gt;</code> element, nested inside a <code>&lt;b&gt;</code>
 *     element, which is nested inside an <code>&lt;a&gt;</code> element.</li>
 * <li><em>Tail Match</em> - A pattern "&#42;/a/b" matches a
 *     <code>&lt;b&gt;</code> element, nested inside an <code>&lt;a&gt;</code>
 *      element, no matter how deeply the pair is nested.</li>
 * </ul>
 */

public class RulesBase implements Rules {


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of registered Rule instances, keyed by the matching pattern.
     * Each value is a List containing the Rules for that pattern, in the
     * order that they were orginally registered.
     */
    protected HashMap<String, List<Rule>> cache =
        new HashMap<String, List<Rule>>();


    /**
     * The Digester instance with which this Rules instance is associated.
     */
    protected Digester digester = null;


    /**
     * The namespace URI for which subsequently added <code>Rule</code>
     * objects are relevant, or <code>null</code> for matching independent
     * of namespaces.
     */
    protected String namespaceURI = null;


    /**
     * The set of registered Rule instances, in the order that they were
     * originally registered.
     */
    protected ArrayList<Rule> rules = new ArrayList<Rule>();


    // ------------------------------------------------------------- Properties


    /**
     * Return the Digester instance with which this Rules instance is
     * associated.
     */
    @Override
    public Digester getDigester() {
        return (this.digester);
    }


    /**
     * Set the Digester instance with which this Rules instance is associated.
     *
     * @param digester The newly associated Digester instance
     */
    @Override
    public void setDigester(Digester digester) {

        this.digester = digester;
        Iterator<Rule> items = rules.iterator();
        while (items.hasNext()) {
            Rule item = items.next();
            item.setDigester(digester);
        }

    }


    /**
     * Return the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     */
    @Override
    public String getNamespaceURI() {
        return (this.namespaceURI);
    }


    /**
     * Set the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     *
     * @param namespaceURI Namespace URI that must match on all
     *  subsequently added rules, or <code>null</code> for matching
     *  regardless of the current namespace URI
     */
    @Override
    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Register a new Rule instance matching the specified pattern.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule Rule instance to be registered
     */
    @Override
    public void add(String pattern, Rule rule) {
        // to help users who accidently add '/' to the end of their patterns
        int patternLength = pattern.length();
        if (patternLength>1 && pattern.endsWith("/")) {
            pattern = pattern.substring(0, patternLength-1);
        }
        
        
        List<Rule> list = cache.get(pattern);
        if (list == null) {
            list = new ArrayList<Rule>();
            cache.put(pattern, list);
        }
        list.add(rule);
        rules.add(rule);
        if (this.digester != null) {
            rule.setDigester(this.digester);
        }
        if (this.namespaceURI != null) {
            rule.setNamespaceURI(this.namespaceURI);
        }

    }


    /**
     * Clear all existing Rule instance registrations.
     */
    @Override
    public void clear() {

        cache.clear();
        rules.clear();

    }


    /**
     * Return a List of all registered Rule instances that match the specified
     * nesting pattern, or a zero-length List if there are no matches.  If more
     * than one Rule instance matches, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     *
     * @param namespaceURI Namespace URI for which to select matching rules,
     *  or <code>null</code> to match regardless of namespace URI
     * @param pattern Nesting pattern to be matched
     */
    @Override
    public List<Rule> match(String namespaceURI, String pattern) {

        // List rulesList = (List) this.cache.get(pattern);
        List<Rule> rulesList = lookup(namespaceURI, pattern);
        if ((rulesList == null) || (rulesList.size() < 1)) {
            // Find the longest key, ie more discriminant
            String longKey = "";
            Iterator<String> keys = this.cache.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith("*/")) {
                    if (pattern.equals(key.substring(2)) ||
                        pattern.endsWith(key.substring(1))) {
                        if (key.length() > longKey.length()) {
                            // rulesList = (List) this.cache.get(key);
                            rulesList = lookup(namespaceURI, key);
                            longKey = key;
                        }
                    }
                }
            }
        }
        if (rulesList == null) {
            rulesList = new ArrayList<Rule>();
        }
        return (rulesList);

    }


    /**
     * Return a List of all registered Rule instances, or a zero-length List
     * if there are no registered Rule instances.  If more than one Rule
     * instance has been registered, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     */
    @Override
    public List<Rule> rules() {
        return (this.rules);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a List of Rule instances for the specified pattern that also
     * match the specified namespace URI (if any).  If there are no such
     * rules, return <code>null</code>.
     *
     * @param namespaceURI Namespace URI to match, or <code>null</code> to
     *  select matching rules regardless of namespace URI
     * @param pattern Pattern to be matched
     */
    protected List<Rule> lookup(String namespaceURI, String pattern) {

        // Optimize when no namespace URI is specified
        List<Rule> list = this.cache.get(pattern);
        if (list == null) {
            return (null);
        }
        if ((namespaceURI == null) || (namespaceURI.length() == 0)) {
            return (list);
        }

        // Select only Rules that match on the specified namespace URI
        ArrayList<Rule> results = new ArrayList<Rule>();
        Iterator<Rule> items = list.iterator();
        while (items.hasNext()) {
            Rule item = items.next();
            if ((namespaceURI.equals(item.getNamespaceURI())) ||
                    (item.getNamespaceURI() == null)) {
                results.add(item);
            }
        }
        return (results);

    }


}
