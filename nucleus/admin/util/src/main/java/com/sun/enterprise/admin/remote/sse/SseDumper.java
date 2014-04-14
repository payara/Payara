/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.remote.sse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author martinmares
 */
public class SseDumper {
  
    private static SseDumper instance = new SseDumper();

    private boolean active = false;
    private StringBuilder data = new StringBuilder();
    
    private SseDumper() {
    }
    
    public void activate() {
        active = true;
    }
    
    public void deactivate() {
        active = false;
    }
    
    public void addchar(char ch) {
        if (active) {
            data.append(ch);
        }
    }
    
    public void addchar(int ch) {
        addchar((char) ch);
    }
    
    private String getSeparator(String message, boolean endSeparator) {
        StringBuilder result = new StringBuilder(61);
        if (endSeparator) {
            result.append('\n');
        }
        result.append("###################################");
        if (message != null) {
            result.append(' ').append(message.trim().toUpperCase()).append(' ');
        }
        while (result.length() < 60) {
            result.append('#');
        }
        result.append('\n');
        return result.toString();
    }
    
    public void dump() {
        if (!active) {
            return;
        }
        String logfilename = System.getenv("AS_LOGFILE");
        if (logfilename == null || logfilename.isEmpty()) {
            logfilename = System.getProperty("AS_LOGFILE");
            if (logfilename == null || logfilename.isEmpty()) {
                return;
            }
        }
        File aslogfile = new File(logfilename);
        String name = aslogfile.getName();
        int ind = name.lastIndexOf('.');
        if (ind >= 0) {
            name = name.substring(0, ind);
        }
        File logFile = new File(aslogfile.getParent(), name + "_ssedump.log");
        int index = 1;
        while (logFile.exists()) {
            logFile = new File(aslogfile.getParent(), name + "_ssedump" + index +".log");
            index++;
        }
        //DUMP IT
        OutputStream os = null;
        try {
            os = new FileOutputStream(logFile);
            os.write((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(new Date()).getBytes());
            os.write('\n');
            os.write(getSeparator("raw output", false).getBytes());
            String msg = data.toString();
            os.write(msg.getBytes());
            os.write(getSeparator("(END) raw output", true).getBytes());
            os.flush();
            //Easy readable version
            os.write(getSeparator("NON PRINTABLE CHARS", false).getBytes());
            for (char ch : msg.toCharArray()) {
                switch (ch) {
                    case '\n':
                        os.write("\\n\n".getBytes());
                        break;
                    case '\r':
                        os.write("\\r\n".getBytes());
                        break;
                    case '\t':
                        os.write("\\t  ".getBytes());
                        break;
                    default:
                        os.write(ch);
                }
            }
            os.write(getSeparator("(END) NON PRINTABLE CHARS", false).getBytes());
            os.flush();
        } catch (Exception ex) {
            System.out.println("Unexpected exception: " + ex);
        } finally {
            try { os.close(); } catch (Exception ex) {}
        }
        //Remove cached data
        this.data = new StringBuilder();
        deactivate();
    }
    
    public static SseDumper getInstance() {
        return instance;
    }
  
}
