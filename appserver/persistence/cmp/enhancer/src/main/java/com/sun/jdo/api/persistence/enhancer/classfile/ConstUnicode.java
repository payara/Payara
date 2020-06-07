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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.jdo.api.persistence.enhancer.classfile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class representing a unicode string value in the constant pool
 */

/*
 * Note: evidence suggests that this is no longer part of the java VM
 * spec.
 */

public class ConstUnicode extends ConstBasic {
  /* The tag associated with ConstClass entries */
  public static final int MyTag = CONSTANTUnicode;

  /* The unicode string of interest */
  private String stringValue;

  /* public accessors */

  /**
   * The tag of this constant entry
   */
  public int tag () { return MyTag; }

  /**
   * return the value associated with the entry
   */
  public String asString() {
    return stringValue;
  }

  /**
   * A printable representation
   */
  public String toString () {
      return "CONSTANTUnicode(" + indexAsString() + "): " + stringValue;//NOI18N
  }

  /* package local methods */

  ConstUnicode (String s) {
    stringValue = s;
  }

  void formatData (DataOutputStream b) throws IOException {
    b.writeBytes(stringValue);
  }

  static ConstUnicode read (DataInputStream input) throws IOException {
    int count = input.readShort(); // Is this chars or bytes?
    StringBuilder b = new StringBuilder();
    for (int i=0; i < count; i++) {
      b.append(input.readChar());
    }
    return new ConstUnicode (b.toString());
  }

  void resolve (ConstantPool p) {
  }
}


