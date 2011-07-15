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

package com.sun.enterprise.jbi.serviceengine.util;

import java.util.HashMap;


/**
 * This provides necessary methods for the components to  pass the task result data to
 * ManagemntMessageBuilder. ManagementMessageBuilder constructs an XML document based on
 * this.
 *
 * @author bhavanishankar@dev.java.net.
 */
public class ComponentMessageHolder {
    /**
     * Type of message to build. Currently it can either be 'Status' or 'Exception'
     */
    private String mMsgTypeToBuild = null;

    /**
     * Cache for Localized Tokens
     */
    private HashMap mLocTokenCache = new HashMap();

    /**
     * Cache for Localized Tokens
     */
    private HashMap mLocMessageCache = new HashMap();

    /**
     * Cache for Localized Tokens
     */
    private HashMap mLocParamCache = new HashMap();

    /**
     * Exception Object
     */
    private Throwable mExceptionObject = null;

    /**
     * task name
     */
    private String mTaskName = null;

    /**
     * task result
     */
    private String mTaskResult = null;

    /**
     * status message type
     */
    private String mStatusMsgType = null;

    /**
     * exception message type
     */
    private String mExceptionMsgType = null;

    /**
     * component name
     */
    private String mComponentName = null;

    /**
     * Creates a new instance of MessageContentHolder with a message type.
     *
     * @param msgType String describing the message type to build.
     */
    public ComponentMessageHolder(String msgType) {
        mMsgTypeToBuild = msgType;
    }

    /**
     * Set the name of the task executed by component.
     *
     * @param taskName - task executed by component.
     */
    public void setTaskName(String taskName) {
        mTaskName = taskName;
    }

    /**
     * Set the name of the component that executed the task.
     *
     * @param name - Name of the component.
     */
    public void setComponentName(String name) {
        mComponentName = name;
    }

    /**
     * Set the result of the task executed by component.
     *
     * @param taskResult - result of task executed by component.
     */
    public void setTaskResult(String taskResult) {
        mTaskResult = taskResult;
    }

    /**
     * Set the exception object.
     *
     * @param exObj - exception object.
     */
    public void setExceptionObject(Throwable exObj) {
        mExceptionObject = exObj;
    }

    /**
     * Set the message type being returned by the component.
     *
     * @param statMsgType - type of success message that is  being returned by the
     *                    component.
     */
    public void setStatusMessageType(String statMsgType) {
        mStatusMsgType = statMsgType;
    }

    /**
     * Set the exception message type being returned by the component.
     *
     * @param exMsgType - type of exception message that is  being returned by the
     *                  component.
     */
    public void setExceptionMessageType(String exMsgType) {
        mExceptionMsgType = exMsgType;
    }

    /**
     * Set the message token for the exception being thrown by the component.
     *
     * @param nestingLevel - nesting level of the exception
     * @param locToken     - message token.
     */
    public void setLocToken(
            int nestingLevel,
            String locToken
    ) {
        mLocTokenCache.put(String.valueOf(nestingLevel), locToken);
    }

    /**
     * Set the message for the exception being thrown by the component.
     *
     * @param nestingLevel - nesting level of the exception.
     * @param locMessage   - exception message.
     */
    public void setLocMessage(
            int nestingLevel,
            String locMessage
    ) {
        mLocMessageCache.put(String.valueOf(nestingLevel), locMessage);
    }

    /**
     * Set the message parameters for the exception being thrown by the component.
     *
     * @param nestingLevel - nesting level of the exception.
     * @param locParam     - exception message parameters.
     */
    public void setLocParam(
            int nestingLevel,
            String[] locParam
    ) {
        mLocParamCache.put(String.valueOf(nestingLevel), locParam);
    }

    /**
     * Returns the message token.
     *
     * @param nestingLevel nesting level of the exception.
     * @return message token at the given nesting level.
     */
    public String getLocToken(int nestingLevel) {
        return (String) mLocTokenCache.get(String.valueOf(nestingLevel));
    }

    /**
     * Returns the exception message.
     *
     * @param nestingLevel nesting level of the exception
     * @return exception message at the given nesting level
     */
    public String getLocMessage(int nestingLevel) {
        return (String) mLocMessageCache.get(String.valueOf(nestingLevel));
    }

    /**
     * Returns the exception message parameters.
     *
     * @param nestingLevel nesting level of the exception.
     * @return exception message parameters at the given nesting level.
     */
    public String[] getLocParam(int nestingLevel) {
        return (String[]) mLocParamCache.get(String.valueOf(nestingLevel));
    }

    /**
     * Get the exception object being thrown by this component.
     *
     * @return Exception Object.
     */
    public Throwable getExceptionObject() {
        return mExceptionObject;
    }

    /**
     * Get the name of the task executed by this component.
     *
     * @return Task name.
     */
    public String getTaskName() {
        return mTaskName;
    }

    /**
     * Get the result of the task executed by this component.
     *
     * @return Task Result.
     */
    public String getTaskResult() {
        return mTaskResult;
    }

    /**
     * Get the type(status, exception) of message to be built for this component.
     *
     * @return Task Result.
     */
    public String getComponentMessageType() {
        return mMsgTypeToBuild;
    }

    /**
     * Get the status message type being returned by the component.
     *
     * @return Status message type.
     */
    public String getStatusMessageType() {
        return mStatusMsgType;
    }

    /**
     * Get the exception message type being returned by the component.
     *
     * @return Status message type.
     */
    public String getExceptionMessageType() {
        return mExceptionMsgType;
    }

    /**
     * Return the name of the component.
     *
     * @return component name.
     */
    public String getComponentName() {
        return mComponentName;
    }
}
