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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.enterprise.universal;

import com.sun.enterprise.util.StringUtils;

import java.util.*;

/**
 *
 * @author  bnevins
 * @since October 2, 2001
 */
public class SystemProps
{
    public static List<Map.Entry> get()
    {
        Properties p = System.getProperties();
        // these 2 lines woul;d be nice -- but it's case-sensitive...
        //Map sortedMap = new TreeMap(p);
        //Set sortedSet = sortedMap.entrySet();
        Set<Map.Entry<Object, Object>>  set  = p.entrySet();
        List<Map.Entry>	list = new ArrayList<>(set);

        Collections.sort(list, (me1, me2) -> ((String)me1.getKey()).compareToIgnoreCase((String)me2.getKey()));

        return list;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String toStringStatic()
    {
        int             longestKey	= 0;
        List<Map.Entry>	list		= get();
        StringBuilder	sb		    = new StringBuilder();

        /* Go through the list twice.
         * The first time through gets the maximum length entry
         * The second time through uses that info for 'pretty printing'
         */

        for(Map.Entry entry : list)
        {
            int len = ((String)entry.getKey()).length();

            if(len > longestKey)
                longestKey = len;
        }

        longestKey += 1;

        for(Map.Entry entry : list)
        {
            sb.append(StringUtils.padRight((String)entry.getKey(), longestKey));
            sb.append("= ");
            sb.append((String)entry.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    private SystemProps()
    {
    }

    ///////////////////////////////////////////////////////////////////////////

    public static void main(String[] args)
    {
        System.out.println(toStringStatic());
    }
}
