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
package com.sun.enterprise.universal.xml;

import com.sun.enterprise.universal.collections.CollectionUtils;
import java.util.*;
//import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Normally I'd have named this class "Cluster" but we don't want it to clash
 * with the config class with the same name.  This name makes life easier.
 *
 * This class is only used in this package.  It is more like an old-fashioned
 * "C" struct.  Help yourself to the data variables!
 *
 * A cluster in domain.xml contains 2 things that we care about:
 * <ul>
 * <li> a list of servers that the cluster owns
 * <li> a list of system properties
 *
 * created April 9, 2011
 * @author Byron Nevins
 */
final class ParsedCluster {
    ParsedCluster(String theName) {
        name = theName;
    }

    // forced to make this public...
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Cluster: ");
        sb.append(name).append('\n');
        sb.append("properties: ").append(CollectionUtils.toString(sysProps)).append('\n');
        sb.append("server names: ").append(CollectionUtils.toString(serverNames)).append('\n');
        return sb.toString();
    }

    Map<String, String> getMySysProps(String serverName) {
        if(serverNames.contains(serverName))
            return sysProps;
        return null;
    }

    final Map<String, String> sysProps = new HashMap<String, String>();
    final List<String> serverNames = new ArrayList<String>();
    private final String name;

}
