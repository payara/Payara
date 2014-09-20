/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util.cluster;

import java.util.*;

public final class NodeInfo {

    private final String host;
    private final String name;
    private final String installDir;
    private final String instancesList;
    private final String type;

    private static final String NAME = Strings.get("ListNode.name");
    private static final String TYPE = Strings.get("ListNode.type");
    private static final String HOST = Strings.get("ListNode.host");
    private static final String INSTALLDIR = Strings.get("ListNode.installDir");
    private static final String INSTANCESLIST = Strings.get("ListNode.instancesList");

    public NodeInfo(String name1, String host1, String installDir1, String type1, String instancesList1){
        this.name = name1;
        this.host = host1;
        this.installDir = installDir1;
        this.type = type1;
        this.instancesList = instancesList1;
    }


    public final String getHost() {
        return host;
    }

    public final String getType() {
        return type;
    }

    public final String getName() {
        return name;
    }

    public final String getInstallDir() {
        return installDir;
    }

    public final String getInstancesList() {
        return instancesList;
    }
    
    public static String format(List<NodeInfo> infos) {
        int longestName = NAME.length();
        int longestType = TYPE.length();
        int longestInstallDir = INSTALLDIR.length();
        int longestHost = HOST.length();
        int longestInstancesList = INSTANCESLIST.length();

        int nullStringLen = 4;
        for (NodeInfo info : infos) {
            int namel = info.getName().length();
            int hostl = info.getHost() != null ? info.getHost().length(): nullStringLen;
            int type1 = info.getType() != null ? info.getType().length(): nullStringLen;
            int installDir1 = info.getInstallDir() != null ? info.getInstallDir().length() : nullStringLen;
            int instancesList1 = info.getInstancesList() != null ? info.getInstancesList().length() : nullStringLen;

            if (namel > longestName)
                longestName = namel;
            if (hostl > longestHost)
                longestHost = hostl;
            if (type1 > longestType)
                longestType = type1;
            if (installDir1 > longestInstallDir)
                longestInstallDir = installDir1;
            if (instancesList1 > longestInstancesList)
                longestInstancesList = instancesList1;
        }

        longestName += 2;
        longestHost += 2;
        longestType += 2;
        longestInstallDir += 2;
        longestInstancesList += 2;

        StringBuilder sb = new StringBuilder();

        String formattedLine =
                "%-" + longestName
                + "s %-" + longestType
                + "s %-" + longestHost
                + "s %-" + longestInstallDir
                + "s %-" + longestInstancesList
                + "s";

        sb.append(String.format(formattedLine, NAME, TYPE, HOST, INSTALLDIR, INSTANCESLIST));
        sb.append('\n');

        // no linefeed at the end!!!
        boolean first = true;
        for (NodeInfo info : infos) {
            if (first)
                first = false;
            else
                sb.append('\n');

            sb.append(String.format(formattedLine, info.getName(), info.getType(),  info.getHost(), info.getInstallDir(), info.getInstancesList()));
        }

        return sb.toString();
    }



}
