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
package org.glassfish.admin.amx.impl.mbean;

import org.glassfish.admin.amx.core.Util;

import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.base.Query;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.jmx.ObjectNameQueryImpl;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.RegexUtil;
import org.glassfish.admin.amx.util.CollectionUtil;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.management.MBeanInfo;
import org.glassfish.admin.amx.core.proxy.AMXProxyHandler;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;

/**
 */
public class QueryMgrImpl extends AMXImplBase // implements Query
{

    public QueryMgrImpl(final ObjectName parentObjectName) {
        super(parentObjectName, Query.class);
    }

    public ObjectName[] queryProps(final String props) {
        return queryPattern(Util.newObjectNamePattern(getJMXDomain(), props));
    }

    public ObjectName[] queryTypes(final Set<String> types)
            throws IOException {
        final Set<ObjectName> result = new HashSet<ObjectName>();

        for (final ObjectName objectName : queryAll()) {
            if (types.contains(Util.getTypeProp(objectName))) {
                result.add(objectName);
            }
        }

        return asArray(result);
    }

    public ObjectName[] queryType(final String type) {
        return queryProps(Util.makeTypeProp(type));
    }

    public ObjectName[] queryName(final String name) {
        return queryProps(Util.makeNameProp(name));
    }

    public ObjectName[] queryPattern(final ObjectName pattern) {
        return asArray(JMXUtil.queryNames(getMBeanServer(), pattern, null));
    }

    /**
    @return Set<ObjectName> containing all items that have the matching type and name
     */
    public ObjectName[] queryTypeName(
            final String type,
            final String name) {
        return queryProps(Util.makeRequiredProps(type, name));
    }

    private static String[] convertToRegex(String[] wildExprs) {
        String[] regexExprs = null;

        if (wildExprs != null) {
            regexExprs = new String[wildExprs.length];

            for (int i = 0; i < wildExprs.length; ++i) {
                final String expr = wildExprs[i];

                final String regex = expr == null ? null : RegexUtil.wildcardToJavaRegex(expr);

                regexExprs[i] = regex;
            }
        }
        return (regexExprs);
    }

    private Set<ObjectName> matchWild(
            final Set<ObjectName> candidates,
            final String[] wildKeys,
            final String[] wildValues) {
        final String[] regexNames = convertToRegex(wildKeys);
        final String[] regexValues = convertToRegex(wildValues);

        final ObjectNameQueryImpl query = new ObjectNameQueryImpl();
        final Set<ObjectName> resultSet = query.matchAll(candidates, regexNames, regexValues);

        return resultSet;
    }

    public ObjectName[] queryWildAll(
            final String[] wildKeys,
            final String[] wildValues) {
        final ObjectName[] candidates = queryAll();
        final Set<ObjectName> candidatesSet = SetUtil.newSet(candidates);

        return asArray(matchWild(candidatesSet, wildKeys, wildValues));
    }

    public ObjectName[] queryAll() {
        final ObjectName pat = Util.newObjectNamePattern(getJMXDomain(), "");

        final Set<ObjectName> names = JMXUtil.queryNames(getMBeanServer(), pat, null);

        return asArray(names);
    }

    private final ObjectName[] asArray(final Set<ObjectName> items) {
        return CollectionUtil.toArray(items, ObjectName.class);
    }

    public ObjectName[] getGlobalSingletons() {
        final ObjectName[] all = queryAll();
        final List<ObjectName> globalSingletons = new ArrayList<ObjectName>();

        final ProxyFactory proxyFactory = getProxyFactory();
        for (final ObjectName candidate : all) {
            final MBeanInfo mbeanInfo = proxyFactory.getMBeanInfo(candidate);
            if (mbeanInfo != null && AMXProxyHandler.globalSingleton(mbeanInfo)) {
                globalSingletons.add(candidate);
            }
        }

        return CollectionUtil.toArray(globalSingletons, ObjectName.class);
    }

    public ObjectName getGlobalSingleton(final String type) {
        final ObjectName[] gs = getGlobalSingletons();
        for (final ObjectName objectName : gs) {
            if (Util.getTypeProp(objectName).equals(type)) {
                return objectName;
            }
        }
        return null;
    }

    public ObjectName[] queryDescendants(final ObjectName parentObjectName) {
        final AMXProxy parent = getProxyFactory().getProxy(parentObjectName);

        final List<AMXProxy> items = ParentChildren.hierarchy(parent).asList();

        return Util.toObjectNamesArray(items);
    }
}













