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

package com.sun.jdo.api.persistence.enhancer.classfile;

import java.io.*;

/**
 * Class representing a class reference in the constant pool
 */

public class ConstClass extends ConstBasic {
  /* The tag associated with ConstClass entries */
  public static final int MyTag = CONSTANTClass;

  /* The name of the class being referred to */
  private ConstUtf8 theClassName;

  /* The index of name of the class being referred to
   *  - used while reading from a class file */
  private int theClassNameIndex;

  /* public accessors */

  /**
   * Return the tag for this constant
   */
  public int tag () { return MyTag; }

  /**
   * Return the class name
   */
  public ConstUtf8 className() {
    return theClassName;
  }

  /**
   * Return the class name in simple string form
   */
  public String asString() {
    return theClassName.asString();
  }

  /**
   * A printable representation 
   */
  public String toString () {
      return "CONSTANTClass(" + indexAsString() + "): " + //NOI18N
          "className(" + theClassName.toString() + ")";//NOI18N
  }

  /**
   * Change the class reference (not to be done lightly)
   */
  public void changeClass(ConstUtf8 newName) {
    theClassName = newName;
    theClassNameIndex = newName.getIndex();
  }

  /* package local methods */

  /**
   * Construct a ConstClass
   */
  public ConstClass (ConstUtf8 cname) {
    theClassName = cname;
  }

  ConstClass (int cname) {
    theClassNameIndex = cname;
  }

  void formatData (DataOutputStream b) throws IOException {
    b.writeShort(theClassName.getIndex());
  }

  static ConstClass read (DataInputStream input) throws IOException {
    return new ConstClass (input.readUnsignedShort());
  }

  void resolve (ConstantPool p) {
    theClassName = (ConstUtf8) p.constantAt(theClassNameIndex);
  }
}

