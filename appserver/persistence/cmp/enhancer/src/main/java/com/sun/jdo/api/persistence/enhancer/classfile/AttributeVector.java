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
import java.util.NoSuchElementException;

/**
 * A list of attributes within a class file.
 * These lists occur in several places within a class file
 *    - at class level
 *    - at method level
 *    - at field level
 *    - at attribute level
 */

public class AttributeVector {

  /* Vector of ClassAttribute */
  private ClassAttribute attributes[] = null;

  /**
   * Returns the i'th attribute in the array
   */
  private ClassAttribute attrAt(int i) {
    return attributes[i];
  }

  /**
   * Construct an empty AttributeVector
   */
  public AttributeVector() { }

  /**
   * Add an element to the vector
   */
  public void addElement(ClassAttribute attr) {
    if (attributes == null)
      attributes = new ClassAttribute[1];
    else {
      ClassAttribute newAttributes[] = new ClassAttribute[attributes.length+1];
      System.arraycopy(attributes, 0, newAttributes, 0, attributes.length);
      attributes = newAttributes;
    }
    attributes[attributes.length-1] = attr;
  }

  public Enumeration elements() {
    class AttributeVectorEnumeration implements Enumeration {
      private ClassAttribute[] attributes;
      private int current = 0;

      AttributeVectorEnumeration(ClassAttribute attrs[]) {
	attributes = attrs;
      }

      public boolean hasMoreElements() {
	return attributes != null && current < attributes.length;
      }
      public Object nextElement() {
	if (!hasMoreElements())
	  throw new NoSuchElementException();
	return attributes[current++];
      }
    }

    return new AttributeVectorEnumeration(attributes);
  }

  /**
   * Look for an attribute of a specific name
   */
  public ClassAttribute findAttribute(String attrName) {
    Enumeration e = elements();
    while (e.hasMoreElements()) {
      ClassAttribute attr = (ClassAttribute) e.nextElement();
      if (attr.attrName().asString().equals(attrName))
	return attr;
    }
    return null;
  }

  /**
   * General attribute reader
   */
  static AttributeVector readAttributes(
	DataInputStream data, ConstantPool constantPool)
	throws IOException {
    AttributeVector attribs = new AttributeVector();
    int n_attrs = data.readUnsignedShort();
    while (n_attrs-- > 0) {
      attribs.addElement(ClassAttribute.read(data, constantPool));
    }
    return attribs;
  }

  /**
   * ClassMethod attribute reader
   */
  static AttributeVector readAttributes(
	DataInputStream data, CodeEnv codeEnv)
	throws IOException {
    AttributeVector attribs = new AttributeVector();
    int n_attrs = data.readUnsignedShort();
    while (n_attrs-- > 0) {
      attribs.addElement(ClassAttribute.read(data, codeEnv));
    }
    return attribs;
  }

  /**
   * Write the attributes to the output stream
   */
  void write(DataOutputStream out) throws IOException {
    if (attributes == null) {
      out.writeShort(0);
    } else {
      out.writeShort(attributes.length);
      for (int i=0; i<attributes.length; i++)
	attributes[i].write(out);
    }
  }

  /**
   * Print a description of the attributes
   */
  void print(PrintStream out, int indent) {
    if (attributes != null) {
      for (int i=0; i<attributes.length; i++)
	attributes[i].print(out, indent);
    }
  }

  /**
   * Print a brief summary of the attributes
   */
  void summarize() {
    System.out.println((attributes == null ? 0 : attributes.length) +
		       " attributes");//NOI18N
  }

}

