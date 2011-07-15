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

import org.glassfish.admin.amx.util.stringifier.ArrayStringifier;
import org.glassfish.admin.amx.util.stringifier.Stringifier;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

public class MBeanInfoStringifier extends MBeanFeatureInfoStringifier implements Stringifier
{
    public static final MBeanInfoStringifier DEFAULT = new MBeanInfoStringifier();

    public MBeanInfoStringifier()
    {
        super();
    }

    public MBeanInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        super(options);
    }

    private String stringifyArray(Object[] a, Stringifier stringifier)
    {
        String temp = "";

        if (a.length != 0)
        {
            temp = "\n" + ArrayStringifier.stringify(a, "\n", stringifier);
        }
        return (temp);
    }

    // subclass may override
    MBeanAttributeInfoStringifier getMBeanAttributeInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        return (new MBeanAttributeInfoStringifier(options));
    }

    // subclass may override
    MBeanOperationInfoStringifier getMBeanOperationInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        return (new MBeanOperationInfoStringifier(options));
    }

    // subclass may override
    MBeanConstructorInfoStringifier getMBeanConstructorInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        return (new MBeanConstructorInfoStringifier(options));
    }

    // subclass may override
    MBeanNotificationInfoStringifier getMBeanNotificationInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        return (new MBeanNotificationInfoStringifier(options));
    }

    public String stringify(Object o)
    {
        String result = "";
        final MBeanInfo info = (MBeanInfo) o;

        final MBeanOperationInfo[] operations = info.getOperations();
        final MBeanAttributeInfo[] attributes = info.getAttributes();
        final MBeanConstructorInfo[] constructors = info.getConstructors();
        final MBeanNotificationInfo[] notifications = info.getNotifications();
        final String description = info.getDescription();

        result = "Summary: " +
                 operations.length + " operations, " +
                 attributes.length + " attributes, " +
                 constructors.length + " constructors, " +
                 notifications.length + " notifications" +
                 (description == null ? "" : ", \"" + description + "\"");

        final MBeanFeatureInfoStringifierOptions options =
                new MBeanFeatureInfoStringifierOptions(true, ",");

        // Do formal terms like "Attributes" need to be I18n?
        // Probabably not as they are part of a specification.
        result = result + "\n\n- Attributes -" +
                 stringifyArray(attributes, getMBeanAttributeInfoStringifier(options));

        result = result + "\n\n- Operations -" +
                 stringifyArray(operations, getMBeanOperationInfoStringifier(options));

        result = result + "\n\n- Constructors -" +
                 stringifyArray(constructors, getMBeanConstructorInfoStringifier(options));

        result = result + "\n\n- Notifications -" +
                 stringifyArray(notifications, getMBeanNotificationInfoStringifier(options));

        return (result);

    }

}





