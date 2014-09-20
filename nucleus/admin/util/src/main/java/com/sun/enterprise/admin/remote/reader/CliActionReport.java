/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.remote.reader;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.glassfish.api.ActionReport;

/**
 * Temporary implementation. Copy of AcctionReporter. It is here until 
 * ActionReport refactoring will be complete. 
 *
 * @author mmares
 */
//TODO: Remove when ActionReport refactoring will be done
public class CliActionReport extends ActionReport {

    private static final String EOL = System.getProperty("line.separator");
    
    protected Throwable exception = null;
    protected String actionDescription = null;
    protected List<CliActionReport> subActions = new ArrayList<CliActionReport>();
    protected ExitCode exitCode = ExitCode.SUCCESS;
    protected MessagePart topMessage = new MessagePart();

    /** Creates a new instance of HTMLActionReporter */
    public CliActionReport() {
    }

    public void setFailure() {
        setActionExitCode(ExitCode.FAILURE);
    }
    
    public boolean isFailure() {
        return getActionExitCode() == ExitCode.FAILURE;
    }
    
    public void setWarning() {
        setActionExitCode(ExitCode.WARNING);
    }

    public boolean isWarning() {
        return getActionExitCode() == ExitCode.WARNING;
    }
    
    public boolean isSuccess() {
        return getActionExitCode() == ExitCode.SUCCESS;
    }
    
    public void setSuccess() {
        setActionExitCode(ExitCode.SUCCESS);
    }
    
