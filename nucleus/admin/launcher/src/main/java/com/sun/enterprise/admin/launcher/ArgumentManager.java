/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.launcher;

import java.util.*;

/**
 * API -- For now sticking with the draft1 API and behavior
 * This class will be handy for fixing error detection of bad input as below.
 * 
 * -name1 value1 -name2 value2 value3 value4 value5 -name3 -name4 -name5
 * --> "-name1":"value1",  "-name2":"value2", "default":"value5", "-name3":"-name4" 
 * 
 * @author bnevins
 */

public class ArgumentManager 
{
    public static Map<String,String> argsToMap(String[] sargs)
    {
        ArgumentManager mgr = new ArgumentManager(sargs);
        return mgr.getArgs();
    }
 
    public static Map<String,String> argsToMap(List<String>sargs)
    {
        ArgumentManager mgr = new ArgumentManager(sargs);
        return mgr.getArgs();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    //////   ALL PRIVATE BELOW      ///////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    
    private ArgumentManager(String[] sargs)
    {
        args = new ArrayList<String>();
        
        for(String s : sargs)
            args.add(s);
    }

    private ArgumentManager(List<String> sargs)
    {
        args = sargs;
    }

    private Map<String, String> getArgs()
    {
        int len = args.size();
        
        // short-circuit out of here!
        if(len <= 0)
            return map;
        
        for(int i = 0; i < len; i++)
        {
            String name = args.get(i);
            
            if(name.startsWith("-"))
            {
                // throw it away if there is no value left
                if(i + 1 < len)
                {
                    map.put(name, args.get(++i));
                }
            }
            else
            {
                // default --> last one wins!
                map.put("default", args.get(i));
            }
        }
        return map;
    }

    Map<String,String>  map     = new HashMap<String,String>();
    List<String>        args;
}
