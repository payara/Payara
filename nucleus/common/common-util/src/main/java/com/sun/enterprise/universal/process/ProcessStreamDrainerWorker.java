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

package com.sun.enterprise.universal.process;

import java.io.*;


///////////////////////////////////////////////////////////////////////////

class ProcessStreamDrainerWorker implements Runnable
{
    ProcessStreamDrainerWorker(InputStream in, PrintStream Redirect, boolean save)
    {
        if(in == null)
            throw new NullPointerException("InputStream argument was null.");
        
        reader = new BufferedInputStream(in);
        redirect = Redirect;

        if(save) {
            sb = new StringBuilder();
        }
    }
    
    public void run()
    {
        if(reader == null)
            return;
        
        try
        {
            int count = 0;
            byte[] buffer = new byte[4096];
            
            while ((count = reader.read(buffer)) != -1)
            {
                if(redirect != null)
                    redirect.write(buffer, 0, count);

               if(sb != null)
                   sb.append(new String(buffer, 0, count));
            }
        } 
        catch (IOException e)
        {
        }
    }

    String getString() {
        if(sb != null)
            return sb.toString();
        else
            return "";
    }
    
    private final   BufferedInputStream reader;
    private final   PrintStream         redirect;
    private         StringBuilder       sb;
}
