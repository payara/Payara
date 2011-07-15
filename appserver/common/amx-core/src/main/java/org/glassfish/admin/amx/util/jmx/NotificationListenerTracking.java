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

package org.glassfish.admin.amx.util.jmx;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 */
public class NotificationListenerTracking
{
    // NotificationListeners are not unique, so we can't use a Map
    private final List<NotificationListenerInfo> mInfos;

    public NotificationListenerTracking(boolean synchronize)
    {
        final List<NotificationListenerInfo> infos =
                new ArrayList<NotificationListenerInfo>();

        mInfos = synchronize ? Collections.synchronizedList(infos) : infos;
    }

    public void addNotificationListener(
            NotificationListener listener,
            NotificationFilter filter,
            Object handback)
    {
        final NotificationListenerInfo info =
                new NotificationListenerInfo(listener, filter, handback);

        mInfos.add(info);
    }

    public int getListenerCount()
    {
        return mInfos.size();
    }

    private final boolean listenersEqual(
            final NotificationListener listener1,
            final NotificationListener listener2)
    {
        return (listener1 == listener2);
    }

    private final boolean handbacksEqual(
            final Object handback1,
            final Object handback2)
    {
        return (handback1 == handback2);
    }

    /**
    Remove <b>all instances</b> of the specified listener and return
    their corresponding NotificationListenerInfo.
    This behavior matches the behavior of
    {@link javax.management.NotificationEmitter}.

    @return list of NotificationListenerInfo
     */
    public List<NotificationListenerInfo> removeNotificationListener(final NotificationListener listener)
    {
        final Iterator iter = mInfos.iterator();

        final List<NotificationListenerInfo> results = new ArrayList<NotificationListenerInfo>();

        while (iter.hasNext())
        {
            final NotificationListenerInfo info =
                    (NotificationListenerInfo) iter.next();

            if (listenersEqual(listener, info.getListener()))
            {
                iter.remove();
                results.add(info);
            }
        }

        return (results);
    }

    /**
    Remove <b>the first instance</b> of the specified listener/filter/handback
    combination and return its corresponding NotificationListenerInfo.
    This behavior matches the behavior of
    {@link javax.management.NotificationEmitter}.

    @return list of NotificationListenerInfo
     */
    public NotificationListenerInfo removeNotificationListener(
            final NotificationListener listener,
            final NotificationFilter filter,
            final Object handback)
    {
        final Iterator iter = mInfos.iterator();
        NotificationListenerInfo result = null;

        while (iter.hasNext())
        {
            final NotificationListenerInfo info =
                    (NotificationListenerInfo) iter.next();

            if (listenersEqual(listener, info.getListener()) &&
                handbacksEqual(handback, info.getHandback()))
            {
                iter.remove();
                result = info;
                break;
            }
        }

        return (result);
    }

}
























