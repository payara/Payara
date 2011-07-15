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

package com.sun.jaspic.config.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;

/*
 * Each entry is either a constructor entry or a registration entry.
 * Use nulls rather than empty Strings or Lists for fields that
 * have no value.
 *
 * This class will not be used outside of its package.
 *
 * @author Bobby Bissett
 */
public final class EntryInfo {
    private final String className;
    private final Map<String, String> properties;
    private List<RegistrationContext> regContexts;

    /*
     * This will create a constructor entry. The className
     * must not be null.
     * 
     * ONLY CONSTRUCTOR that should be used used to construct defaultEntries (passed
     * RegStoreFileParser construction). DO NOT USE OTHER CONSTRUCTORS to define
     * defaultEntries because they can create persisted registration entries which
     * are not appropriate as defaultEntries.
     */
    public EntryInfo(String className, Map<String, String> properties) {
        if (className == null) {
            throw new IllegalArgumentException(
                "Class name for registration entry cannot be null");
        }
        this.className = className;
        this.properties = properties;
    }

    /*
     * This will create a registration entry. The list of
     * registration contexts must not be null or empty. Each registration
     * context will contain at least a non-null layer or appContextId.
     */
    EntryInfo(String className, Map<String, String> properties,
        List<RegistrationContext> ctxs) {

        if (ctxs == null || ctxs.isEmpty()) {
            throw new IllegalArgumentException(
                "Registration entry must contain one or" +
                "more registration contexts");
        }
        this.className = className;
        this.properties = properties;
        this.regContexts = ctxs;
    }

    /*
     * THIS METHOD MAY BE USED FOR CONSTRUCTOR OR REGISTRATION ENTRIES
     * A helper method for creating a registration entry with
     * one registration context. If the context is null, this
     * entry is a constructor entry.
     */
    EntryInfo(String className, Map<String, String> properties,
        RegistrationContext ctx) {

        this.className = className;
        this.properties = properties;
        if (ctx != null) {
            RegistrationContext ctxImpl =
                new RegistrationContextImpl(ctx.getMessageLayer(),
                ctx.getAppContext(), ctx.getDescription(), ctx.isPersistent());
            List<RegistrationContext> newList =
                new ArrayList<RegistrationContext>(1);
            newList.add(ctxImpl);
            this.regContexts = newList;
        }
    }

    EntryInfo(EntryInfo parent) {
        this.className = parent.className;
        this.properties = parent.properties;
        if (parent.regContexts != null) {
            this.regContexts = new ArrayList<RegistrationContext>(1);
            for (RegistrationContext rc : parent.regContexts) {
                this.regContexts.add(rc);
            }
        }
    }

    boolean isConstructorEntry() {
        return (regContexts == null);
    }

    String getClassName() {
        return className;
    }

    Map<String, String> getProperties() {
        return properties;
    }

    List<RegistrationContext> getRegContexts() {
        return regContexts;
    }

    /*
     * Compares an entry info to this one. They are
     * considered to match if:
     * - they are both constructor or are both registration entries
     * - the classnames are equal or are both null
     * - the property maps are equal or are both null
     *
     * If the entry is a registration entry, registration
     * contexts are not considered for our purposes. For
     * instance, we may want to get a certain registration
     * entry in order to add a registration context to it.
     *
     * @see com.sun.enterprise.security.jmac.config.RegStoreFileParser
     */
    boolean matchConstructors(EntryInfo target) {
        if (target == null) {
            return false;
        }
        return ( !(isConstructorEntry() ^ target.isConstructorEntry()) &&
            matchStrings(className, target.getClassName()) &&
            matchMaps(properties, target.getProperties()) );
    }

    /*
     * Utility method for comparing strings such that
     * two null strings are considered "equal."
     */
    static boolean matchStrings(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }

    /*
     * Utility method for comparing maps such that
     * two null maps are considered "equal."
     */
    static boolean matchMaps(Map m1, Map m2) {
        if (m1 == null && m2 == null) {
            return true;
        }
        if (m1 == null || m2 == null) {
            return false;
        }
        return m1.equals(m2);
    }


}
