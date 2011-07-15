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
import java.util.Vector;
import java.util.Enumeration;

/**
 * ClassMethod models the static and non-static methods of a class within
 * a class file.  This includes constructors and initializer code.
 */

public class ClassMethod extends ClassMember {
  /* The name of the constructor code */
    public final static String intializerName = "<init>";//NOI18N

  /* The name of the static initializer code */
    public final static String staticIntializerName = "<clinit>";//NOI18N

  /* access flag bit mask - see VMConstants */
  private int accessFlags;

  /* The name of the method */
  private ConstUtf8 methodName;

  /* The type signature of the method */
  private ConstUtf8 methodSignature;

  /* The attributes associated with the field */
  private AttributeVector methodAttributes;
  
  
  /* public accessors */

  /**
   * Return the access flags for the method - see VMConstants
   */
  public int access() {
    return accessFlags;
  }

  /**
   * Update the access flags for the field - see VMConstants
   */
  public void setAccess(int newFlags) {
    accessFlags = newFlags;
  }

  /**
   * Is the method abstract?
   */
  public boolean isAbstract() {
    return (accessFlags & ACCAbstract) != 0;
  }

  /**
   * Is the method native?
   */
  public boolean isNative() {
    return (accessFlags & ACCNative) != 0;
  }

  /**
   * Return the name of the method
   */
  public ConstUtf8 name() {
    return methodName;
  }

  /**
   * Change the name of the method
   */
  public void changeName(ConstUtf8 name) {
    methodName = name;
  }

  /**
   * Return the type signature of the method
   */
  public ConstUtf8 signature() {
    return methodSignature;
  }

  /**
   * Change the type signature of the method
   */
  public void changeSignature(ConstUtf8 newSig) {
    methodSignature = newSig;
  }

  /**
   * Return the attributes associated with the method
   */
  public AttributeVector attributes() {
    return methodAttributes;
  }

  /**
   * Construct a class method object
   */
  
  public ClassMethod(int accFlags, ConstUtf8 name, ConstUtf8 sig,
		     AttributeVector methodAttrs) {
    accessFlags = accFlags;
    methodName = name;
    methodSignature = sig;
    methodAttributes = methodAttrs;
  }

  /**
   * Returns the size of the method byteCode (if any)
   */
  int codeSize() {
    CodeAttribute codeAttr = codeAttribute();
    return (codeAttr == null) ? 0  : codeAttr.codeSize();
  }

  /**
   * Returns the CodeAttribute associated with this method (if any)
   */
  public CodeAttribute codeAttribute() {
    Enumeration e = methodAttributes.elements();
    while (e.hasMoreElements()) {
      ClassAttribute attr = (ClassAttribute) e.nextElement();
      if (attr instanceof CodeAttribute)
	return (CodeAttribute) attr;
    }
    return null;
  }

  /* package local methods */


  static ClassMethod read(DataInputStream data, ConstantPool pool) 
    throws IOException {
    int accessFlags = data.readUnsignedShort();
    int nameIndex = data.readUnsignedShort();
    int sigIndex = data.readUnsignedShort();
    ClassMethod f = 
      new ClassMethod(accessFlags, 
		      (ConstUtf8) pool.constantAt(nameIndex),
		      (ConstUtf8) pool.constantAt(sigIndex),
		      null);

    f.methodAttributes = AttributeVector.readAttributes(data, pool);
    return f;
  }

  void write(DataOutputStream data) throws IOException {
    CodeAttribute codeAttr = codeAttribute();
    data.writeShort(accessFlags);
    data.writeShort(methodName.getIndex());
    data.writeShort(methodSignature.getIndex());
    methodAttributes.write(data);
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.print("'" + methodName.asString() + "'");//NOI18N
    out.print(" sig = " + methodSignature.asString());//NOI18N
    out.print(" accessFlags = " + Integer.toString(accessFlags));//NOI18N
    out.println(" attributes:");//NOI18N
    methodAttributes.print(out, indent+2);
  }

}


