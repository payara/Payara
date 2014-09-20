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

/**
 * Environment for decoding byte codes into instructions
 */

class InsnReadEnv {

  /* The parent method environment */
  private CodeEnv codeEnv;

  /* The byte codes to be decoded */
  private byte[] byteCodes;

  /* The index into byteCodes for the next instruction to be decoded */
  private int currPc;

  /**
   * Constructor
   */
  InsnReadEnv(byte[] bytes, CodeEnv codeEnv) {
    this.byteCodes = bytes;
    this.currPc = 0;
    this.codeEnv = codeEnv;
  }

  /**
   * Return the index of the next instruction to decode
   */
  int currentPC() {
    return currPc;
  }

  /**
   * Are there more byte codes to decode?
   */
  boolean more() {
    return currPc < byteCodes.length;
  }

  /**
   * Get a single byte from the byte code stream
   */
  byte getByte() {
    if (!more())
        throw new InsnError("out of byte codes");//NOI18N

    return byteCodes[currPc++];
  }

  /**
   * Get a single unsigned byte from the byte code stream
   */
  int getUByte() {
    return getByte() & 0xff;
  }

  /**
   * Get a short from the byte code stream
   */
  int getShort() {
    byte byte1 = byteCodes[currPc++];
    byte byte2 = byteCodes[currPc++];
    return (byte1 << 8) | (byte2 & 0xff);
  }

  /**
   * Get an unsigned short from the byte code stream
   */
  int getUShort() {
    return getShort() & 0xffff;
  }

  /**
   * Get an int from the byte code stream
   */
  int getInt() {
    byte byte1 = byteCodes[currPc++];
    byte byte2 = byteCodes[currPc++];
    byte byte3 = byteCodes[currPc++];
    byte byte4 = byteCodes[currPc++];
    return (byte1 << 24) | ((byte2 & 0xff) << 16) |
	    ((byte3  & 0xff) << 8) | (byte4 & 0xff);
  }

  /**
   * Get the constant pool which applies to the method being decoded
   */
  ConstantPool pool() {
    return codeEnv.pool();
  }

  /**
   * Get the canonical InsnTarget instance for the specified
   * pc within the method.
   */
  InsnTarget getTarget(int targ) {
    return codeEnv.getTarget(targ);
  }
}
