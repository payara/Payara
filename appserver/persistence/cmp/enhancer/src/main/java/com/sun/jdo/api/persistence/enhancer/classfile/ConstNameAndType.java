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
 * Class representing a name and an associated type in the constant pool
 * of a class file
 */

public class ConstNameAndType extends ConstBasic {
  /* The tag value associated with ConstDouble */
  public static final int MyTag = CONSTANTNameAndType;

  /* The name of interest */
  private ConstUtf8 theName;

  /* The index of the name to be resolved
   *   - used during class file reading */
  private int theNameIndex;

  /* The type signature associated with the name */
  private ConstUtf8 typeSignature;

  /* The index of the signature to be resolved
   *   - used during class file reading */
  private int typeSignatureIndex;

  /* public accessors */

  /**
   * The tag of this constant entry
   */
  public int tag () { return MyTag; }

  /**
   * Return the name
   */
  public ConstUtf8 name() {
    return theName;
  }

  /**
   * Return the type signature associated with the name
   */
  public ConstUtf8 signature() {
    return typeSignature;
  }

  /**
   * Modify the signature
   */
  public void changeSignature(ConstUtf8 newSig) {
    typeSignature = newSig;
  }

  /**
   * A printable representation
   */
  public String toString () {
      return "CONSTANTNameAndType(" + indexAsString() + "): " + //NOI18N
          "name(" + theName.toString() + ") " +//NOI18N
          " type(" + typeSignature.toString() + ")";//NOI18N
  }

  /* package local methods */

  ConstNameAndType (ConstUtf8 n, ConstUtf8 sig) {
    theName = n; typeSignature = sig;
  }

  ConstNameAndType (int n, int sig) {
    theNameIndex = n; typeSignatureIndex = sig;
  }

  void formatData (DataOutputStream b) throws IOException {
    b.writeShort(theName.getIndex());
    b.writeShort(typeSignature.getIndex());
  }

  static ConstNameAndType read (DataInputStream input) throws IOException {
    int cname = input.readUnsignedShort();
    int sig = input.readUnsignedShort();

    return new ConstNameAndType (cname, sig);
  }

  void resolve (ConstantPool p) {
    theName = (ConstUtf8) p.constantAt(theNameIndex);
    typeSignature = (ConstUtf8) p.constantAt(typeSignatureIndex);
  }
}


