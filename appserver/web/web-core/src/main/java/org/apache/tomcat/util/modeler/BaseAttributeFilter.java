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

package org.apache.tomcat.util.modeler;


import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import java.util.HashSet;


/**
 * <p>Implementation of <code>NotificationFilter</code> for attribute change
 * notifications.  This class is used by <code>BaseModelMBean</code> to
 * construct attribute change notification event filters when a filter is not
 * supplied by the application.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2003/07/20 07:35:12 $
 */

public class BaseAttributeFilter implements NotificationFilter {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new filter that accepts only the specified attribute
     * name.
     *
     * @param name Name of the attribute to be accepted by this filter, or
     *  <code>null</code> to accept all attribute names
     */
    public BaseAttributeFilter(String name) {

        super();
        if (name != null)
            addAttribute(name);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of attribute names that are accepted by this filter.  If this
     * list is empty, all attribute names are accepted.
     */
    private HashSet<String> names = new HashSet<String>();


    // --------------------------------------------------------- Public Methods


    /**
     * Add a new attribute name to the set of names accepted by this filter.
     *
     * @param name Name of the attribute to be accepted
     */
    public void addAttribute(String name) {

        synchronized (names) {
            names.add(name);
        }

    }


    /**
     * Clear all accepted names from this filter, so that it will accept
     * all attribute names.
     */
    public void clear() {

        synchronized (names) {
            names.clear();
        }

    }


    /**
     * Return the set of names that are accepted by this filter.  If this
     * filter accepts all attribute names, a zero length array will be
     * returned.
     */
    public String[] getNames() {

        synchronized (names) {
            return names.toArray(new String[names.size()]);
        }

    }


    /**
     * <p>Test whether notification enabled for this event.
     * Return true if:</p>
     * <ul>
     * <li>This is an attribute change notification</li>
     * <li>Either the set of accepted names is empty (implying that all
     *     attribute names are of interest) or the set of accepted names
     *     includes the name of the attribute in this notification</li>
     * </ul>
     */
    public boolean isNotificationEnabled(Notification notification) {

        if (notification == null)
            return (false);
        if (!(notification instanceof AttributeChangeNotification))
            return (false);
        AttributeChangeNotification acn =
            (AttributeChangeNotification) notification;
        if (!AttributeChangeNotification.ATTRIBUTE_CHANGE.equals(acn.getType()))
            return (false);
        synchronized (names) {
            if (names.size() < 1)
                return (true);
            else
                return (names.contains(acn.getAttributeName()));
        }

    }


    /**
     * Remove an attribute name from the set of names accepted by this
     * filter.
     *
     * @param name Name of the attribute to be removed
     */
    public void removeAttribute(String name) {

        synchronized (names) {
            names.remove(name);
        }

    }


}
