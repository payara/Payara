/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]
 */

package com.sun.enterprise.admin.report;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Class for reporting the result of a command using HTML.
 * <p>
 * 
 * @author dochez
 */
@Service(name = "html")
@PerLookup
public class HTMLActionReporter extends ActionReporter {
    
    /** Creates a new instance of HTMLActionReporter */
    public HTMLActionReporter() {
    }
    
    @Override
    public void writeReport(OutputStream os) throws IOException {
        PrintWriter writer = new PrintWriter(os);
        writer.print("<html><head/>");
        writer.println("<body>" +
                "<h1>GlassFish " + actionDescription + " command report</h1>" +
                "<br><br>");
        writer.println("Exit Code : " + this.exitCode);
        writer.println("<hr>");
        write(2, topMessage, writer);
        writer.println("<hr>");
        if (exception!=null) {
            writer.println("Exception raised during operation : <br>");
            writer.println("<pre>");
           exception.printStackTrace(writer);
            writer.println("</pre>");
        }
        if (subActions.size()>0) {
            writer.println("There are " + subActions.size() + " sub operations");
        }
        writer.print("</body></html>");
        writer.flush();        
    }

    private void write(int level, MessagePart part, PrintWriter writer) {
        String mess =  part.getMessage();
        if (mess==null){
            mess = "";//better than a null string output
        }
        if (level>6) {
            writer.println(mess);
        } else {
            writer.println("<h" + level + ">" + mess + "</h" + level + ">");
        }
        write(part.getProps(), writer);

        for (MessagePart child : part.getChildren()) {
            write(level+1, child, writer);
        }
    }
    
    private void write(Properties props, PrintWriter writer) {
        if (props==null || props.isEmpty()) {
            return;
        }
        writer.println("<table border=\"1\">");
        for (Map.Entry entry : props.entrySet()) {
            writer.println("<tr>");
            writer.println("<td>" + entry.getKey() + "</td>");
            writer.println("<td>" + entry.getValue() + "</td>");
            writer.println("</tr>");
        }
        writer.println("</table>");
        
    }
}
