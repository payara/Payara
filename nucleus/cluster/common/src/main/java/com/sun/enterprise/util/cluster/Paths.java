/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.StringUtils;

/**
 * Why go through the painful process of creating Paths and hard-coding in filenames
 * in more than one place?  Let's do it in here and only in here
 * @author Byron Nevins
 */
public final class Paths {
    public final static String DAS_PROPS_FILENAME =  "das.properties";
    public final static String DAS_PROPS_SUBPATH =  "agent/config/" + DAS_PROPS_FILENAME;

    // TODO: the Node class ought to do this!
    public final static String getNodesDir(final Node node) {
        if (node == null) // don't do that!
            throw new NullPointerException();

        String nodesDir = node.getNodeDirAbsoluteUnixStyle();

        if (nodesDir == null)
            nodesDir = node.getInstallDirUnixStyle() + "/glassfish/nodes";

        return nodesDir;
    }

    public final static String getNodeDir(final Node node) {
        return getNodesDir(node) + "/" + node.getName();
    }

    public final static String getDasPropsPath(final Node node) {
        return getNodeDir(node) + "/" + DAS_PROPS_SUBPATH;
    }

    public static String getInstanceDirPath(final Node node, final String instanceName) {
        if (!StringUtils.ok(instanceName))
            throw new IllegalArgumentException(); // don't do that!

        return getNodeDir(node) + "/" + instanceName;
    }

    private Paths() {
        // all static methods
    }
}
