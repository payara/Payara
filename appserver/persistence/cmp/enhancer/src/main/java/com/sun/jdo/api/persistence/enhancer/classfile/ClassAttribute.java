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

import java.util.Vector;
import java.util.Hashtable;
import java.io.*;

/**
 * An abstract base class for the attributes within a class file
 */

public abstract class ClassAttribute implements VMConstants {

  /* The name of the attribute */
  private ConstUtf8 attributeName;

  /**
   * Returns the name of the attribute
   */
  public ConstUtf8 attrName() {
    return attributeName;
  }

  /**
   * Constructor
   */
  ClassAttribute(ConstUtf8 theAttrName) {
    attributeName = theAttrName;
  }

  /**
   * General attribute reader 
   */
  static ClassAttribute read(DataInputStream data, ConstantPool pool)
	throws IOException {

    ClassAttribute attr = null;
    int attrNameIndex = data.readUnsignedShort();
    ConstUtf8 attrName8 = (ConstUtf8) pool.constantAt(attrNameIndex);
    String attrName = attrName8.asString();
    int attrLength = data.readInt();

    if (attrName.equals(CodeAttribute.expectedAttrName)) {
      /* The old style code attribute reader uses more memory and
	 cpu when the instructions don't need to be examined than the
	 new deferred attribute reader.  We may at some point decide that
	 we want to change the default based on the current situation
	 but for now we will just use the deferred reader in all cases. */
      if (true) {
	attr = CodeAttribute.read(attrName8, attrLength, data, pool);
      } else {
	attr = CodeAttribute.read(attrName8, data, pool);
      }
    }
    else if (attrName.equals(SourceFileAttribute.expectedAttrName)) {
      attr = SourceFileAttribute.read(attrName8, data, pool);
    }
    else if (attrName.equals(ConstantValueAttribute.expectedAttrName)) {
      attr = ConstantValueAttribute.read(attrName8, data, pool);
    }
    else if (attrName.equals(ExceptionsAttribute.expectedAttrName)) {
      attr = ExceptionsAttribute.read(attrName8, data, pool);
    }
    else if (attrName.equals(AnnotatedClassAttribute.expectedAttrName)) {
      attr = AnnotatedClassAttribute.read(attrName8, data, pool);
    }
    else {
      /* Unrecognized method attribute */
      byte attrBytes[] = new byte[attrLength];
      data.readFully(attrBytes);
      attr = new GenericAttribute (attrName8, attrBytes);
    }

    return attr;
  }

  /*
   * CodeAttribute attribute reader
   */

  static ClassAttribute read(DataInputStream data, CodeEnv env)
	throws IOException {
    ClassAttribute attr = null;
    int attrNameIndex = data.readUnsignedShort();
    ConstUtf8 attrName8 = (ConstUtf8) env.pool().constantAt(attrNameIndex);
    String attrName = attrName8.asString();
    int attrLength = data.readInt();

    if (attrName.equals(LineNumberTableAttribute.expectedAttrName)) {
      attr = LineNumberTableAttribute.read(attrName8, data, env);
    }
    else if (attrName.equals(LocalVariableTableAttribute.expectedAttrName)) {
      attr = LocalVariableTableAttribute.read(attrName8, data, env);
    }
    else if (attrName.equals(AnnotatedMethodAttribute.expectedAttrName)) {
      attr = AnnotatedMethodAttribute.read(attrName8, data, env);
    }
    //@olsen: fix 4467428, added support for synthetic code attribute
    else if (attrName.equals(SyntheticAttribute.expectedAttrName)) {
      attr = SyntheticAttribute.read(attrName8, data, env.pool());
    }
    else {
      /* Unrecognized method attribute */
      byte attrBytes[] = new byte[attrLength];
      data.readFully(attrBytes);
      attr = new GenericAttribute (attrName8, attrBytes);
    }

    return attr;
  }

  /**
   * Write the attribute to the output stream
   */
  abstract void write(DataOutputStream out) throws IOException;

  /**
   * Print a description of the attribute to the print stream
   */
  abstract void print(PrintStream out, int indent);
}

