/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

#ifndef JAVA_MD_H
#define JAVA_MD_H

#include <windows.h>
#include <stdio.h>
#include <io.h>
#include <fcntl.h>

#define PATH_SEPARATOR_CHAR ';'
#define FILE_SEPARATOR_CHAR '\\'
#define PATH_SEPARATOR ";"
#define FILE_SEPARATOR "\\"
#define MAXPATHLEN MAX_PATH

#define JAVA_EXE FILE_SEPARATOR "bin" FILE_SEPARATOR "java.exe"
#define JAVAW_EXE FILE_SEPARATOR "bin" FILE_SEPARATOR "javaw.exe"
#define KEYTOOL_EXE FILE_SEPARATOR "bin" FILE_SEPARATOR "keytool.exe"

/*
 * Make this a Windows application by default. Defining a main function makes
 * the application run as a Console application.
 */
#define main _main
extern int _main(int argc, char **argv);

#define access _access
#define F_OK 0
#define W_OK 2
#define R_OK 4
#define X_OK 0

#define snprintf _snprintf

/*
 * Define a function for printing out status messages
 */
extern int statusf(const char* format, ...);

/* Utility functions for displaying the main status window */
extern HINSTANCE instance;
extern HWND statusdialog;

#endif

