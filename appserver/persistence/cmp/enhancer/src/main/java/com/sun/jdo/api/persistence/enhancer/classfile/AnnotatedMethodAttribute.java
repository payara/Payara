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
 * AnnotatedMethodAttribute represents a class level attribute
 * class file which identifies the level of annotation of the class.
 */

public class AnnotatedMethodAttribute extends ClassAttribute {

  /* The expected attribute name */
    public final static String expectedAttrName = "filter.annotatedMethod";//NOI18N

  /* The expected attribute version */
  public final static short expectedAttrVersion = 1;

  /* Bit mask indicating that the class was filter generated */
  public final static short generatedFlag = 0x1;

  /* Bit mask indicating that the class was filter annotated */
  public final static short annotatedFlag = 0x2;

    /* Bit mask indicating that the class was "repackaged" *///NOI18N
  public final static short modifiedFlag = 0x4;

  /* The version of the attribute */
  private short attrVersion;

  /* Flags associated with the annotation */
  private short annotationFlags;

  /* list of targets in the code sequence delimiting inserted instruction
   * sequences.  Even index targets are a range start (inclusive) and odd
   * targets represent a range end (exclusive) */
  private InsnTarget annotationRanges[];

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

  public InsnTarget[] getAnnotationRanges() {
    return annotationRanges;
  }

  public void setAnnotationRanges(InsnTarget[] ranges) {
    annotationRanges = ranges;
  }

  /**
   * Constructor
   */
  public AnnotatedMethodAttribute(
	ConstUtf8 nameAttr, short version, short annFlags,
	InsnTarget[] annRanges) {
    super(nameAttr);
    attrVersion = version;
    annotationFlags = annFlags;
    annotationRanges = annRanges;
  }

  /* package local methods */

  static AnnotatedMethodAttribute read(
	ConstUtf8 attrName, DataInputStream data, CodeEnv env)
    throws IOException {
    short version = data.readShort();
    short annFlags = data.readShort();

    short nRanges = data.readShort();

    InsnTarget ranges[] = new InsnTarget[nRanges*2];
    for (int i=0; i<nRanges; i++) {
      ranges[i*2] = env.getTarget(data.readShort());
      ranges[i*2+1] = env.getTarget(data.readShort());
    }
    return  new AnnotatedMethodAttribute(attrName, version, annFlags, ranges);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    if (annotationRanges == null)
      out.writeShort(2);
    else
      out.writeShort(4 + 2 * annotationRanges.length);
    out.writeShort(attrVersion);
    out.writeShort(annotationFlags);
    if (annotationRanges == null)
      out.writeShort(0);
    else {
      out.writeShort(annotationRanges.length / 2);
      for (int i=0; i<annotationRanges.length; i++)
	out.writeShort(annotationRanges[i].offset());
    }
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println("version: " + attrVersion);//NOI18N
    out.println(" flags: " + annotationFlags);//NOI18N
    if (annotationRanges != null) {
        out.println("Annotations: ");//NOI18N
      for (int i=0; i<annotationRanges.length/2; i++) {
	ClassPrint.spaces(out, indent+2);
	out.println(annotationRanges[i*2] + " to " +//NOI18N
		    annotationRanges[i*2+1]);
      }
    }
  }
}
