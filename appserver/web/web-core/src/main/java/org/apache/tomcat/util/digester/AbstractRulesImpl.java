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

package org.apache.tomcat.util.digester;


import java.util.List;


/**
 * <p><code>AbstractRuleImpl</code> provides basic services for <code>Rules</code> implementations.
 * Extending this class should make it easier to create a <code>Rules</code> implementation.</p>
 * 
 * <p><code>AbstractRuleImpl</code> manages the <code>Digester</code> 
 * and <code>namespaceUri</code> properties.
 * If the subclass overrides {@link #registerRule} (rather than {@link #add}),
 * then the <code>Digester</code> and <code>namespaceURI</code> of the <code>Rule</code>
 * will be set correctly before it is passed to <code>registerRule</code>.
 * The subclass can then perform whatever it needs to do to register the rule.</p>
 *
 * @since 1.5
 */

abstract public class AbstractRulesImpl implements Rules {

    // ------------------------------------------------------------- Fields
    
    /** Digester using this <code>Rules</code> implementation */
    private Digester digester;
    /** Namespace uri to assoicate with subsequent <code>Rule</code>'s */
    private String namespaceURI;
    
    // ------------------------------------------------------------- Properties

    /**
     * Return the Digester instance with which this Rules instance is
     * associated.
     */
    public Digester getDigester() {
        return digester;
    }

    /**
     * Set the Digester instance with which this Rules instance is associated.
     *
     * @param digester The newly associated Digester instance
     */
    public void setDigester(Digester digester) {
        this.digester = digester;
    }

    /**
     * Return the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Set the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     *
     * @param namespaceURI Namespace URI that must match on all
     *  subsequently added rules, or <code>null</code> for matching
     *  regardless of the current namespace URI
     */
    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Registers a new Rule instance matching the specified pattern.
     * This implementation sets the <code>Digester</code> and the
     * <code>namespaceURI</code> on the <code>Rule</code> before calling {@link #registerRule}.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule Rule instance to be registered
     */
    public void add(String pattern, Rule rule) {
        // set up rule
        if (this.digester != null) {
            rule.setDigester(this.digester);
        }
        
        if (this.namespaceURI != null) {
            rule.setNamespaceURI(this.namespaceURI);
        }
        
        registerRule(pattern, rule);
        
    }
    
    /** 
     * Register rule at given pattern.
     * The the Digester and namespaceURI properties of the given <code>Rule</code>
     * can be assumed to have been set properly before this method is called.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule Rule instance to be registered
     */ 
    abstract protected void registerRule(String pattern, Rule rule);

    /**
     * Clear all existing Rule instance registrations.
     */
    abstract public void clear();


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
    abstract public List<Rule> match(String namespaceURI, String pattern);


    /**
     * Return a List of all registered Rule instances, or a zero-length List
     * if there are no registered Rule instances.  If more than one Rule
     * instance has been registered, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     */
    abstract public List<Rule> rules();

}
