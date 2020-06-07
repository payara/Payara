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


import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Superclass for common ActionReport extension.
 *
 * @author Jerome Dochez
 */
public abstract class ActionReporter extends ActionReport {

    protected Throwable exception = null;
    protected String actionDescription = null;
    protected List<ActionReporter> subActions = new ArrayList<ActionReporter>();
    protected ExitCode exitCode = ExitCode.SUCCESS;
    protected MessagePart topMessage = new MessagePart();
    protected String contentType = "text/html";

    public static final String EOL_MARKER = "%%%EOL%%%";

    /** Creates a new instance of HTMLActionReporter */
    public ActionReporter() {
    }

    /**
     * Sets the exit code of the report to failure
     */
    public void setFailure() {
        setActionExitCode(ExitCode.FAILURE);
    }
    
    /**
     * 
     * @return 
     */
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
        ActionReporter subAction;
        try {
            subAction = this.getClass().newInstance();
        } catch (IllegalAccessException ex) {
            return null;
        } catch (InstantiationException ex) {
            return null;
        }
        subActions.add(subAction);
        return subAction;
    }

    @Override
    public List<ActionReporter> getSubActionsReport() {
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
    
    /**
     * Returns the content type to be used in sending the response back to 
     * the client/caller.
     * <p>
     * This is the default type.  Specific subclasses of ActionReporter might
     * override the method to return a different valid type.
     * @return content type to be used in formatting the command response to the client
     */
    @Override
    public String getContentType() {
        return contentType;
    }
    @Override
    public void setContentType(String s) {
        contentType = s;
    }

    /** Returns combined messages. Meant mainly for long running
     *  operations where some of the intermediate steps can go wrong, although
     *  overall operation succeeds. Does nothing if either of the arguments are null.
     *  The traversal visits the message of current reporter first. The various
     *  parts of the message are separated by EOL_MARKERs. 
     * <p>
     * Note: This method is a recursive implementation.
     * @param aReport a given (usually top-level) ActionReporter instance
     * @param sb StringBuilder instance that contains all the messages  
     */
    public void getCombinedMessages(ActionReporter aReport, StringBuilder sb) {
        if (aReport == null || sb == null)
            return;
        String mainMsg = ""; //this is the message related to the topMessage
        String failMsg; //this is the message related to failure cause
        // Other code in the server may write something like report.setMessage(exception.getMessage())
        // and also set report.setFailureCause(exception). We need to avoid the duplicate message.
        if (aReport.getMessage() != null && aReport.getMessage().length() != 0) {
            mainMsg = aReport.getMessage();
            String format = "{0}";
            if (ActionReport.ExitCode.WARNING.equals(aReport.getActionExitCode())) {
                LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ActionReporter.class);
                format = localStrings.getLocalString("flag.message.as.warning", "Warning: {0}");
            }
            if (ActionReport.ExitCode.FAILURE.equals(aReport.getActionExitCode())) {
                LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ActionReporter.class);
                format = localStrings.getLocalString("flag.message.as.failure", "Failure: {0}");
            }
            if (sb.length() > 0) sb.append(EOL_MARKER);
            sb.append(MessageFormat.format(format,mainMsg));
        }
        if (aReport.getFailureCause() != null && aReport.getFailureCause().getMessage() != null && aReport.getFailureCause().getMessage().length() != 0) {
            failMsg = aReport.getFailureCause().getMessage();
            if (!failMsg.equals(mainMsg)) {
                if (sb.length() > 0) sb.append(EOL_MARKER);
                sb.append(failMsg);
            }
        }
        for (ActionReporter sub : aReport.subActions) {
            getCombinedMessages(sub, sb);
        }
    }

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

    private static boolean has(ActionReporter ar, ExitCode value) {
        if (null != ar.exitCode && ar.exitCode.equals(value)) {
            return true;
        }
        Queue<ActionReporter> q = new LinkedList<ActionReporter>();
        q.addAll(ar.subActions);
        while (!q.isEmpty()) {
            ActionReporter lar = q.remove();
            ExitCode ec = lar.getActionExitCode();
            if (null != ec && ec.equals(value)) {
                return true;
            } else {
                q.addAll(lar.subActions);
            }
        }
        return false;
    }
}
