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

package com.sun.jdo.api.persistence.enhancer.util;

import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;


/**
 * FilePath provides general file path manipulation utilities.
 */

public class FilePath {
  private static String cwdAbsolute;

  /**
   * Return the absolute path for a file.  All directory separators are
   * converted to be File.separatorChar
   */
  public static String getAbsolutePath(File file) {
    /* VJ++ blows it here and doesn't use File.separatorChar when making
       a relative path absolute.  It uses '/' instead.  */

    String basicAbsolute = file.getAbsolutePath();
    if (file.separatorChar == '/')
      return basicAbsolute.replace('\\', '/');
    else
      return basicAbsolute.replace('/', '\\');
  }

  private static String getCwdAbsolute() {
    if (cwdAbsolute == null)
        cwdAbsolute = getAbsolutePath(new File("."));//NOI18N
    return cwdAbsolute;
  }

  /**
   * Attempt to produce a canonical path name for the specified file
   */
  public static String canonicalize(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException ioe) {
      /* JDK 1.1.4 gets an IOException if you pass it a UNC path name. */
    }

    /* Do it the hard way if getCanonicalPath fails.
     * This is far from perfect.
     * - It doesn't know about multiple mount points
     * - Doesn't deal with case differences.
     */
    String absolutePath = getAbsolutePath(file);
    Vector components = new Vector();
    StringTokenizer parser = 
      new StringTokenizer(absolutePath, File.separator, true);
    while (parser.hasMoreElements())
      components.addElement(parser.nextToken());

    boolean editted = true;
    while (editted) {
      editted = false;
      for (int i=1; i<components.size() && !editted; i++) {
	String s = (String)components.elementAt(i);
	if (s.equals(".")) {//NOI18N
	  components.removeElementAt(i);
	  components.removeElementAt(i-1);
	  editted = true;
	} else if (s.equals("..")) {//NOI18N
	  components.removeElementAt(i);
	  components.removeElementAt(i-1);
	  if (i > 2) {
	    if (!((String)components.elementAt(i-2)).equals(File.separator) &&
		((String)components.elementAt(i-3)).equals(File.separator)) {
	      components.removeElementAt(i-2);
	      components.removeElementAt(i-3);
	    }
	  }
	  editted = true;
	}
      }
    }

    /* Special case for Windows */
    String cwd = getCwdAbsolute();
    if (cwd.length() > 2 &&
	cwd.charAt(0) != File.separatorChar &&
	cwd.charAt(1) == ':') {
      /* probably a drive letter */
      if (((String)components.elementAt(0)).equals(File.separator) &&
	  (components.size() == 1 ||
	   !((String)components.elementAt(1)).equals(File.separator))) {
	String drive = cwd.substring(0,2);
	components.insertElementAt(drive, 0);
      }
    }

    /* Remove a trailing File.separatorChar */
    if (components.size() > 0 &&
	((String)components.elementAt(components.size()-1)).equals(
		File.separator))
      components.removeElementAt(components.size()-1);

    StringBuffer result = new StringBuffer();
    for (int j=0; j<components.size(); j++)
      result.append((String)components.elementAt(j));

    return result.toString();
  }

  /**
   * Compare two "canonical" file names for equivalence
   */
  public static boolean canonicalNamesEqual(String f1, String f2) {
    boolean equal;
    String cwd = getCwdAbsolute();
    if (cwd.length() > 2 &&
	cwd.charAt(0) != File.separatorChar &&
	cwd.charAt(1) == ':') {
      equal = f1.equalsIgnoreCase(f2);
    }
    else
      equal = f1.equals(f2);
    return equal;
  }

}

