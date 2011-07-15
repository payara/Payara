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

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Stack;

import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompiler;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * @author Sudipto Ghosh
 */
public class WebArchiveLoadableHelper {

    public static String getFailedResults(ClosureCompiler cc, File jspDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Object o : cc.getFailed().entrySet()) {
            Map.Entry<String, List<String>> referencingPathToFailedList =
                    (Map.Entry<String, List<String>>)o;
            LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
            String classes = "Failed to find following classes:";
            if (smh != null) {
                classes = smh.getLocalString(
                        WebArchiveLoadableHelper.class.getName() + ".classes",
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
            String ref = "referenced in the following call stack :";
            String reference = "at";
            if (smh != null) {
                ref = smh.getLocalString(
                        WebArchiveLoadableHelper.class.getName() + ".ref",
                        ref);
                reference = smh.getLocalString(
                        WebArchiveLoadableHelper.class.getName() + ".reference",
                        reference);
            }
            StringTokenizer st = new StringTokenizer(referencingPath, File.separator);
            Stack<String> referencingClassStack = new Stack<String>();
            boolean jspDirExists = (jspDir != null && jspDir.exists());
            final String jspPkgName = "org.apache.jsp.";
            while(st.hasMoreTokens()) {
                String className = st.nextToken();
                //This logic is to map the compiled jsp class to original jsp name.
                //The logic is to find whether the ref class is in jsp out dir. If true
                //then maniputale the ref class to get the jsp name
                String fileName = className.replace(".", File.separator)+".class";
                if (jspDirExists &&
                        className.startsWith(jspPkgName) &&
                        new File(jspDir, fileName).exists()) {
                    StringBuilder jspName = new StringBuilder(
                            className.substring(jspPkgName.length()));
                    int innerClassIndex = jspName.indexOf("$");
                    if (innerClassIndex != -1) {
                        jspName = jspName.replace(innerClassIndex, jspName.length(), "");
                    }
                    if(jspName.toString().endsWith("_jsp")) {
                        jspName = jspName.replace(jspName.lastIndexOf("_jsp"),
                                jspName.length(), ".jsp");
                    } else if(jspName.toString().endsWith("_jspx")) {
                        jspName = jspName.replace(jspName.lastIndexOf("_jspx"),
                                jspName.length(), ".jspx");
                    }
                    className = jspName.toString();
                }
                referencingClassStack.push(className);
            }
            if ((!referencingClassStack.isEmpty()))
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

