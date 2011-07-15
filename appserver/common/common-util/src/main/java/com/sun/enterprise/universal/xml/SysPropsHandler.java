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

import java.util.HashMap;
import java.util.Map;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * MiniXmlParser is getting way too long and complicated.  All new code should go
 * into new and/or different classes.
 *
 * This class is for organizing system-property's from several different parts of the
 * config.
 * The system-property order of priority, from highest to lowest is:
 * 1. <server>
 * 2. <cluster> -- if applicable
 * 3. <config>
 * 4. <domain>
 * ref: http://java.net/jira/browse/GLASSFISH-16121
 * created April 8, 2011
 * @author Byron Nevins
 */
class SysPropsHandler {
    enum Type {
        SERVER, CLUSTER, CONFIG, DOMAIN
    };

    Map<String, String> getCombinedSysProps() {
        Map<String, String> map = new HashMap<String, String>(domain);
        map.putAll(config);
        map.putAll(cluster);
        map.putAll(server);

        return map;
    }

    // perhaps a bit inefficient.
    // TODO go through the maps one after the next.
    String get(String key) {
        return getCombinedSysProps().get(key);
    }

    // TODO these 2 add methods could be made more efficient.  Probably not worth
    // the effort.

    void add(Type type, Map<String, String> map) {
        if (type == null || map == null)
            return; // TODO : throw ????

        switch (type) {
            case SERVER:
                server.putAll(map);
                break;
            case CLUSTER:
                cluster.putAll(map);
                break;
            case CONFIG:
                config.putAll(map);
                break;
            case DOMAIN:
                domain.putAll(map);
                break;
            default:
                throw new IllegalArgumentException("unknown type");
       }
    }
    void add(Type type, String name, String value) {
        if (type == null || !ok(name))
            //throw new NullPointerException();
            return; // TODO : throw ????

        switch (type) {
            case SERVER:
                server.put(name, value);
                break;
            case CLUSTER:
                cluster.put(name, value);
                break;
            case CONFIG:
                config.put(name, value);
                break;
            case DOMAIN:
                domain.put(name, value);
                break;
            default:
                throw new IllegalArgumentException("unknown type");
        }
    }
    private Map<String, String> server = new HashMap<String, String>();
    private Map<String, String> cluster = new HashMap<String, String>();
    private Map<String, String> config = new HashMap<String, String>();
    private Map<String, String> domain = new HashMap<String, String>();
}
