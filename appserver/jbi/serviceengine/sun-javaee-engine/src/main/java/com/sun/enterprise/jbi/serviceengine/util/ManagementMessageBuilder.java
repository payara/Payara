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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * BuildManagementMessageImpl provides utility methods to build messages (as XML string)
 * that are returned to the clients by the JBI Framework.
 *
 * @author bhavanishankar@dev.java.net.
 */
public class ManagementMessageBuilder {
    /**
     * String to represent UNKNOWN elements
     */
    private static final String MISSING_DATA_STRING = "UNKNOWN";

    /**
     * String to represent INVALID input
     */
    private static final String CANNOT_BUILD_MESSAGE_STRING =
            "CANNOT_BUILD_MESSAGE_INVALID_DATA";

    /**
     * Max. nesting level
     */
    private static final int MAX_NESTING_LEVEL = 32;


    /**
     * Creates a new <CODE>BuildManagementMessageImpl</CODE> instance.
     */
    public ManagementMessageBuilder() {
    }

    /**
     * Return an XML string of the task result(either status of exception).  This is the
     * task executed by the component
     *
     * @param cmObj Status details of the component
     * @return XML string with task status
     * @throws Exception If fails to build XML string
     */
    public String buildComponentMessage(ComponentMessageHolder cmObj)
            throws Exception {
        String mMsgTypeToBuild = null;
        String compTaskMsg = null;

        try {
            mMsgTypeToBuild = cmObj.getComponentMessageType();

            if (mMsgTypeToBuild.equalsIgnoreCase("STATUS_MSG")) {
                compTaskMsg = buildComponentTaskStatusMessage(cmObj);
            }

            if (mMsgTypeToBuild.equalsIgnoreCase("EXCEPTION_MSG")) {
                compTaskMsg = buildComponentTaskExceptionMessage(cmObj);
            }

            if (compTaskMsg == null) {
                compTaskMsg = CANNOT_BUILD_MESSAGE_STRING;
            }
        }
        catch (Exception e) {
            throw e;
        }

        return compTaskMsg;
    }

