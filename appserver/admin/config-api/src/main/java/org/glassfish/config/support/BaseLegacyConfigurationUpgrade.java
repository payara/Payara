/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.config.LegacyConfigurationUpgrade;
import static org.glassfish.config.support.IntrospectionUtils.*;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

public abstract class BaseLegacyConfigurationUpgrade implements LegacyConfigurationUpgrade {
    protected void report(AdminCommandContext context, final String message) {
        context.getActionReport().setMessage("DEPRECATION WARNING: " + message);
    }

    protected void updatePropertyToAttribute(final AdminCommandContext context, final ConfigBeanProxy target,
        final String property, final String attribute)
        throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
            public Object run(ConfigBeanProxy param) {
                PropertyBag bag = (PropertyBag) param;
                final List<Property> propertyList = new ArrayList<Property>(bag.getProperty());
                setProperty(target, attribute, getValue(propertyList, property));
                final String message = MessageFormat.format("Moved {0}.property.{1} to {0}.{2}",
                    Dom.convertName(Dom.unwrap(target).getProxyType().getSimpleName()),
                    property,
                    Dom.convertName(attribute));
                report(context, message);
                bag.getProperty().clear();
                bag.getProperty().addAll(propertyList);
                return param;
            }
        }, target);
    }

    protected void removeProperty(final ConfigBeanProxy target, final String property)
        throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
            public Object run(ConfigBeanProxy param) {
                PropertyBag bag = (PropertyBag) param;
                final List<Property> propertyList = new ArrayList<Property>(bag.getProperty());
                final Iterator<Property> it = propertyList.iterator();
                boolean done = false;
                while (!done && it.hasNext()) {
                    Property prop = it.next();
                    if (property.equals(prop.getName())) {
                        done = true;
                        it.remove();
                    }
                }
                bag.getProperty().clear();
                bag.getProperty().addAll(propertyList);
                return param;
            }
        }, target);
    }

    private String getValue(List<Property> list, String property) {
        final Iterator<Property> iterator = list.iterator();
        while (iterator.hasNext()) {
            Property prop = iterator.next();
            if (property.equals(prop.getName())) {
                iterator.remove();
                return prop.getValue();
            }
        }
        return null;
    }


}
