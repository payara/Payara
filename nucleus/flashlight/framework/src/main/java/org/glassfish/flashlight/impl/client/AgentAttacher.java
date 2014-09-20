/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.flashlight.impl.client;

/**
 * created May 26, 2011
 * @author Byron Nevins
 */
public final class AgentAttacher {
    public synchronized static boolean canAttach() {
        return canAttach;
    }
    
    public synchronized static boolean isAttached() {
        try {
            if (!canAttach)
                return false;

            return AgentAttacherInternal.isAttached();
        }
        catch (Throwable t) {
            return false;
        }
    }

    public synchronized static boolean attachAgent() {

        try {
            if (!canAttach)
                return false;

            return attachAgent(-1, "");
        }
        catch (Throwable t) {
            return false;
        }
    }

    public synchronized static boolean attachAgent(int pid, String options) {
        try {
            if (!canAttach)
                return false;

            return AgentAttacherInternal.attachAgent(pid, options);
        }
        catch (Throwable t) {
            return false;
        }
    }

    private final static boolean canAttach;

    static {
        boolean b = false;
        try {
            // this will cause a class not found error if tools.jar is missing
            // this is a distinct possibility in embedded mode.
            AgentAttacherInternal.isAttached();
            b = true;
        }
        catch (Throwable t) {
            b = false;
        }
        canAttach = b;
    }
}
