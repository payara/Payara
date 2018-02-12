/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.tools.classmodel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jvnet.hk2.component.MultiMap;

/**
 * Utilities
 * 
 * @author Jeff Trent
 */
public class Utilities {

  /**
   * Sorts all of the lines in an inhabitants descriptor
   * 
   * @param in the input string
   * @param innerSort true if each line in the inhabitants file is sorted as well
   * @return the sorted output string
   */
  public static String sortInhabitantsDescriptor(String in, boolean innerSort) {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(in.getBytes())));
    String line;
    try {
      while (null != (line = reader.readLine())) {
        if (!line.startsWith("#") && !line.isEmpty()) {
          lines.add(innerSort ? innerSort(line) : line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Collections.sort(lines);

    StringBuilder sb = new StringBuilder();
    for (String oline : lines) {
      sb.append(oline).append("\n");
    }
    return sb.toString();
  }

  static String innerSort(String line) {
    MultiMap<String, String> mm = split(line);
    StringBuilder sb = new StringBuilder();

    // class
    List<String> vals = mm.remove("class");
    assert(null != vals && 1 == vals.size());
    sb.append("class=").append(vals.iterator().next());
    
    // indicies
    vals = mm.remove("index");
    if (null != vals && vals.size() > 0) {
      Collections.sort(vals);
      for (String index : vals) {
        sb.append(",index=").append(index);
      }
    }
    
    // metadata
    vals = new ArrayList<String>(mm.keySet());
    Collections.sort(vals);
    for (String key : vals) {
      List<String> subVals = new ArrayList<String>(mm.get(key));
      Collections.sort(subVals);
      for (String val : subVals) {
        sb.append(",").append(key).append("=").append(val);
      }
    }

    return sb.toString();
  }

  static MultiMap<String, String> split(String value) {
    MultiMap<String, String> result = new MultiMap<String, String>();
    String split[] = value.split(",");
    for (String s : split) {
      String split2[] = s.split("=");
      assert(2 == split2.length);
      result.add(split2[0], split2[1]);
    }
    return result;
  }
}
