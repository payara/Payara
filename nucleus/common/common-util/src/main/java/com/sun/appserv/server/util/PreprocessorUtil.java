/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.server.util;

import com.sun.enterprise.util.CULoggerInfo;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.*;

/**
 * PreprocessorUtil is a utility class for managing the bytecode
 * preprocessor(s). The list of preprocessors are passed in as a string array
 * to the initialize method.  If there is a problem initialize any of the
 * preprocessors, all preprocessing is disabled.
 */
public class PreprocessorUtil {

    private static boolean _preprocessorEnabled = false;
    private static BytecodePreprocessor[] _preprocessor;

    /**
     * Initializes the preprocessor utility with the associated class names
     * array arugment.
     * @param ppClassNames - the String array of preprocessor class names.
     * @return - true if successful, otherwise false.  All preprocessors must
     * successfully initialize for true to be returned.
     */
    public static boolean init (String[] ppClassNames) {
        if (ppClassNames != null) {
            setupPreprocessor(ppClassNames);
        }
        return _preprocessorEnabled;
    }

    /**
     * Processes a class through the preprocessor.
     * @param className - the class name.
     * @param classBytes - the class byte array.
     * @return - the processed class byte array.
     */
    public static byte[] processClass (String className, byte[] classBytes) {
        Logger _logger = CULoggerInfo.getLogger();
        byte[] goodBytes = classBytes;
        if (_preprocessorEnabled) {
            if (_preprocessor != null) {
                // Loop through all of the defined preprocessors...
                for (int i=0; i < _preprocessor.length; i++) {
                    classBytes =
                        _preprocessor[i].preprocess(className, classBytes);
                    _logger.log(Level.FINE,
                            "[PreprocessorUtil.processClass] Preprocessor {0} Processed Class: {1}",
                            new Object[]{i, className});
                    // Verify the preprocessor returned some bytes
                    if (classBytes != null){
                        goodBytes = classBytes;
                    }
                    else{
                        _logger.log(Level.SEVERE, CULoggerInfo.preprocessFailed,
                            new String[] {className,
                                          _preprocessor[i].getClass().getName()});

                        // If were on the 1st preprocessor
                        if (i == 0){
                            _logger.log(Level.SEVERE, CULoggerInfo.resettingOriginal,
                                className);
                        }
                        // We're on the 2nd or nth preprocessor.
                        else {
                            _logger.log(Level.SEVERE, CULoggerInfo.resettingLastGood,
                                className);
                        }
                    }
                }
            }
        }
        return goodBytes;
    }

    private synchronized static void setupPreprocessor(String[] ppClassNames) {
        Logger _logger = CULoggerInfo.getLogger();

        if (_preprocessor != null) {
            // The preprocessors have already been set up.
            return;
        }

        try {
            _preprocessor = new BytecodePreprocessor[ppClassNames.length];
            for (int i = 0; i < ppClassNames.length; i++) {
                String ppClassName = ppClassNames[i].trim();
                Class ppClass = Class.forName(ppClassName);
                if (ppClass != null){
                    _preprocessor[i] = (BytecodePreprocessor)
                                                        ppClass.newInstance();
                        _preprocessorEnabled = true;
                }
                if (_preprocessor[i] != null){
                    if (!_preprocessor[i].initialize(new Hashtable())) {
                        _logger.log(Level.SEVERE, CULoggerInfo.failedInit,
                            ppClassName);
                        _logger.log(Level.SEVERE, CULoggerInfo.disabled);
                        _preprocessorEnabled = false;
                    }
                } else {
                    _logger.log(Level.SEVERE, CULoggerInfo.failedInit,
                        ppClassName);
                    _logger.log(Level.SEVERE, CULoggerInfo.disabled);
                    _preprocessorEnabled = false;
                }
            }
        } catch (Throwable t) {
            _logger.log(Level.SEVERE, CULoggerInfo.setupEx, t);
            _logger.log(Level.SEVERE, CULoggerInfo.disabled);
            _preprocessorEnabled = false;
        }
    }

    /**
     * Indicates whether or not the preprocessor is enabled
     * @return - true of the preprocessor is enabled, otherwise false.
     */
    public static boolean isPreprocessorEnabled() {
        return _preprocessorEnabled;
    }
}
