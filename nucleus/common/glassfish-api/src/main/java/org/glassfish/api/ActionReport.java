/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api;

import org.jvnet.hk2.annotations.Contract;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An action report is an abstract class allowing any type of server side action
 * like a service execution, a command execution to report on its execution
 * to the originator of the action. 
 * 
 * Implementations of this interface should provide a good reporting 
 * experience based on the user's interface like a browser or a command line 
 * shell. 
 *
 * @author Jerome Dochez
 */
@Contract
public abstract class ActionReport implements Serializable {
    
    private static final long serialVersionUID = -238144192513668688L;

    public enum ExitCode { SUCCESS, WARNING, FAILURE ;

        public boolean isWorse(final ExitCode other) {
            return (compareTo(other) > 0);
        }
    }
    
    public abstract void setActionDescription(String message);
    
    public abstract void setFailureCause(Throwable t);

    public abstract Throwable getFailureCause();

    public abstract void setMessage(String message);

    public abstract void appendMessage(String message);
    
    public abstract void writeReport(OutputStream os) throws IOException;

    public abstract void setMessage(InputStream in);

    public abstract String getMessage();
    
    public abstract MessagePart getTopMessagePart();
    
    public abstract ActionReport addSubActionsReport();
    
    public abstract void setActionExitCode(ExitCode exitCode);

    public abstract ExitCode getActionExitCode();
    
    public abstract String getContentType();

    public abstract void setContentType(String s);

    public abstract List<? extends ActionReport> getSubActionsReport();


    /**
     * Report a failure to the logger and {@link ActionReport}.
     *
     * This is more of a convenience to the caller.
     */
    public final void failure(Logger logger, String message, Throwable e) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, message,e );
        }
        logger.log(Level.SEVERE, message);
        if (e!=null) {
            setMessage(message + " : "+ e.toString());
            setFailureCause(e);
        } else {
            setMessage(message);
        }
        setActionExitCode(ActionReport.ExitCode.FAILURE);
    }

    /**
     * Short for {@code failure(logger,message,null)}
     */
    public final void failure(Logger logger, String message) {
        failure(logger,message,null);
    }

    /**
     * return true if the action report or a subaction report has ExitCode.SUCCESS.
     */
    public abstract boolean hasSuccesses();

    /**
     * return true if the action report or a subaction report has ExitCode.WARNING.
     */
    public abstract boolean hasWarnings();

    /**
     * return true if the action report or a subaction report has ExitCode.FAILURE.
     */
    public abstract boolean hasFailures();

    public static class MessagePart implements Serializable {

        private static final long serialVersionUID = -8708934987452414280L;

        Properties props = new Properties();
        String message;
        String childrenType;

        List<MessagePart> children = new ArrayList<MessagePart>();

        public MessagePart addChild() {
            MessagePart newPart = new MessagePart();
            children.add(newPart);
            return newPart;
        }

        public void setChildrenType(String type) {
            this.childrenType = type;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void appendMessage(String message) {
            // overkill Engineering seemingly but the strings might be HUGE
            // let the optimized JDK class handle it.
            if(this.message == null)
                this.message = message;
            else {
                StringBuilder sb = new StringBuilder(this.message);
                sb.append(message);
                this.message = sb.toString();
            }
        }

        public void addProperty(String key, String value) {
            props.put(key, value);
        }

        public Properties getProps() {
            return props;
        }

        public String getMessage() {
            return message;
        }

        public String getChildrenType() {
            return childrenType;
        }

        public List<MessagePart> getChildren() {
            return children;
        }
        
        protected String findPropertyImpl(final String key) {
            String value = props.getProperty(key);
            if (value != null) {
                return value;
            }
            for (MessagePart child : children) {
                value = child.findProperty(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        
        /** Search in message parts properties then in extra properties and then
         * in sub reports. Returns first occurrence of the key.
         */
        public String findProperty(String key) {
            if (key == null) {
                return null;
            }
            if (key.endsWith("_value")) {
                key = key.substring(0, key.length() - 6); //Because of back compatibility
            }
            return findPropertyImpl(key);
        }
        
        protected String toString(int indent) {
            StringBuilder result = new StringBuilder();
            if (message != null && !message.isEmpty()) {
                for (int i = 0; i < indent; i++) {
                    result.append(' ');
                }
                result.append(message);
            }
            for (MessagePart child : children) {
                String msg = child.toString(indent + 4);
                if (msg != null && !msg.isEmpty()) {
                    if (result.length() > 0) {
                        result.append('\n');
                    }
                    result.append(msg);
                }
            }
            return result.toString();
        }
        
        @Override
        public String toString() {
            return toString(0);
        }
        
    }

    Properties extraProperties;

    public final Properties getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Properties properties) {
        extraProperties = properties;
    }

    private Map resultTypes = new ConcurrentHashMap();

    /**
     * Gets a type that was set by the command implementation
     * @param resultType the type requested
     * @return <T> the actual instance that was set
     */
    public <T> T getResultType(Class<T> resultType) {
        return (T) resultTypes.get(resultType);
    }

    /**
     * Stores the supplies type and its instance. This is a way for the command implementation
     * to pass information between Supplemental command(s) and the main command. For example, the Supplemental
     * command for DeployCommand requires information on pay load, generated directories etc. In this case, the
     * DeployCommand will be expected to set this information in, for example DeployResult, and set it in the
     * ActionReport. The Supplemental Command will then retrieve the DeployResult for its use. 
     * @param resultType the type
     * @param resultTypeInstance the actual instance
     */
    public <T> void setResultType(Class<T> resultType, T resultTypeInstance) {
        resultTypes.put(resultType, resultTypeInstance);
    }
    
    /** Search in message parts properties then in extra properties and then
     * in sub reports. Returns first occurrence of the key.
     */
    public String findProperty(String key) {
        MessagePart topMessagePart = getTopMessagePart();
        if (topMessagePart != null) {
            String value = topMessagePart.findProperty(key);
            if (value != null) {
                return value;
            }
        }
        if (extraProperties != null) {
            String value = extraProperties.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        if (getSubActionsReport() != null) {
            for (ActionReport subReport : getSubActionsReport()) {
                String value = subReport.findProperty(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
}
