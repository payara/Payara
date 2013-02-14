/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.admincli.util;

import java.io.*;

/**
 *
 * @author Administrator
 */
public class RtExec {
    private static boolean result=false;
    private static Process proc;
    private static Runtime rt = Runtime.getRuntime();
    private static int exitVal = 101;

    public static boolean execute(String cmd) throws IOException {
        try {
            System.out.println("RtExec.execute: " + cmd);
            proc = rt.exec(cmd);

            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

            // Flush out the OUTPUT and ERROR
            errorGobbler.start();
            outputGobbler.start();

            // Checking exit satus
            exitVal = proc.waitFor();
//            System.out.println("ExitValue: " + exitVal);
            if (exitVal == 0){
                result = true;
            } else {
                result = false;
                System.out.println(outputGobbler.getOutput());
                System.out.println(errorGobbler.getOutput());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return result;
    }
}
