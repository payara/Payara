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
package org.glassfish.installer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openinstaller.util.ClassUtils;

/**
 * Utility class to perform file I/O operations such as read/write/update.
 * Also used to do Strings related operations in file such as find and replace.
 * @author sathyan
 */
public class FileIOUtils {

    /**
     * File object manipulated by this class.
     */
    protected File inputFile;
    /**
     * Buffer where all string operations related to file contents are done.
     * Mirrors the contents of the file.
     */
    protected ArrayList contentBuffer;
    /**
     * Total number of lines in the buffer.
     * Used to track insertion, deletion based on line numbers.
     */
    protected int totalLines;

    /* LOGGING */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    /**
     * Creates a new instance of FileIOUtils.
     */
    public FileIOUtils() {
        this.contentBuffer = new ArrayList();
        this.totalLines = 0;
    }

    /**
     * Sets up file for manipulation, If the file exists, then the contents are read
     * into array list.
     * @param fileName text file to manipulate.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void openFile(String fileName)
            throws IOException, FileNotFoundException {
        this.contentBuffer.clear();
        this.totalLines = 0;
        inputFile = new File(fileName);
        if (inputFile.exists()) {
            readFile();
        }
    }

    /**
     * Read the contents of the file using BufferedReader and store them in ArrayList.
     */
    private void readFile()
            throws IOException, FileNotFoundException {
        BufferedReader filePointer =
                new BufferedReader(new FileReader(inputFile));
        String eachLine = null;
        eachLine = filePointer.readLine();
        totalLines = 0;
        while (eachLine != null) {
            contentBuffer.add(eachLine);
            eachLine = filePointer.readLine();
        }
        totalLines = contentBuffer.size();
        filePointer.close();
    }

    /**
     * Find and replace all occurences of given string with the given value.
     * @param token Token to replace.
     * @param newValue replacement string.
     */
    public void findandReplaceAll(String token, String newValue) {
        for (int loopVar = 0; loopVar < totalLines; loopVar++) {
            contentBuffer.remove(loopVar);
            contentBuffer.add(loopVar,
                    StringUtils.substString((StringBuffer) contentBuffer.get(loopVar),
                    token, newValue));

        }

    }

    /**
     * Save the buffer content into the currently opened file. Functionality
     * equal to regular file "Save" operation.
     * @throws IOException
     */
    public void saveFile()
            throws IOException {
        BufferedWriter filePointer = new BufferedWriter(new FileWriter(inputFile));
        for (int loopVar = 0; loopVar < totalLines; loopVar++) {
            String temp = (String) contentBuffer.get(loopVar);
            LOGGER.log(Level.FINEST, temp);
            filePointer.write(temp);
            filePointer.newLine();
        }
        filePointer.close();
    }

    /**
     * Save the buffer content into the given file. Functionality
     * equal to regular file "Save As" operation. Useful for copying,
     * updating templates and converting them to actual files.
     * @param outputFile target file to be used in save operation.
     * @throws IOException
     */
    public void saveFileAs(String outputFile)
            throws IOException {
        BufferedWriter filePointer = new BufferedWriter(new FileWriter(outputFile));
        for (int loopVar = 0; loopVar < totalLines; loopVar++) {
            String temp = (String) contentBuffer.get(loopVar);
            LOGGER.log(Level.FINEST, temp);
            filePointer.write(temp);
            filePointer.newLine();
        }
        filePointer.close();
    }

    /**
     * Close the file, clear buffer, reset number of lines counter to 0.
     */
    public void closeFile() {
        this.contentBuffer.clear();
        this.totalLines = 0;
        this.inputFile = null;
    }

    /**
     * Case sensitive string search(only the first occurence) in the file.
     * @param searchString string to be searched in the file.
     * @return -1 if the given string not found, line number in the file otherwise.
     */
    public int findfirstStrcmp(String searchString) {
        int matchingLineNumber = -1;
        LOGGER.log(Level.FINEST, searchString);
        for (int loopVar = 0; loopVar < totalLines; loopVar++) {
            String str = (String) contentBuffer.get(loopVar);
            LOGGER.log(Level.FINEST, str);
            if (str.equals(searchString)) {
                matchingLineNumber = loopVar;
                break;
            }
        }
        return matchingLineNumber;
    }

    /** Case insensitive string search(only the first occurence) in the file.
     * * @param searchString string to be searched in the file.
     * @param searchString
     * @return -1 if the given string not found, line number in the file otherwise.
     */
    public int findfirstSubStrcmp(String searchString) {
        int matchingLineNumber = -1;
        LOGGER.log(Level.FINEST, searchString);
        for (int loopVar = 0; loopVar < totalLines; loopVar++) {
            String str = (String) contentBuffer.get(loopVar);
            LOGGER.log(Level.FINEST, str);
            if (str.indexOf(searchString) != -1) {
                matchingLineNumber = loopVar;
            }
        }
        return matchingLineNumber;
    }

    /**
     * Returns the whole line from the file, given the line number.
     * @param lineIndex line number in the file.
     * @return String found in the specified line number in file, null in case of errors.
     */
    public String getLine(int lineIndex) {
        return (String) contentBuffer.get(lineIndex);
    }

    /**
     * Deletes the whole line from the file, given the line number.
     * @param lineIndex line number in the file to remove.
     * @return true if successful, false otherwise.
     */
    public boolean removeLine(int lineIndex) {
        if (contentBuffer.remove(lineIndex) != null) {
            totalLines--;
            return true;
        }
        return false;
    }

    /**
     * Insert a whole line into the file, given the line number.
     * @param lineIndex line number after which to insert the string.
     * @param newLine content to be inserted.
     *
     */
    public void insertLine(int lineIndex, String newLine) {
        contentBuffer.add(lineIndex, newLine);
        totalLines++;
    }

    /**
     * Replace a line of text from the file, given the line number.
     * @param lineIndex line number.
     * @param newLine content to be replaced.
     * @return replaced string if successful, null otherwise.
     */
    public String replaceLine(int lineIndex, String newLine) {
        String retVal = (String) contentBuffer.remove(lineIndex);
        if (retVal != null) {
            contentBuffer.add(lineIndex, newLine);
        }
        return retVal;
    }

    /**
     * Add a line to the end of file.
     * @param newLine String to be added.
     * @return true if successful, false otherwise.
     */
    public boolean appendLine(String newLine) {
        if (contentBuffer.add(newLine)) {
            totalLines++;
            return true;
        }
        return false;
    }
}

