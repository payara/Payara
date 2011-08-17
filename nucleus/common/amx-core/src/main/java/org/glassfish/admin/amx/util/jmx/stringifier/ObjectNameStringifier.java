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

package org.glassfish.admin.amx.util.jmx.stringifier;

import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.amx.util.stringifier.Stringifier;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
Stringifier for an ObjectName which sorts the properties in the ObjectName
for more consistent and readable output.
 */
public final class ObjectNameStringifier implements Stringifier
{
    public final static ObjectNameStringifier DEFAULT = new ObjectNameStringifier();

    private static List<String> PROPS = null;

    private synchronized static List<String> getPROPS()
    {
        if (PROPS == null)
        {
            PROPS = Collections.unmodifiableList(ListUtil.newListFromArray(new String[]
                    {
                        "j2eeType", "type",
                        "name",
                        "J2EEDomain",
                        "J2EEServer",
                        "JVM",
                        "Node",
                        "J2EEApplication",
                        "AppClientModule",
                        "EJBModule",
                        "EntityBean",
                        "StatefulSessionBean",
                        "StatelessSessionBean",
                        "MessageDrivenBean",
                        "WebModule", "Servlet",
                        "ResourceAdapterModule",
                        "JavaMailResource",
                        "JCAResource",
                        "JCAConnectionFactory",
                        "JCAManagedConnectionFactory",
                        "JDBCResource",
                        "JDBCDataSource",
                        "JDBCDriver",
                        "JMSResource",
                        "JNDIResource",
                        "JTAResource",
                        "RMI_IIOPResource",
                        "URL_Resource",
                    }));
        }
        return (PROPS);
    }

    private List<String> mOrderedProps;

    private boolean mOmitDomain;

    public ObjectNameStringifier()
    {
        this(getPROPS());
    }

    public ObjectNameStringifier(final List<String> props)
    {
        mOrderedProps = props;
        mOmitDomain = false;
    }

    public ObjectNameStringifier(final String[] props)
    {
        this(ListUtil.newListFromArray(props));
    }

    private String makeProp(final String name, final String value)
    {
        return (name + "=" + value);
    }

    public String stringify(Object o)
    {
        if (o == null)
        {
            return ("null");
        }


        final ObjectName on = (ObjectName) o;

        final StringBuffer buf = new StringBuffer();
        if (!mOmitDomain)
        {
            buf.append(on.getDomain() + ":");
        }

        final Map<String, String> props = TypeCast.asMap(on.getKeyPropertyList());

        final List<String> ordered = new ArrayList<String>(mOrderedProps);
        ordered.retainAll(props.keySet());

        // go through each ordered property, and if it exists, emit it
        final Iterator<String> iter = ordered.iterator();
        while (iter.hasNext() && props.keySet().size() >= 2)
        {
            final String key = iter.next();
            final String value = props.get(key);
            if (value != null)
            {
                buf.append(makeProp(key, value) + ",");
                props.remove(key);
            }
        }

        // emit all remaining properties in order
        final Set<String> remainingSet = props.keySet();
        final String[] remaining = new String[remainingSet.size()];
        remainingSet.toArray(remaining);
        Arrays.sort(remaining);

        for (int i = 0; i < remaining.length; ++i)
        {
            final String key = remaining[i];
            final String value = props.get(key);
            buf.append(makeProp(key, value) + ",");
        }

        final String result = StringUtil.stripSuffix(buf.toString(), ",");

        return (result);
    }

    public List getProps()
    {
        return (mOrderedProps);
    }

    public void setProps(final List<String> props)
    {
        mOrderedProps = props;
    }

    public boolean getOmitDomain()
    {
        return (mOmitDomain);
    }

    public void setOmitDomain(final boolean omit)
    {
        mOmitDomain = omit;
    }

}
























