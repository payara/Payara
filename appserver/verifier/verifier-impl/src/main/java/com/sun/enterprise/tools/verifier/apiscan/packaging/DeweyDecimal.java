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

package com.sun.enterprise.tools.verifier.apiscan.packaging;

class DeweyDecimal {

    private int major = 0, minor = 0, micro = 0;

    public DeweyDecimal() {
    }

    public DeweyDecimal(String s) {
        s = s.trim();
        int idxOfFirstDot = s.indexOf('.', 0);
        if (idxOfFirstDot == -1) {
            major = Integer.parseInt(s);
            return;
        } else {
            major = Integer.parseInt(s.substring(0, idxOfFirstDot));
        }
        int idxOfSecondDot = s.indexOf('.', idxOfFirstDot + 1);
        if (idxOfSecondDot == -1) {
            minor = Integer.parseInt(s.substring(idxOfFirstDot + 1));
            return;
        } else {
            minor =
                    Integer.parseInt(
                            s.substring(idxOfFirstDot + 1, idxOfSecondDot));
        }
        micro = Integer.parseInt(s.substring(idxOfSecondDot + 1));
    }

    public boolean isCompatible(DeweyDecimal another) {
        if (another == null) return false;
        if (major < another.major) {
            return false;
        } else if (major == another.major) {
            if (minor < another.minor) {
                return false;
            } else if (minor == another.minor) {
                return micro >= another.micro;
            }
            //this.minor> another.minor && this.major==another.major, hence return true
            return true;
        }
        //this.major> another.major, hence return true
        return true;
    }

    public boolean isCompatible(String another) {
        if (another == null) return false;
        return isCompatible(new DeweyDecimal(another));
    }

    //provides value semantics, hence we should overrise hashCode and equals method.
    public int hashCode() {
        return major + minor + micro;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        try {
            DeweyDecimal other = (DeweyDecimal) o;
            return major == other.major && minor == other.minor &&
                    micro == other.micro;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "" + major + "." + minor + "." + micro; // NOI18N
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(
                    "Usage: " + DeweyDecimal.class.getName() + // NOI18N
                    " <s1 in the format 1.2.3> <s2 in the format 5.5.6>"); // NOI18N
            System.exit(1);
        }
        DeweyDecimal d1 = new DeweyDecimal(args[0]);
        DeweyDecimal d2 = new DeweyDecimal(args[1]);
        System.out.println(d1 + ".isCompatible(" + d1 + ")=" + d1.isCompatible( // NOI18N
                d1));
        System.out.println(d2 + ".isCompatible(" + d2 + ")=" + d2.isCompatible( // NOI18N
                d2));
        System.out.println(d1 + ".isCompatible(" + d2 + ")=" + d1.isCompatible( // NOI18N
                d2));
        System.out.println(d2 + ".isCompatible(" + d1 + ")=" + d2.isCompatible( // NOI18N
                d1));
        System.out.println(d1 + ".equals(" + d1 + ")=" + d1.equals(d1)); // NOI18N
        System.out.println(d2 + ".equals(" + d2 + ")=" + d2.equals(d2)); // NOI18N
        System.out.println(d1 + ".equals(" + d2 + ")=" + d1.equals(d2)); // NOI18N
        System.out.println(d2 + ".equals(" + d1 + ")=" + d2.equals(d1)); // NOI18N
        System.out.println(d1 + ".hashCode()=" + d1.hashCode()); // NOI18N
        System.out.println(d2 + ".hashCode()=" + d2.hashCode()); // NOI18N
    }
}
