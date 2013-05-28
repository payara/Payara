/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.ChangePairRef;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Group;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.ModeType;

/**
 * This class provides method to process {@link ModeType}.
 * The ModeType is predefined set of values use to alter the
 * substitution containing forward or backward slash in the
 * after value. This attribute can be applied to {@link Group}
 * or {@link ChangePairRef}.
 *
 * @see ModeType
 * @see Group
 */
public class ModeProcessor {

    private static final Logger _logger = SLogger.getLogger();
    /**
     * Process the {@link ModeType} for a given string.
     * <li>
     * {@link ModeType#FORWARD} : Replaces all backward slashes to forward slash.
     * </li>
     * <li>
     * {@link ModeType#DOUBLE} : Append a slash to all backward slash and also add
     *    a backward slash before each colon.
     * </li>
     * <li>
     * {@link ModeType#POLICY} : Replaces {@link File#separator} by ${/} for java
     *   policy files.
     * </li>
     *
     * @param modeType The mode type to be applied on the given input string. 
     * @param input Input string for mode processing.
     * @return Processed string
     */
    static String processModeType(ModeType modeType, String input) {
        if (modeType == null || input == null || input.isEmpty()) {
            return input;
        }
        switch (modeType) {
            case FORWARD:
                // Change all backward slashes to forward slash.
                input = input.replace("\\", "/");
                break;
            case DOUBLE:
                // Add a slash to all back slashes.
                input = input.replace("\\", "\\\\");
                // Add a backslash before each colon.
                input = input.replace(":", "\\:");
                break;
            case POLICY:
                // Replace File.separator by ${/} for java.policy files
                input = input.replace(File.separator, "${/}");
                break;
            default:
                _logger.log(Level.WARNING, SLogger.NO_PROCESSOR_DEFINED, modeType.toString());
                break;
        }
        return input;
    }
}
