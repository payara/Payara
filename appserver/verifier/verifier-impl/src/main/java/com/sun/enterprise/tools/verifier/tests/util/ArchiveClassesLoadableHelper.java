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

package com.sun.enterprise.tools.verifier.tests.util;

import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompiler;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import java.io.File;

/**
 * This class is a helper around {@link ClosureCompiler}.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ArchiveClassesLoadableHelper {
    /**
     * This method is used to print the result in various
     * *ArchiveClassesLoadable tests.
     * @param cc a closure compiler which provides the necesasry information
     * @return a localized string which contains the details.
     */
    public static String getFailedResult(ClosureCompiler cc){
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Object o : cc.getFailed().entrySet()) {
            Map.Entry<String, List<String>> referencingPathToFailedList =
                    (Map.Entry<String, List<String>>)o;
            LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
            String classes = "Failed to find following classes:";
            if (smh != null) {
                classes = smh.getLocalString(
                        ArchiveClassesLoadableHelper.class.getName() + ".classes",
                        classes);
            }
            sb.append(classes).append("\n[");
            for (Iterator<String> iii = referencingPathToFailedList.getValue().iterator();
                 iii.hasNext();) {
                sb.append("\n\t").append(iii.next());
                if(iii.hasNext()) sb.append(",");
            }
            sb.append("\n]");
            String referencingPath = referencingPathToFailedList.getKey();
            if(referencingPath.length()==0) continue; // skip if a top level class is not found
            String ref = "referenced in the following call stack :\n";
            String reference = "at";
            if (smh != null) {
               ref = smh.getLocalString(
                        ArchiveClassesLoadableHelper.class.getName() + ".ref",
                        ref);
                reference = smh.getLocalString(
                        ArchiveClassesLoadableHelper.class.getName() + ".reference",
                        reference);
            }
            StringTokenizer st = new StringTokenizer(referencingPath, File.separator);
            Stack<String> referencingClassStack = new Stack<String>();
            while(st.hasMoreTokens()) {
                referencingClassStack.push(st.nextToken());
            }
            if(!referencingClassStack.isEmpty())
                 sb.append("\n\n"+ref);
            while(!referencingClassStack.isEmpty()){
                sb.append("\n\t").append(reference).append(" ");
                sb.append(referencingClassStack.pop());
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }
}