    /**
     * Return an XML string of the task status. This is the task executed by the
     * component
     *
     * @param mmObj Status details of the component
     * @return XML string with task status
     * @throws Exception If fails to build XML string
     */
    public String buildComponentTaskStatusMessage(ComponentMessageHolder mmObj)
            throws Exception {
        String cmpStatMsg = null;
        Document doc = null;
        String locToken = null;
        String locMessage = null;
        String locParam = null;

        try {
            String componentName = mmObj.getComponentName();

            if (componentName == null) {
                componentName = MISSING_DATA_STRING;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Element elem = doc.createElementNS(
                    "http://java.sun.com/xml/ns/jbi/management-message",
                    "component-task-result");
            Element compNameElem = doc.createElement("component-name");
            Element compTaskRsltDtlsElem =
                    doc.createElement("component-task-result-details");

            DOMUtil.UTIL.setTextData(compNameElem, componentName);

            Element taskRsltDtlsElem = buildStatusXMLFragment(doc, mmObj);
            compTaskRsltDtlsElem.appendChild(taskRsltDtlsElem);
            elem.appendChild(compNameElem);
            elem.appendChild(compTaskRsltDtlsElem);
            doc.appendChild(elem);

            StringWriter sw = new StringWriter();
            cmpStatMsg = DOMUtil.UTIL.DOM2String(doc, sw);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return cmpStatMsg;
    }

    /**
     * DOCUMENT ME!
     *
     * @param doc   DOCUMENT ME!
     * @param mmObj DOCUMENT ME!
     * @return DOCUMENT ME!
     * @throws Exception DOCUMENT ME!
     */
    private Element buildStatusXMLFragment(
            Document doc,
            ComponentMessageHolder mmObj
    ) throws Exception {
        String cmpStatMsg = null;
        String locToken = null;
        String locMessage = null;
        String[] locParam = null;
        Element taskRsltDtlsElem = null;

        try {
            String taskName = mmObj.getTaskName();

            if (taskName == null) {
                /* Note : Since this is a helper class which can
                 * be instantiated by the component, JBI resource
                 * bundle cannot be used. Hence strings are hard coded.
                */
                String errMsg =
                        "JBIMA0451: BuildManagementMessage : Task name cannot be null ";
                throw new Exception(errMsg);
            }

            String taskResult = mmObj.getTaskResult();

            if (taskResult == null) {
                /* Note : Since this is a helper class which can
                 * be instantiated by the component, JBI resource
                 * bundle cannot be used. Hence strings are hard coded.
                */
                String errMsg =
                        "JBIMA0456: BuildManagementMessage : Task result cannot be null ";
                throw new Exception(errMsg);
            }

            String messageType = mmObj.getStatusMessageType();

            if (messageType != null) {
                locToken = mmObj.getLocToken(1);

                if (locToken == null) {
                    locToken = MISSING_DATA_STRING;
                }

                locMessage = mmObj.getLocMessage(1);

                if (locMessage == null) {
                    locMessage = MISSING_DATA_STRING;
                }

                locParam = (String[]) mmObj.getLocParam(1);
            }

            Element[] msgLocInfoElem = new Element[MAX_NESTING_LEVEL];
            Element[] locTokenElem = new Element[MAX_NESTING_LEVEL];
            Element[] locMessageElem = new Element[MAX_NESTING_LEVEL];
            Element[] taskStatMsgElem = new Element[MAX_NESTING_LEVEL];
            taskRsltDtlsElem = doc.createElement("task-result-details");

            Element taskIdElem = doc.createElement("task-id");
            Element taskRsltStatElem = doc.createElement("task-result");
            Element msgTypeElem = doc.createElement("message-type");
            taskStatMsgElem[0] = doc.createElement("task-status-msg");
            msgLocInfoElem[0] = doc.createElement("msg-loc-info");
            locTokenElem[0] = doc.createElement("loc-token");
            locMessageElem[0] = doc.createElement("loc-message");

            DOMUtil.UTIL.setTextData(taskIdElem, taskName);
            DOMUtil.UTIL.setTextData(taskRsltStatElem, taskResult);
            taskRsltDtlsElem.appendChild(taskIdElem);
            taskRsltDtlsElem.appendChild(taskRsltStatElem);
            int j = 0;

            if (messageType != null) {
                DOMUtil.UTIL.setTextData(msgTypeElem, messageType);

                while ((locToken != null) && (locMessage != null)) {
                    DOMUtil.UTIL.setTextData(locTokenElem[j], locToken);
                    DOMUtil.UTIL.setTextData(locMessageElem[j], locMessage);
                    taskRsltDtlsElem.appendChild(msgTypeElem);
                    msgLocInfoElem[j].appendChild(locTokenElem[j]);
                    msgLocInfoElem[j].appendChild(locMessageElem[j]);

                    if (locParam != null) {
                        Element[] locParamElem = new Element[MAX_NESTING_LEVEL];

                        for (int k = 0; k < locParam.length; k++) {
                            locParamElem[k] = doc.createElement("loc-param");
                            DOMUtil.UTIL
                                    .setTextData(locParamElem[k], locParam[k]);
                            msgLocInfoElem[j].appendChild(locParamElem[k]);
                        }
                    }

                    taskStatMsgElem[j].appendChild(msgLocInfoElem[j]);
                    taskRsltDtlsElem.appendChild(taskStatMsgElem[j]);

                    j++;
                    locToken = mmObj.getLocToken(j + 1);
                    locMessage = mmObj.getLocMessage(j + 1);
                    locParam = mmObj.getLocParam(j + 1);
                    taskStatMsgElem[j] = doc.createElement("task-status-msg");
                    msgLocInfoElem[j] = doc.createElement("msg-loc-info");
                    locTokenElem[j] = doc.createElement("loc-token");
                    locMessageElem[j] = doc.createElement("loc-message");
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return taskRsltDtlsElem;
    }

    /**
     * DOCUMENT ME!
     *
     * @param doc   DOCUMENT ME!
     * @param mmObj DOCUMENT ME!
     * @return DOCUMENT ME!
     * @throws Exception DOCUMENT ME!
     */
    public Element buildExceptionXMLFragment(
            Document doc,
            ComponentMessageHolder mmObj
    ) throws Exception {
        String cmpStatMsg = null;
        String locToken = null;
        String locMessage = null;
        String[] locParam = null;
        Throwable exObj = null;
        Element taskRsltDtlsElem = null;

        try {
            String taskName = mmObj.getTaskName();

            if (taskName == null) {
                /* Note : Since this is a helper class which can
                 * be instantiated by the component, JBI resource
                 * bundle cannot be used. Hence strings are hard coded.
                */
                String errMsg =
                        "JBIMA0452: BuildManagementMessage : Task name cannot be null ";
                throw new Exception(errMsg);
            }

            String taskResult = mmObj.getTaskResult();

            if (taskResult == null) {
                /* Note : Since this is a helper class which can
                 * be instantiated by the component, JBI resource
                 * bundle cannot be used. Hence strings are hard coded.
                 *
                */
                String errMsg =
                        "JBIMA0453: BuildManagementMessage : Task result cannot be null ";
                throw new Exception(errMsg);
            }

            String messageType = mmObj.getExceptionMessageType();

            locToken = mmObj.getLocToken(1);

            if (locToken == null) {
                locToken = MISSING_DATA_STRING;
            }

            locMessage = mmObj.getLocMessage(1);

            locParam = mmObj.getLocParam(1);

            exObj = mmObj.getExceptionObject();

            taskRsltDtlsElem = doc.createElement("task-result-details");

            Element taskIdElem = doc.createElement("task-id");
            Element taskRsltStatElem = doc.createElement("task-result");
            Element msgTypeElem = doc.createElement("message-type");
            DOMUtil.UTIL.setTextData(taskIdElem, taskName);
            DOMUtil.UTIL.setTextData(taskRsltStatElem, taskResult);

            if (messageType != null) {
                DOMUtil.UTIL.setTextData(msgTypeElem, messageType);
                taskRsltDtlsElem.appendChild(msgTypeElem);
            }

            taskRsltDtlsElem.appendChild(taskIdElem);
            taskRsltDtlsElem.appendChild(taskRsltStatElem);

            if (messageType != null) {
                DOMUtil.UTIL.setTextData(msgTypeElem, messageType);
                taskRsltDtlsElem.appendChild(msgTypeElem);
            }

            if (exObj == null) {
                Element exInfoElem = doc.createElement("exception-info");
                Element msgLocInfoElem = doc.createElement("msg-loc-info");
                Element locTokenElem = doc.createElement("loc-token");
                Element locMessageElem = doc.createElement("loc-message");
                Element nestingLevelElem = doc.createElement("nesting-level");
                Element stckTraceElem = doc.createElement("stack-trace");

                DOMUtil.UTIL.setTextData(locTokenElem, locToken);

                if (locMessage != null) {
                    DOMUtil.UTIL.setTextData(locMessageElem, locMessage);
                } else {
                    DOMUtil.UTIL
                            .setTextData(locMessageElem, MISSING_DATA_STRING);
                }

                DOMUtil.UTIL.setTextData(nestingLevelElem, MISSING_DATA_STRING);
                DOMUtil.UTIL.setTextData(stckTraceElem, MISSING_DATA_STRING);

                msgLocInfoElem.appendChild(locTokenElem);
                msgLocInfoElem.appendChild(locMessageElem);

                if (locParam != null) {
                    Element[] locParamElem = new Element[MAX_NESTING_LEVEL];

                    for (int i = 0; i < locParam.length; i++) {
                        locParamElem[i] = doc.createElement("loc-param");
                        DOMUtil.UTIL.setTextData(locParamElem[i], locParam[i]);
                        msgLocInfoElem.appendChild(locParamElem[i]);
                    }
                }

                exInfoElem.appendChild(nestingLevelElem);
                exInfoElem.appendChild(msgLocInfoElem);
                exInfoElem.appendChild(stckTraceElem);
                taskRsltDtlsElem.appendChild(exInfoElem);
            } else {
                int nestingLevel = 1;
                Element[] exInfoElem = new Element[MAX_NESTING_LEVEL];
                Element[] msgLocInfoElem = new Element[MAX_NESTING_LEVEL];
                Element[] locTokenElem = new Element[MAX_NESTING_LEVEL];
                Element[] locMessageElem = new Element[MAX_NESTING_LEVEL];
                Element[] nestingLevelElem = new Element[MAX_NESTING_LEVEL];
                Element[] stckTraceElem = new Element[MAX_NESTING_LEVEL];

                while (exObj != null) {
                    exInfoElem[nestingLevel] =
                            doc.createElement("exception-info");
                    msgLocInfoElem[nestingLevel] =
                            doc.createElement("msg-loc-info");
                    locTokenElem[nestingLevel] = doc.createElement("loc-token");
                    locMessageElem[nestingLevel] =
                            doc.createElement("loc-message");
                    nestingLevelElem[nestingLevel] =
                            doc.createElement("nesting-level");
                    stckTraceElem[nestingLevel] =
                            doc.createElement("stack-trace");

                    StackTraceElement[] stckTrElem = exObj.getStackTrace();
                    StringBuffer sb = new StringBuffer("");

                    if (stckTrElem != null) {
                        for (int i = 0; i < stckTrElem.length; i++) {
                            String stckTrace = stckTrElem[i].toString();
                            sb.append(stckTrace);
                            sb.append("\n");
                        }
                    }

                    if (nestingLevel == 1) {
                        DOMUtil.UTIL
                                .setTextData(locTokenElem[nestingLevel],
                                        locToken);

                        if (locMessage == null) {
                            DOMUtil.UTIL.setTextData(
                                    locMessageElem[nestingLevel],
                                    exObj.getMessage()
                            );
                        } else {
                            DOMUtil.UTIL.setTextData(
                                    locMessageElem[nestingLevel],
                                    locMessage
                            );
                        }
                    } else {
                        String errMsgWithToken = exObj.getMessage();
                        String token = errMsgWithToken.substring(0, 9);
                        String errMsg = null;

                        if (token.startsWith("JBI")) {
                            errMsg = errMsgWithToken.substring(11);
                        } else {
                            token = MISSING_DATA_STRING;
                            errMsg = errMsgWithToken;
                        }

                        DOMUtil.UTIL
                                .setTextData(locTokenElem[nestingLevel], token);
                        DOMUtil.UTIL
                                .setTextData(locMessageElem[nestingLevel],
                                        errMsg);

                    }

                    DOMUtil.UTIL.setTextData(
                            nestingLevelElem[nestingLevel],
                            Integer.toString(nestingLevel)
                    );
                    DOMUtil.UTIL
                            .setTextData(stckTraceElem[nestingLevel],
                                    sb.toString());

                    msgLocInfoElem[nestingLevel].appendChild(locTokenElem[nestingLevel]);
                    msgLocInfoElem[nestingLevel].appendChild(
                            locMessageElem[nestingLevel]
                    );

                    if (nestingLevel == 1) {
                        if (locParam != null) {
                            Element[] locParamElem =
                                    new Element[MAX_NESTING_LEVEL];

                            for (int i = 0; i < locParam.length; i++) {
                                locParamElem[i] =
                                        doc.createElement("loc-param");
                                DOMUtil.UTIL
                                        .setTextData(locParamElem[i],
                                                locParam[i]);
                                msgLocInfoElem[nestingLevel].appendChild(
                                        locParamElem[i]);
                            }
                        }
                    } else {
                        Element locParamElement =
                                doc.createElement("loc-param");
                        DOMUtil.UTIL
                                .setTextData(locParamElement,
                                        MISSING_DATA_STRING);
                        msgLocInfoElem[nestingLevel].appendChild(locParamElement);
                    }

                    exInfoElem[nestingLevel].appendChild(nestingLevelElem[nestingLevel]);
                    exInfoElem[nestingLevel].appendChild(msgLocInfoElem[nestingLevel]);
                    exInfoElem[nestingLevel].appendChild(stckTraceElem[nestingLevel]);
                    taskRsltDtlsElem.appendChild(exInfoElem[nestingLevel]);

                    nestingLevel++;
                    exObj = exObj.getCause();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return taskRsltDtlsElem;
    }

    /**
     * Constructs an XML string of the component exception.
     *
     * @param mmObj HashMap containing component exception information
     * @return XML string representing component exception
     * @throws Exception If fails to build component exception message
     */
    public String buildComponentTaskExceptionMessage(ComponentMessageHolder mmObj)
            throws Exception {
        String cmpStatMsg = null;
        Document doc = null;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Element elem = doc.createElementNS(
                    "http://java.sun.com/xml/ns/jbi/management-message",
                    "component-task-result");
            Element compNameElem = doc.createElement("component-name");
            Element compTaskRsltDtlsElem =
                    doc.createElement("component-task-result-details");

            String componentName = mmObj.getComponentName();

            if (componentName == null) {
                String errMsg =
                        "JBIMA0457: BuildManagementMessage : Component name cannot be null ";
                throw new Exception(errMsg);
            }

            DOMUtil.UTIL.setTextData(compNameElem, componentName);

            Element taskRsltDtlsElem = buildExceptionXMLFragment(doc, mmObj);

            compTaskRsltDtlsElem.appendChild(taskRsltDtlsElem);
            elem.appendChild(compNameElem);
            elem.appendChild(compTaskRsltDtlsElem);
            doc.appendChild(elem);

            StringWriter sw = new StringWriter();
            cmpStatMsg = DOMUtil.UTIL.DOM2String(doc, sw);
        }
        catch (Exception e) {
            throw e;
        }

        return cmpStatMsg;
    }

}
