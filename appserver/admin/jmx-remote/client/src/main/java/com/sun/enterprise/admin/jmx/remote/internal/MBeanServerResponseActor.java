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
 * MBeanServerResponseActor.java
 * $Id: MBeanServerResponseActor.java,v 1.3 2005/12/25 04:26:34 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:34 $
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 * Unit Testing Information:
 * 0. Is Standard Unit Test Written (y/n):
 * 1. Unit Test Location: (The instructions should be in the Unit Test Class itself).
 */

package com.sun.enterprise.admin.jmx.remote.internal;

import javax.management.remote.message.MBeanServerResponseMessage;

/** A class to <code> act </code> on the instances of {@link MBeanServerResponseMessage}.
 * Since the response may have both the exceptions and valid results, we have to
 * carefully handle them.
 * @author  mailto:Kedar.Mhaswade@Sun.Com
 * @since Sun Java System Application Server 8
 */
class MBeanServerResponseActor {
    
    private MBeanServerResponseActor() {
        //disllow
    }
    static final void voidOrThrow(final MBeanServerResponseMessage message) throws Exception {
        if (message.isException()) {
            throw ((Exception)message.getWrappedResult());
        }
        /*ignore the return value, if there is no exception, as the message
        implies a method invocation that returns void. */
    }
    
    static final Object returnOrThrow(final MBeanServerResponseMessage message) throws Exception {
        voidOrThrow(message);
        return ( message.getWrappedResult() );
    }
}
