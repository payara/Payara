/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import org.jvnet.hk2.annotations.Contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Inject configuration values to the object.
 *
 * The service name indicates the element name in XML config.
 *
 * @author Kohsuke Kawaguchi
 */
@Contract
public abstract class ConfigInjector<T> {
    /**
     * Reads values from {@link Dom} and inject them into the given target object.
     *
     * @throws ConfigurationException
     *      If the injection fails. This exception won't have its location set yet.
     *      It's the caller's job to do so.
     */
    public abstract void inject(Dom dom, T target);

    /**
     * Injects a single property of the given element name.
     */
    public abstract void injectElement(Dom dom, String elementName, T target );

    /**
     * Injects a single property of the given attribute name.
     */
    public abstract void injectAttribute(Dom dom, String attributeName, T target );

    // utility methods for derived classes
    public final int asInt(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ConfigurationException(v+" is not a number");
        }
    }

    private static final Map<String,Boolean> BOOLEAN_VALUES = new HashMap<String,Boolean>();

    static {
        for( String t : new String[]{"on","true","1","yes","enabled"})
           BOOLEAN_VALUES.put(t,true);
        for( String t : new String[]{"off","false","0","no","disabled"})
           BOOLEAN_VALUES.put(t,false);
    }

    public final boolean asBoolean(String v) {
        Boolean b = BOOLEAN_VALUES.get(v);
        if(b!=null) return b;
        throw new ConfigurationException(v+" is not a boolean");
    }

    /**
     * Resolves a reference to the given type by the given id.
     */
    public final <T> T reference(Dom dom, String id, Class<T> type) {
        // TODO: this doesn't work in case where type is a subtype of indexed type.
        String name = type.getName();
        dom = dom.getSymbolSpaceRoot(name);
        return type.cast(dom.resolveReference(id,name).get());
    }
}
