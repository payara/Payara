/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package org.glassfish.jdbc.util;

import java.util.logging.Logger;

import com.sun.logging.LogDomains;

/**
 * Always use this factory to generate loggers in this library. The reason is
 * that we use classloader from another library to load correct resource bundle
 * - we use messages from there.
 *
 * @author David Matějček
 */
public class LoggerFactory {

    /**
     * @param clazz
     * @return logger using resource bundle same as the connectors-runtime.jar
     */
    public static Logger getLogger(final Class clazz) {
        return LogDomains.getLogger(clazz, LogDomains.RSR_LOGGER, clazz.getClassLoader());
    }
}
