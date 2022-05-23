/*
 * Copyright (c) 2007, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.ts.lib.harness;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

public class ExcludeListProcessor {

  // pass in a string which has the filename#testname
  public static boolean isTestExcluded(String fileName) {
    // check to see if it exists in the exclude list
    return fileNameList.contains(fileName);
  }

  public static void readExcludeList(String fileName) {
    BufferedReader d = null;
    try {
      d = new BufferedReader(new FileReader(fileName));
      String line;
      while ((line = d.readLine()) != null) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("#")) {
          String entry = new String(line);
          fileNameList.addElement(entry.trim());
        }
      }
      d.close();
    } catch (FileNotFoundException e) {
      System.out.println(e.toString());
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println(e.toString());
      e.printStackTrace();
    }
  }

  /*----------- Private Members of this class -------------*/
  private static Vector fileNameList = new Vector();

}
