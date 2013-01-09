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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;

/**
 * Creates {@link Reader} and {@link Writer} for the String substitution file.
 * Implementation is useful for large files which cann't be read entirely in a
 *  memory or need a substantial amount of memory.
 * <p>
 * To perform substitution it take helps of temporary file to write output, after
 * substitution, temporary file renamed to input file.
 * <p> 
 */
public class LargeFileSubstitutionHandler extends FileSubstitutionHandler {
    private static final String BACKUP_FILE_PREFIX = ".bkp";
    private static final String TEMP_FILE_PREFIX = ".tmp";
    private File _outputFile;

    public LargeFileSubstitutionHandler(File file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public Reader getReader() {
        try {
            _reader = new BufferedReader(new InputStreamReader(new FileInputStream(_inputFile)));
        } catch (FileNotFoundException e) {
            _logger.log(Level.INFO, _strings.get("invalidFileLocation", _inputFile.getAbsolutePath()) 
                    , e);
        }
        return _reader;
    }

    @Override
    public Writer getWriter() {
        _outputFile = new File(_inputFile.getAbsolutePath() + TEMP_FILE_PREFIX);
        try {
            if (!_outputFile.exists()) {
                if (!_outputFile.createNewFile()) {
                    throw new IOException();
                }
            }
            _writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_outputFile)));
        } catch (IOException e) {
            _logger.log(Level.INFO, _strings.get("failureTempFileCreation",
                    _outputFile.getAbsolutePath(), e));
        }
        return _writer;
    }

    @Override
    public void finish() {
        super.finish();
        String inputFileName = _inputFile.getName();
        File inputBackUpfile = new File(_inputFile.getAbsolutePath() + BACKUP_FILE_PREFIX);
        if (_inputFile.renameTo(inputBackUpfile)) {
            if (_outputFile.renameTo(new File(_inputFile.getAbsolutePath()))) {
                if (!inputBackUpfile.delete()) {
                    _logger.log(Level.INFO, _strings.get("failureInBackUpFileDeletion", 
                            inputBackUpfile.getAbsolutePath()));
                }
            } else {
                _logger.log(Level.INFO, _strings.get("failureInFileRename", _outputFile.getAbsolutePath(),
                        inputFileName));
            }
        } else {
            _logger.log(Level.WARNING,  _strings.get("failureInFileRename", _inputFile.getAbsolutePath(),
                   inputBackUpfile.getName()));
        }
    }
}