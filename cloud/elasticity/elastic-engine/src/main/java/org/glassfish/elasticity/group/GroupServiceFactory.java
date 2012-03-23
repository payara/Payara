/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.elasticity.group;

import org.glassfish.elasticity.group.gms.GroupServiceProvider;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Mahesh Kannan
 *
 */
public class GroupServiceFactory {

    private ConcurrentHashMap<String, GroupServiceProvider> groupHandles
            = new ConcurrentHashMap<String, GroupServiceProvider>();

    private static final GroupServiceFactory _instance = new GroupServiceFactory();

    private GroupServiceFactory() {
    }

    public static GroupServiceFactory getInstance() {
        return _instance;
    }

    public synchronized GroupService getGroupService(String myName, String groupName, boolean startGMS) {
        String key = makeKey(myName, groupName);
        GroupServiceProvider server = groupHandles.get(key);
        if (server == null) {
            server = new GroupServiceProvider(myName, groupName, startGMS);
            groupHandles.put(key, server);
        }

        return server;
    }

    private static String makeKey(String myName, String groupName) {
        return myName + ":" + groupName;
    }

    public void shutdown(String myName, String groupName) {
        String key = makeKey(myName, groupName);
        GroupServiceProvider server = groupHandles.remove(key);
        if (server != null) {
            server.shutdown();
        }
    }

    public static void main(String[] args)
            throws Exception {
        GroupServiceFactory factory = GroupServiceFactory.getInstance();
        factory.getGroupService(args[0], args[1], true/*startGMS*/);
    }
}