    @Override
    public void setActionDescription(String message) {
        this.actionDescription = message;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    @Override
    public void setFailureCause(Throwable t) {
        this.exception = t;
    }
    @Override
    public Throwable getFailureCause() {
        return exception;
    }
        
    @Override
    public MessagePart getTopMessagePart() {
        return topMessage;
    }

    @Override
    public ActionReport addSubActionsReport() {
        CliActionReport subAction = new CliActionReport();
        subActions.add(subAction);
        return subAction;
    }

    @Override
    public List<CliActionReport> getSubActionsReport() {
        return subActions;
    }

    @Override
    public void setActionExitCode(ExitCode exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public ExitCode getActionExitCode() {
        return exitCode;
    }

    @Override
    public void setMessage(String message) {
        topMessage.setMessage(message);
    }

    @Override
    public void appendMessage(String message) {
        topMessage.appendMessage(message);
    }

    @Override
    public String getMessage() {
        return topMessage.getMessage();
    }
        
    
    @Override
    public void setMessage(InputStream in) {
        try {
            if(in == null)
                throw new NullPointerException("Internal Error - null InputStream");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copyStream(in, baos);
            setMessage(baos.toString());
        }
        catch (Exception ex) {
            setActionExitCode(ExitCode.FAILURE);
            setFailureCause(ex);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }

        out.close();
        in.close();
    }
    
    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void setContentType(String s) {
        throw new UnsupportedOperationException();
    }

//    public static void getCombinedMessages(CliActionReport aReport, StringBuilder sb) {
//        if (aReport == null || sb == null)
//            return;
//        String mainMsg = ""; //this is the message related to the topMessage
//        String failMsg; //this is the message related to failure cause
//        // Other code in the server may write something like report.setMessage(exception.getMessage())
//        // and also set report.setFailureCause(exception). We need to avoid the duplicate message.
//        if (aReport.getMessage() != null && aReport.getMessage().length() != 0) {
//            if (sb.length() > 0) sb.append(EOL);
//            sb.append(aReport.getMessage());
//        }
//        if (aReport.getFailureCause() != null && aReport.getFailureCause().getMessage() != null && aReport.getFailureCause().getMessage().length() != 0) {
//            failMsg = aReport.getFailureCause().getMessage();
//            if (!failMsg.equals(mainMsg))
//                if (sb.length() > 0) sb.append(EOL);
//                sb.append(failMsg);
//        }
//        for (CliActionReport sub : aReport.subActions) {
//            getCombinedMessages(sub, sb);
//        }
//    }

    @Override
    public boolean hasSuccesses() {
        return has(this,ExitCode.SUCCESS);
    }

    @Override
    public boolean hasWarnings() {
        return has(this,ExitCode.WARNING);
    }

    @Override
    public boolean hasFailures() {
        return has(this,ExitCode.FAILURE);
    }

    private static boolean has(CliActionReport ar, ExitCode value) {
        if (null != ar.exitCode && ar.exitCode.equals(value)) {
            return true;
        }
        Queue<CliActionReport> q = new LinkedList<CliActionReport>();
        q.addAll(ar.subActions);
        while (!q.isEmpty()) {
            CliActionReport lar = q.remove();
            ExitCode ec = lar.getActionExitCode();
            if (null != ec && ec.equals(value)) {
                return true;
            } else {
                q.addAll(lar.subActions);
            }
        }
        return false;
    }

    @Override
    public void writeReport(OutputStream os) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void addIndent(int level, StringBuilder sb) {
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
    }
    
    private void messageToString(int indentLevel, String id, MessagePart msg, StringBuilder sb) {
        if (msg == null) {
            return;
        }
        addIndent(indentLevel, sb);
        sb.append("MESSAGE - ").append(id).append(EOL);
        if (msg.getMessage() != null && !msg.getMessage().isEmpty()) {
            addIndent(indentLevel, sb);
            sb.append(" : ").append(msg.getMessage()).append(EOL);
        }
        if (msg.getChildrenType() != null) {
            addIndent(indentLevel, sb);
            sb.append(" childrenType: ").append(msg.getChildrenType()).append(EOL);
        }
        for (Map.Entry<Object, Object> entry : msg.getProps().entrySet()) {
            addIndent(indentLevel, sb);
            sb.append(" >").append(entry.getKey()).append(" = ").append(entry.getValue());
            sb.append(EOL);
        }
        if (msg.getChildren() != null) {
            int counter = 0;
            for (MessagePart child : msg.getChildren()) {
                messageToString(indentLevel + 1, id + ".M" + counter, child, sb);
                counter++;
            }
        }
    }
    
    private String toString(int indentLevel, String id, CliActionReport ar) {
        if (id == null) {
            id = "0";
        }
        StringBuilder r = new StringBuilder();
        addIndent(indentLevel, r);
        r.append("ACTION REPORT - ").append(id);
        r.append(" [").append(ar.getActionExitCode().name()).append(']').append(EOL);
        if (ar.getActionDescription() != null) {
            addIndent(indentLevel, r);
            r.append(" actionDescription: ").append(ar.getActionDescription()).append(EOL);
        }
        if (ar.getFailureCause() != null) {
            addIndent(indentLevel, r);
            r.append(" failure: ");
            String msg = ar.getFailureCause().getMessage();
            if (msg != null && !msg.isEmpty()) {
                r.append(msg).append(EOL);
            } else {
                r.append('[').append(ar.getFailureCause().getClass().getName());
                r.append(']').append(EOL);
            }
        }
        if (ar.getExtraProperties() != null) {
            for (Map.Entry<Object, Object> entry : ar.getExtraProperties().entrySet()) {
                addIndent(indentLevel, r);
                r.append(" >").append(entry.getKey()).append(" = ").append(entry.getValue());
                r.append(EOL);
            }
        }
        messageToString(indentLevel + 1, id + ".M0", ar.getTopMessagePart(), r);
        r.append(EOL);
        if (ar.getSubActionsReport() != null) {
            int counter = 0;
            for (CliActionReport sub : ar.getSubActionsReport()) {
                r.append(toString(indentLevel + 1, id + "." + counter, sub));
                counter++;
            }
        }
        return r.toString();
    }
    
    @Override
    public String toString() {
        return toString(0, "0", this);
    }
}
