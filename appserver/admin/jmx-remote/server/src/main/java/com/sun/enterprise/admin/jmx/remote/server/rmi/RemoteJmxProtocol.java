/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * RjmxProtocol.java
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. No tabs are used, all spaces.
 * 2. In vi/vim -
 *      :set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *      1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *      2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = True.
 *      3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 * Unit Testing Information:
 * 0. Is Standard Unit Test Written (y/n):
 * 1. Unit Test Location: (The instructions should be in the Unit Test Class itself).
 */

package com.sun.enterprise.admin.jmx.remote.server.rmi;

/** An enumerated type for the Remote Jmx protocol.
 *
 * @author  mailto:Kedar.Mhaswade@Sun.Com
 * @since Sun Java System Application Server 8
 */
public class RemoteJmxProtocol {

    private final String prtr;
    /** Field */
    public static final RemoteJmxProtocol RMIJRMP	= new RemoteJmxProtocol("rmi_jrmp");
    /** Field */
    public static final RemoteJmxProtocol RMIIIOP	= new RemoteJmxProtocol("rmi_iiop");
    /** Field */
    public static final RemoteJmxProtocol JMXMP         = new RemoteJmxProtocol("jmxmp");
    /*Implementation note: The fields above are defined per default values in DTD */
    /** Creates a new instance of RemoteJmxProtocol */
    private RemoteJmxProtocol(final String prtr) {
        this.prtr = prtr;
    }

    public String getName() {
        return ( this.prtr );
    }

    public static RemoteJmxProtocol instance(final String prtr) {
        if (RMIJRMP.getName().equals(prtr))
            return ( RMIJRMP );
        else if (RMIIIOP.getName().equals(prtr))
            return ( RMIIIOP );
        else if (JMXMP.getName().equals(prtr))
            return ( JMXMP );
        else
            throw new UnsupportedOperationException("Unsupported: " + prtr);
    }
}
