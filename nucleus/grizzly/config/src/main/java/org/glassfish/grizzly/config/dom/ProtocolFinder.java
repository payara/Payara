/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.dom;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Describes a protocol finder/recognizer, which is able to recognize whether incoming request
 * belongs to the specific protocol or not. If yes - protocol-finder forwards request processing to a
 * specific protocol.
 */
@Configured
public interface ProtocolFinder extends ConfigBeanProxy, PropertyBag {
    /**
     * Finder name, which could be used as reference
     */
    @Attribute(key = true)
    String getName();

    void setName(String value);

    /**
     * Reference to a protocol, which was defined before.
     */
    @Attribute
    String getProtocol();

    void setProtocol(String value);

    /**
     * Finder logic implementation class
     */
    @Attribute(required = true)
    String getClassname();

    void setClassname(String value);

    @DuckTyped
    Protocol findProtocol();

    @DuckTyped
    PortUnification getParent();

    class Duck {
        public static Protocol findProtocol(ProtocolFinder finder) {
            String name = finder.getProtocol();
            final NetworkConfig networkConfig = finder.getParent().getParent().getParent().getParent();
            return networkConfig.findProtocol(name);
        }

        public static PortUnification getParent(ProtocolFinder finder) {
            return finder.getParent(PortUnification.class);
        }
    }
}
