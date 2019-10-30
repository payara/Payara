/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.report;

import com.sun.enterprise.util.LocalStringManagerImpl;
import static com.sun.enterprise.util.StringUtils.ok;
import java.util.*;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;

/**
 *
 * @author Byron Nevins
 */
@Service(name = "plain")
@PerLookup
public class PlainTextActionReporter extends ActionReporter {

    public static final String MAGIC = "PlainTextActionReporter";

    @Override
    public void writeReport(OutputStream os) throws IOException {
        // The caller will read MAGIC and the next characters for success/failure
        // everything after the HEADER_END is good data
        writer = new PrintWriter(os);
        writer.print(MAGIC);
        if (isFailure()) {
            writer.print("FAILURE");
            Throwable t = getFailureCause();

            if (t != null) {
                writer.print(t);
            }
        }
        else {
            writer.print("SUCCESS");
        }

        StringBuilder finalOutput = new StringBuilder();
        getCombinedMessages(this, finalOutput);
        String outs = finalOutput.toString();

        if (!ok(outs)) {
            // we want at least one line of output.  Otherwise RemoteResponseManager
            // will consider this an error.  It is NOT an error there just is no data to report.
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(PlainTextActionReporter.class);
            writer.print(localStrings.getLocalString("get.mon.no.data", "No monitoring data to report."));
            writer.print("\n"); // forces an error to manifest constructor
        }
        else
            writer.print(outs);

        writer.flush();
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    /**
     * Append the string to the internal buffer -- not to the internal message string!
     * @param s the string to append
     */
    @Override
    final public void appendMessage(String s) {
        sb.append(s);
    }

    /**
     * Append the string to the internal buffer and add a linefeed like 'println'
     * @param s the string to append
     */
    final public void appendMessageln(String s) {
        sb.append(s).append('\n');
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        sb.delete(0, sb.length());
        appendMessage(message);
    }

    public final String getMessage() {
        return sb.toString();
    }

    @Override
    public void getCombinedMessages(ActionReporter aReport, StringBuilder out) {
        if(aReport == null || !(aReport instanceof PlainTextActionReporter) )
            throw new RuntimeException("Internal Error: Sub reports are different types than parent report.");
        // guaranteed safe above.
        PlainTextActionReporter ptr = (PlainTextActionReporter) aReport;
        String s = ptr.getOutputData();

        if (ok(s)) {
            if (out.length() > 0)
                out.append('\n');

            out.append(s);
        }

        for (ActionReporter ar : aReport.subActions) {
            getCombinedMessages(ar, out);
        }
    }

    private String getOutputData() {
        if (superSimple(topMessage))
            return simpleGetOutputData();
        else
            return notSoSimpleGetOutputData();
    }

    private boolean superSimple(MessagePart part) {
        // this is mainly here for backward compatability for when this Reporter
        // only wrote out the main message.
        List<MessagePart> list = part.getChildren();
        Properties props = part.getProps();
        boolean hasChildren = (list != null && !list.isEmpty());
        boolean hasProps = (props != null && props.size() > 0);

        // return true if we are very very simple!
        return !hasProps && !hasChildren;
    }

    private String simpleGetOutputData() {
        StringBuilder out = new StringBuilder();
        String tm = topMessage.getMessage();
        String body = sb.toString();

        if (ok(tm) && !ok(body))
            body = tm;

        if (ok(body)) {
            out.append(body);
        }

        return out.toString();
    }

    private String notSoSimpleGetOutputData() {
        StringBuilder out = new StringBuilder();

        if (ok(actionDescription)) {
            out.append("Description: ").append(actionDescription);
        }

        write("", topMessage, out);
        return out.toString();
    }

    private void write(String indent, MessagePart part, StringBuilder out) {
        out.append(indent).append(part.getMessage()).append('\n');
        write(indent + INDENT, part.getProps(), out);

        for (MessagePart child :
                part.getChildren()) {
            write(indent + INDENT, child, out);
        }
    }

    private void write(String indent, Properties props, StringBuilder out) {
        if (props == null || props.size() <= 0) {
            return;
        }

        for (Map.Entry<Object, Object> entry :
                props.entrySet()) {
            String key = "" + entry.getKey();
            String val = "" + entry.getValue();
            out.append(indent).append('[').append(key).append('=').append(val).append("\n");
        }
    }
    private transient PrintWriter writer;
    private static final String INDENT = "    ";
    private final StringBuilder sb = new StringBuilder();
}
