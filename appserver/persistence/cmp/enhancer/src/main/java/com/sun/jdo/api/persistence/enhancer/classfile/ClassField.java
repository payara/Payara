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
 * ClassField models the static and non-static fields of a class within
 * a class file.
 */

final public class ClassField extends ClassMember {
  /* access flag bit mask - see VMConstants */
  private int accessFlags;

  /* The name of the field */
  private ConstUtf8 fieldName;

  /* The type signature of the field */
  private ConstUtf8 fieldSignature;

  /* The attributes associated with the field */
  private AttributeVector fieldAttributes;
  

  /* public accessors */

  /**
   * Is the field transient?
   */
  public boolean isTransient() {
    return (accessFlags & ACCTransient) != 0;
  }

  /**
   * Return the access flags for the field - see VMConstants
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
   * Return the name of the field
   */
  public ConstUtf8 name() {
    return fieldName;
  }

  /**
   * Change the name of the field
   */
  public void changeName(ConstUtf8 name) {
    fieldName = name;
  }

  /**
   * Return the type signature of the field
   */
  public ConstUtf8 signature() {
    return fieldSignature;
  }

  /**
   * Change the type signature of the field
   */
  public void changeSignature(ConstUtf8 newSig) {
    fieldSignature = newSig;
  }

  /**
   * Return the attributes associated with the field
   */
  public AttributeVector attributes() {
    return fieldAttributes;
  }

  /**
   * Construct a class field object
   */
  public ClassField(int accFlags, ConstUtf8 name, ConstUtf8 sig,
                    AttributeVector field_attrs) {
    accessFlags = accFlags;
    fieldName = name;
    fieldSignature = sig;
    fieldAttributes = field_attrs;
  }

  /* package local methods */

  static ClassField read(DataInputStream data, ConstantPool pool) 
    throws IOException {
    ClassField f = null;
    int accessFlags = data.readUnsignedShort();
    int name_index = data.readUnsignedShort();
    int sig_index = data.readUnsignedShort();
    AttributeVector fieldAttribs = AttributeVector.readAttributes(data, pool);
    f = new ClassField(accessFlags, 
		       (ConstUtf8) pool.constantAt(name_index),
		       (ConstUtf8) pool.constantAt(sig_index),
		       fieldAttribs);
    return f;
  }

  void write (DataOutputStream data) throws IOException {
    data.writeShort(accessFlags);
    data.writeShort(fieldName.getIndex());
    data.writeShort(fieldSignature.getIndex());
    fieldAttributes.write(data);
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.print("'" + fieldName.asString() + "'");//NOI18N
    out.print(" sig = " + fieldSignature.asString());//NOI18N
    out.print(" access_flags = " + Integer.toString(accessFlags));//NOI18N
    out.println(" attributes:");//NOI18N
    fieldAttributes.print(out, indent+2);
  }
}

