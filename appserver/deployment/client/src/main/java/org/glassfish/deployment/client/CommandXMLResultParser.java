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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]
package org.glassfish.deployment.client;

import com.sun.enterprise.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author tjquinn
 * @author David Matejcek null-safe, logging
 */
public class CommandXMLResultParser {

    private static final Logger LOG = Logger.getLogger(CommandXMLResultParser.class.getName());

    static DFDeploymentStatus parse(ByteArrayOutputStream response)
        throws ParserConfigurationException, SAXException, IOException {
        if (response == null || response.size() == 0) {
            throw new IOException("Cannot parse an empty response.");
        }
        SAXParserFactory pf = SAXParserFactory.newInstance();
        pf.setValidating(true);
        pf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        SAXParser parser = pf.newSAXParser();
        ResultHandler rh = new ResultHandler();
        try (ByteArrayInputStream is = new ByteArrayInputStream(response.toByteArray())) {
            parser.parse(is, rh);
        }
        return rh.getTopStatus();
    }

    private static DFDeploymentStatus.Status exitCodeToStatus(String exitCodeText) {
        return DFDeploymentStatus.Status.valueOf(exitCodeText);
    }

    private static class ResultHandler extends DefaultHandler {

        private DFDeploymentStatus topStatus;

        /** currentLevel will always point to the depl status we are currently working on */
        private DFDeploymentStatus currentLevel;

        private String attrToText(Attributes attrs, String attrName) {
            return attrs.getValue(attrName);
        }

        private DFDeploymentStatus getTopStatus() {
            return topStatus;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            LOG.finest(
                () -> String.format("startElement(uri=%s, localName=%s, qName=%s, attributes)", uri, localName, qName));
            if (qName.equals("action-report")) {
                /*
                 * If this is the first action-report then the resulting
                 * DFDeploymentStatus will be the top-level one as well as the
                 * current-level one.
                 */
                if (topStatus == null) {
                    currentLevel = topStatus = new DFDeploymentStatus();
                } else {
                    /*
                     * This is a nested action-report, so add it as a sub-stage
                     * to the current level DFDeploymentStatus.
                     */
                    addLevel();
                }
                currentLevel.setStageStatus(exitCodeToStatus(attrToText(attributes, "exit-code")));
                currentLevel.setStageDescription(attrToText(attributes, "description"));
                String failureCause = attrToText(attributes, "failure-cause");
                if (failureCause != null) {
                    currentLevel.setStageStatusMessage(failureCause);
                }
            } else if (qName.equals("message-part")) {
                /*
                 * The "message" attribute may not be present if the operation succeeded.
                 */
                addLevel();
                String msg = attrToText(attributes, "message");
                if (msg != null) {
                    String origMsg = currentLevel.getStageStatusMessage();
                    msg = currentLevel.getStageStatusMessage() + (StringUtils.ok(origMsg) ? " " : "") + msg;
                    currentLevel.setStageStatusMessage(msg);
                }
            } else if (qName.equals("property")) {
                currentLevel.addProperty(attrToText(attributes, "name"), attrToText(attributes, "value"));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            LOG.finest(() -> String.format("endElement(uri=%s, localName=%s, qName=%s)", uri, localName, qName));
            if (qName.equals("action-report")) {
                popLevel();
            } else if (qName.equals("message-part")) {
                popLevel();
            }
        }

        private void addLevel() {
            DFDeploymentStatus newLevel = new DFDeploymentStatus();
            currentLevel.addSubStage(newLevel);
            currentLevel = newLevel;
        }

        private void popLevel() {
            currentLevel = currentLevel.getParent();
        }
    }
}
