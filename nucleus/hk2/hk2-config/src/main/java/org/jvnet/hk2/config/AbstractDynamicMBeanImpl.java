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

import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.Attribute;
import javax.management.InvalidAttributeValueException;
import javax.management.DynamicMBean;
import java.util.List;

/**
 * Partial implementation of {@link DynamicMBean}.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractDynamicMBeanImpl implements DynamicMBean {
    public final AttributeList getAttributes(String[] attributes) {
        AttributeList r = new AttributeList(attributes.length);
        for (String name : attributes) {
            Object value = null;
            try {
                value = getAttribute(name);
            } catch (AttributeNotFoundException e) {
                // error is reported as the lack of value
            } catch (MBeanException e) {
                // error is reported as the lack of value
            } catch (ReflectionException e) {
                // error is reported as the lack of value
            }
            r.add(new Attribute(name, value));
        }
        return r;
    }

    public final AttributeList setAttributes(AttributeList attributes) {
        AttributeList r = new AttributeList(attributes.size());
        for (Object a : attributes) {
            try {
                setAttribute(Attribute.class.cast(a));
                r.add(Attribute.class.cast(a));
            } catch (AttributeNotFoundException e) {
                // error is silently ignored
            } catch (ReflectionException e) {
                // error is silently ignored
            } catch (MBeanException e) {
                // error is silently ignored
            } catch (InvalidAttributeValueException e) {
                // error is silently ignored
            }
        }
        return r;
    }
}
