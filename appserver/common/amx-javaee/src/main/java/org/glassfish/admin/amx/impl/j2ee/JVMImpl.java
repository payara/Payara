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

package org.glassfish.admin.amx.impl.j2ee;

import org.glassfish.admin.amx.j2ee.JVM;

import javax.management.ObjectName;
import org.glassfish.admin.amx.j2ee.J2EEManagedObject;

/**
Identifies a Java VM being utilized by a server.
 */
public final class JVMImpl
        extends J2EEManagedObjectImplBase {
    public static final Class<? extends J2EEManagedObject> INTF = JVM.class;

    public JVMImpl(
            final ObjectName parentObjectName, final Metadata meta) {
        super(parentObjectName, meta, INTF);
    }

    public String getjavaVersion() {
        return System.getProperty("java.version");
    }

    public String getjavaVendor() {
        return System.getProperty("java.vendor");
    }

    public String getnode() {
        String fullyQualifiedHostName = "localhost";

        /*
        Underlying MBean does not comply with JSR77.3.4.1.3, which states:
        "Identifies the node (machine) this JVM is running on. The value of the node
        attribute must be the fully quailified hostname of the node the JVM is running on."

        value seems to be of the form: BLACK/129.150.29.138

        Roll our own instead.
         */
        try {
            fullyQualifiedHostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException e) {
        }

        return (fullyQualifiedHostName);
    }
}
