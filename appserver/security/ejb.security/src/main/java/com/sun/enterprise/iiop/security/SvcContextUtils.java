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

package com.sun.enterprise.iiop.security;


import com.sun.corba.ee.org.omg.CSI.MTCompleteEstablishContext;
import com.sun.corba.ee.org.omg.CSI.MTContextError;
import com.sun.corba.ee.org.omg.CSI.MTEstablishContext;
import com.sun.corba.ee.org.omg.CSI.MTMessageInContext;
import java.util.logging.*;
import com.sun.logging.*;

/**
 * This class contains the utility methods for dealing with
 * service contexts.
 *
 * @author: Sekhar Vajjhala
 *
 */

public class SvcContextUtils {

    private static java.util.logging.Logger _logger=null;
    static{
       _logger=LogDomains.getLogger(SvcContextUtils.class,LogDomains.SECURITY_LOGGER);
        }
    /** 
     * Define minor codes for errors specified in section 4.5,
     * "ContextError Values and Exceptions"
     * 
     * Currently only MessageInContextMinor code is defined since this
     * is the only used by the security interceptors.
     */

    public static final int MessageInContextMinor = 4 ;


    /** 
     *  Hard code the value of 15 for SecurityAttributeService until
     *  it is defined in IOP.idl.
     *     sc.context_id = SecurityAttributeService.value;
     */
    private static final int SECURITY_ATTRIBUTE_SERVICE_ID = 15;

    /**
     * Define mnemonic strings for SAS message types for debugging
     * purposes.
     */

    private static final String  EstablishContextName  = "EstablishContext";
    private static final String  CompleteEstablishName = "CompleteEstablishContext";
    private static final String  MessageInContextName  = "MessageInContext";
    private static final String  ContextErrorName      = "ContextError";

    /**
     * returns a mnemonic name for the message type based on the 
     * SASContextBody union discriminant 
     */

    public static String getMsgname(short discr) {
  
        String name = null;

        switch (discr) {

        case MTEstablishContext.value:
            name = EstablishContextName;
            break;

        case MTContextError.value:
            name = ContextErrorName;
            break;
 
        case MTCompleteEstablishContext.value:
            name = CompleteEstablishName;
            break;

        case MTMessageInContext.value:
            name = MessageInContextName;
            break;

        default:
		_logger.log(Level.SEVERE,"iiop.unknown_msgtype");
            break;  
	}
        return name;
    }
}
        


