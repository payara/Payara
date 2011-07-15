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
 * AnnotatedClassAttribute represents a class level attribute
 * class file which identifies the level of annotation of the class.
 */

public class AnnotatedClassAttribute extends ClassAttribute {

  /* The expected attribute name */
    public final static String expectedAttrName = "filter.annotatedClass";//NOI18N

  /* The expected attribute version */
  public final static short expectedAttrVersion = 1;

  /* Bit mask indicating that the class was filter generated */
  public final static short generatedFlag = 0x1;

  /* Bit mask indicating that the class was filter annotated */
  public final static short annotatedFlag = 0x2;

  /* Bit mask indicating that the class was "repackaged" or similarly
   * modified */
  public final static short modifiedFlag = 0x4;

  /* The version of the attribute */
  private short attrVersion;

  /* Flags associated with the annotation */
  private short annotationFlags;

  /* The modification date of the class file at the time of modification */
  private long classModTime;

  /* The date of the annotation */
  private long classAnnotationTime;

  /* public accessors */

  public short getVersion() {
    return attrVersion;
  }

  public void setVersion(short version) {
    attrVersion = version;
  }

  public short getFlags() {
    return annotationFlags;
  }

  public void setFlags(short flags) {
    annotationFlags = flags;
  }

  public long getModTime() {
    return classModTime;
  }

  public void setModTime(long time) {
    classModTime = time;
  }

  public long getAnnotationTime() {
    return classAnnotationTime;
  }

  public void setAnnotationTime(long time) {
    classAnnotationTime = time;
  }

  /**
   * Constructor
   */
  public AnnotatedClassAttribute(
	ConstUtf8 nameAttr, short version, short annFlags,
	long modTime, long annTime) {
    super(nameAttr);
    attrVersion = version;
    annotationFlags = annFlags;
    classModTime = modTime;
    classAnnotationTime = annTime;
  }

  /* package local methods */

  static AnnotatedClassAttribute read(
	ConstUtf8 attrName, DataInputStream data, ConstantPool pool)
    throws IOException {
    short version = data.readShort();
    short annFlags = data.readShort();
    long modTime = data.readLong();
    long annTime = data.readLong();
    return  new AnnotatedClassAttribute(attrName, version, annFlags,
					modTime, annTime);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    out.writeShort(20);
    out.writeShort(attrVersion);
    out.writeShort(annotationFlags);
    out.writeLong(classModTime);
    out.writeLong(classAnnotationTime);
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println("version: " + attrVersion);//NOI18N
    out.println(" flags: " + annotationFlags);//NOI18N
    out.println(" modTime: " + classModTime);//NOI18N
    out.println(" annTime: " + classAnnotationTime);//NOI18N
  }
}
