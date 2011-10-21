/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.os;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Abstraction for local/remote file operations on a machine. Operations
 * on this interface are implemented by a machine which can be local
 * or remote.
 *
 * @author Jerome Dochez
 *
 */
public interface FileOperations {

    /**
     * returns true if the path exists on the machine
     * @param path path to a file or directory to check for existence
     * @return  true if the path exists
     * @throws IOException if the path cannot be checked.
     */
    boolean exists(String path) throws IOException;

    /**
     * Returns the Date of the last modification of the file.
     * @param path file path on the machine
     * @return the last modification time of that file
     * @throws IOException if the operation fialed.
     */
    Date mod(String path) throws IOException;

    /**
     *  mkdir on the target machine
     * @param destPath  path to the directory to create
     * @return  true if the mkdir opertation happened and was successful
     * @throws IOException  if the operation failed
     */
    boolean mkdir(String destPath) throws IOException;

    /**
     * deletes a file or a directory on the target machine
     * @param path  path to the file to delete
     * @return true of the file existed and was deleted successfully
     * @throws IOException if the operation failed
     */
    boolean delete(String path) throws IOException;

    /**
     * move a file within the file system
     * @param source the source path for the file
     * @param dest the destination path to move the file to
     * @return true if the operation succeeded.
     * @throws IOException if the operation failed
     */
    boolean mv(String source, String dest) throws IOException;

    /**
     * copy the file from the local machine  to the remote machine represented
     * by this instance
     * @param source  the source file path
     * @param destDir  the destination directory to copy the file to
     * @throws IOException if the operation failed
     */
    void copy(File source, File destDir) throws IOException;

    /**
     * copy the file on a remote machine from a source path to a destination path.
     *
     * @param source the source path.
     * @param destDir  the destination directory to copy the file to
     * @throws IOException if the operation failed
     */
    void localCopy(String source, String destDir) throws IOException;

    /**
     * Returns a file size
     * @param path path to the file
     * @return the file size
     * @throws IOException if the file is not found
     */
    long length(String path) throws IOException;

    /**
     * Returns a list of file names in the passed directory
     * @param directory path
     * @return the list of files in that directory
     */
    List<String> ls(String path) throws IOException;
}
