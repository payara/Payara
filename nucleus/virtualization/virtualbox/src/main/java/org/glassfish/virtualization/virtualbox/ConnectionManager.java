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
package org.glassfish.virtualization.virtualbox;

import org.glassfish.virtualization.spi.VirtException;
import org.virtualbox_4_1.VBoxException;
import org.virtualbox_4_1.VirtualBoxManager;

/**
 *
 * @author ludo
 */
public class ConnectionManager {

    static public VirtualBoxManager connection(String machineName) throws VirtException {

 
        if ((machineName==null)||machineName.equals("")){
            machineName = "localhost";
        }
        VirtualBoxManager connection = VirtualBoxManager.createInstance(null);
        try {
            connection.connect("http://"+machineName+":18083", "", "");

        } catch (VBoxException e) {
            String message = "Connection Error : " + e;
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isMacOs = osName.startsWith("mac os x");
            if (isMacOs) {
                message = message + ", Did you start the VirtualBox deamon via /Applications/VirtualBox.app/Contents/MacOS/vboxwebsrv ?";

            } else if (osName.indexOf( "win" ) >= 0){
                message = message + ", Did you start the VirtualBox deamon via Program Files\\Oracle\\VirtualBox\\vboxwebsrv ?";

            }
            else {
                message = message + ", Did you start the VirtualBox deamon via /usr/lib/virtualbox/vboxwebsrv ?";

            }
            throw new VirtException(message, e);
        }

        return connection;
    }
}
