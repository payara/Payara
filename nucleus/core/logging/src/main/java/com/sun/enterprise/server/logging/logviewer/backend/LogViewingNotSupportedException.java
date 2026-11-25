/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License ("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 */
package com.sun.enterprise.server.logging.logviewer.backend;

import com.sun.enterprise.server.logging.LogFacade;

import java.text.MessageFormat;

/**
 * Thrown when the logs of an instance cannot be viewed because its node type
 * is not supported (anything that is neither local nor SSH).
 */
public class LogViewingNotSupportedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String nodeType;

    public LogViewingNotSupportedException(String nodeType) {
        super(MessageFormat.format(LogFacade.LOGGING_LOGGER.getResourceBundle()
                .getString(LogFacade.LOG_VIEWING_NOT_SUPPORTED), nodeType));
        this.nodeType = nodeType;
    }

    public String getNodeType() {
        return nodeType;
    }
}
