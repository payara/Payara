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

/**
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/PoolException.java,v $</I>
 * @author     $Author: tcfujii $
 * @version    $Revision: 1.3 $ $Date: 2005/12/25 04:13:35 $
 */
 
package com.sun.ejb.containers.util.pool;

import java.io.PrintStream;
import java.io.PrintWriter;

public class PoolException
    extends RuntimeException
{
    Throwable throwable = null;
    
    public PoolException() {
        super();
    }
    
    public PoolException(String message) {
        super(message);
    }
    
    public PoolException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
    
    public Throwable getThrowable() {
    	return this.throwable;
    }
    
    @Override
    public void printStackTrace() {
    	printStackTrace(new PrintWriter(System.err));
    }
    
    @Override
    public void printStackTrace(PrintStream ps) {
    	printStackTrace(new PrintWriter(ps));
    }
    
    @Override
    public void printStackTrace(PrintWriter pw) {
    	if (throwable != null) {
            pw.println("PoolException: " + getMessage());
            throwable.printStackTrace(pw);
    	} else {
            super.printStackTrace(pw);
    	}
    }
    
}
