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

import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.stringifier.SmartStringifier;
import org.glassfish.admin.amx.util.stringifier.Stringifier;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import java.util.Date;

public class NotificationStringifier implements Stringifier
{
    public static final NotificationStringifier DEFAULT = new NotificationStringifier();

    protected Options mOptions;

    public final static class Options
    {
        // don't make 'final' fields; allow changes after instantiation
        public boolean mIncludeObjectName;

        public boolean mIncludeTimeStamp;

        public boolean mIncludeType;

        public boolean mIncludeSequenceNumber;

        public boolean mIncludeUserData;

        public String mDelim;

        public Options()
        {
            mIncludeObjectName = true;
            mIncludeTimeStamp = true;
            mIncludeType = true;
            mIncludeSequenceNumber = true;
            mIncludeUserData = false;
            mDelim = ", ";
        }

    }

    public NotificationStringifier()
    {
        mOptions = new Options();
    }

    public NotificationStringifier(Options options)
    {
        mOptions = options;
    }

    protected void append(StringBuffer b, Object o)
    {
        if (b.length() != 0)
        {
            b.append(mOptions.mDelim);
        }

        b.append(SmartStringifier.toString(o));
    }

    public String stringify(Object o)
    {
        final Notification notif = (Notification) o;

        return (_stringify(notif).toString());
    }

    public static String toString(Object o)
    {
        return (DEFAULT.stringify(o));
    }

    protected StringBuffer _stringify(Notification notif)
    {
        final StringBuffer b = new StringBuffer();

        if (mOptions.mIncludeSequenceNumber)
        {
            append(b, "#" + notif.getSequenceNumber());
        }

        if (mOptions.mIncludeTimeStamp)
        {
            append(b, new Date(notif.getTimeStamp()));
        }

        if (mOptions.mIncludeObjectName)
        {
            append(b, StringUtil.quote(notif.getSource()));
        }

        if (mOptions.mIncludeType)
        {
            append(b, notif.getType());
        }

        if (mOptions.mIncludeUserData)
        {
            append(b, StringUtil.quote(notif.getUserData()));
        }

        if (notif instanceof MBeanServerNotification)
        {
            // this should really be done in a MBeanServerNotificationStringifier!
            final MBeanServerNotification n = (MBeanServerNotification) notif;

            append(b, StringUtil.quote(n.getMBeanName()));
        }

        return (b);
    }

}



















