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

package com.sun.enterprise.admin.cli;

public class StringEditDistance {
 
     /**
      * Computes the edit distance between two strings.
      *
      * <p>
      * The complexity is O(nm) where n=a.length() and m=b.length().
      */
     public static int editDistance(String a, String b) {
         return new StringEditDistance(a, b).calc();
     }
  
     /**
      * Finds the string in the <code>group</code> closest to
      * <code>key</code> and returns it.
      *
      * @return null if group.length==0.
      */
     public static String findNearest(String key, String[] group) {
         int c = Integer.MAX_VALUE;
         String r = null;
         for (int i = 0; i < group.length; i++) {
             int ed = editDistance(key, group[i]);
             if (c > ed) {
                 c = ed;
                 r = group[i];
             }
         }
         return r;
     }
 
 
     /** cost vector. */
     private int[] cost;
 
     /** back buffer. */
     private int[] back;
 
     /** Two strings to be compared. */
     private final String a,b;
  
     private StringEditDistance(String a, String b) {
         this.a = a;
         this.b = b;
         cost = new int[a.length() + 1];
         back = new int[a.length() + 1];
         // back buffer
         for (int i = 0; i <= a.length(); i++)
             cost[i] = i;
     }
 
     /**
      * Swaps two buffers.
      */
     private void flip() {
         int[] t = cost;
         cost = back;
         back = t;
     }
  
     private int min(int a, int b, int c) {
          return Math.min(a, Math.min(b, c));
     }
 
     private int calc() {
         for (int j = 0; j < b.length(); j++) {
             flip();
             cost[0] = j + 1;
             for (int i = 0; i < a.length(); i++) {
                 int match = (a.charAt(i) == b.charAt(j)) ? 0 : 1;
                 cost[i+1] = min(back[i] + match, cost[i] + 1, back[i+1] + 1);
             }
         }
         return cost[a.length()];
     }
}
