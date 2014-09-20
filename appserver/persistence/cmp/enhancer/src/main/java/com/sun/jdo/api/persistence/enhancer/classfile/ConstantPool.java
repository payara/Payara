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
 *  Constant Pool implementation - this represents the constant pool
 *  of a class in a class file.
 */

public class ConstantPool implements VMConstants {

  /* The actual pool */
  private Vector pool = new Vector();

  /* uniqifier tables */
  private boolean hashed = false;
  private Hashtable utfTable = new Hashtable(11);
  private Hashtable unicodeTable = new Hashtable(3);
  private Hashtable stringTable = new Hashtable(11);
  private Hashtable classTable = new Hashtable(11);
  private Hashtable intTable = new Hashtable(3);
  private Hashtable floatTable = new Hashtable(3);
  private Hashtable longTable = new Hashtable(3);
  private Hashtable doubleTable = new Hashtable(3);

  private Vector methodRefTable = new Vector();
  private Vector fieldRefTable = new Vector();
  private Vector ifaceMethodRefTable = new Vector();
  private Vector nameAndTypeTable = new Vector();

  /* public accessors */

  /**
   * Return the number of pool entries.
   */
  public int nEntries() {
    return pool.size();
  }

  /**
   * Return the constant in the pool at the specified entry index
   */
  public ConstBasic constantAt (int index) {
    return (ConstBasic) pool.elementAt(index);
  }

  /**
   * Find or create a class constant in the pool
   */
  public ConstClass addClass (String className) {
    hashConstants();
    ConstClass c = (ConstClass) classTable.get(className);
    if (c == null) {
      c = new ConstClass(addUtf8(className));
      internConstant(c);
    }
    return c;
  }
  
  /**
   * Find or create a field constant in the pool
   */
  public ConstFieldRef addFieldRef (String className, String fieldName,
                                     String type) {
    hashConstants();
    ConstFieldRef f = (ConstFieldRef)
      searchTable(fieldRefTable, className, fieldName, type);

    if (f == null) {
      f = new ConstFieldRef (addClass(className),
			     addNameAndType(fieldName, type));
      internConstant(f);
    }
    return f;
  }

  /**
   * Find or create a method constant in the pool
   */
  public ConstMethodRef addMethodRef (String className, String methodName,
				      String type) {
    hashConstants();
    ConstMethodRef m = (ConstMethodRef)
      searchTable(methodRefTable, className, methodName, type);
    if (m == null) {
      m = new ConstMethodRef (addClass(className),
			      addNameAndType(methodName, type));
      internConstant(m);
    }
    return m;
  }

  /**
   * Find or create an interface method constant in the pool
   */
  public ConstInterfaceMethodRef addInterfaceMethodRef (String className,
                              String methodName, String type) {
    hashConstants();
    ConstInterfaceMethodRef m = (ConstInterfaceMethodRef)
      searchTable(ifaceMethodRefTable, className, methodName, type);
    if (m == null) {
      m = new ConstInterfaceMethodRef (addClass(className),
				       addNameAndType(methodName, type));
      internConstant(m);
    }
    return m;
  }

  /**
   * Find or create a string constant in the pool
   */
  public ConstString addString (String s) {
    hashConstants();
    ConstString cs = (ConstString) stringTable.get(s);
    if (cs == null) {
      cs = new ConstString(addUtf8(s));
      internConstant(cs);
    }
    return cs;
  }
  
  /**
   * Find or create an integer constant in the pool
   */
  public ConstInteger addInteger (int i) {
    hashConstants();
    Integer io = new Integer(i);
    ConstInteger ci = (ConstInteger) intTable.get(io);
    if (ci == null) {
      ci = new ConstInteger(i);
      internConstant(ci);
    }
    return ci;
  }
  
  /**
   * Find or create a float constant in the pool
   */
  public ConstFloat addFloat (float f) {
    hashConstants();
    Float fo = new Float(f);
    ConstFloat cf = (ConstFloat) floatTable.get(fo);
    if (cf == null) {
      cf = new ConstFloat(f);
      internConstant(cf);
    }
    return cf;
  }
  
  /**
   * Find or create a long constant in the pool
   */
  public ConstLong addLong (long l) {
    hashConstants();
    Long lo = new Long(l);
    ConstLong cl = (ConstLong) longTable.get(lo);
    if (cl == null) {
      cl = new ConstLong(l);
      internConstant(cl);
      internConstant(null);
    }
    return cl;
  }
  
  /**
   * Find or create a double constant in the pool
   */
  public ConstDouble addDouble (double d) {
    hashConstants();
    Double dobj = new Double(d);
    ConstDouble cd = (ConstDouble) doubleTable.get(dobj);
    if (cd == null) {
      cd = new ConstDouble(d);
      internConstant(cd);
      internConstant(null);
    }
    return cd;
  }
  
  /**
   * Find or create a name/type constant in the pool
   */
  public ConstNameAndType addNameAndType (String name, String type) {
    hashConstants();
    for (int i=0; i<nameAndTypeTable.size(); i++) {
      ConstNameAndType nt = (ConstNameAndType) nameAndTypeTable.elementAt(i);
      if (nt.name().asString().equals(name) &&
	  nt.signature().asString().equals(type))
	return nt;
    }

    ConstNameAndType nt =
      new ConstNameAndType(addUtf8(name), addUtf8(type));
    internConstant(nt);
    return nt;
  }

  /**
   * Find or create a utf8 constant in the pool
   */
  public ConstUtf8 addUtf8 (String s) {
    hashConstants();
    ConstUtf8 u = (ConstUtf8) utfTable.get(s);
    if (u == null) {
      u = new ConstUtf8(s);
      internConstant(u);
    }
    return u;
  }

  /**
   * Find or create a unicode constant in the pool
   * Obsolete?
   */
  public ConstUnicode addUnicode (String s) {
    hashConstants();
    ConstUnicode u = (ConstUnicode) unicodeTable.get(s);
    if (u == null) {
      u = new ConstUnicode(s);
      internConstant(u);
    }
    return u;
  }

  /* package local methods */

  ConstantPool() {
    pool.addElement(null);
  }

  ConstantPool(DataInputStream input) throws IOException {
    pool.addElement(null);
    int nconstants = input.readUnsignedShort()-1;
    while (nconstants > 0)
      nconstants -= readConstant(input);

    resolvePool();
  }

  void print (PrintStream out) {
    for (int i=0; i<pool.size(); i++) {
      ConstBasic c = constantAt(i);
      if (c != null) {
        out.print (i);
        out.print (": ");//NOI18N
        out.println (c.toString());
      }
    }
  }

  void summarize () {
    int stringSize = 0;
    int nStrings = 0;
    for (int i=0; i<pool.size(); i++) {
      ConstBasic c = constantAt(i);
      if (c != null && c.tag() == CONSTANTUtf8) {
	ConstUtf8 utf8 = (ConstUtf8) c;
	stringSize += utf8.asString().length();
	nStrings++;
      }
    }
    System.out.println("  " + nStrings + " strings totalling " + //NOI18N
		       stringSize + " bytes");//NOI18N
  }

  void write (DataOutputStream buff) throws IOException {
    buff.writeShort(pool.size());
    for (int i=1; i<pool.size(); i++) {
      ConstBasic cb = (ConstBasic) pool.elementAt(i);
      if (cb != null) {
	buff.writeByte((byte) cb.tag());
	cb.formatData(buff);
      }
    }
  }

  /* private methods */

  private void resolvePool() {
    /* resolve indexes to object references */
    for (int i=0; i<pool.size(); i++) {
      ConstBasic c = constantAt(i);
      if (c != null) {
	c.setIndex(i);
        c.resolve(this);
      }
    }
  }

  private void hashConstants() {
    if (hashed)
      return;

    /* Enter objects into the hash tables */
    for (int j=0; j<pool.size(); j++) {
      ConstBasic c = constantAt(j);
      if (c != null) {
	recordConstant(c);
      }
    }

    hashed = true;
  }

  /* returns the number of slots used */
  private int readConstant(DataInputStream input) throws IOException {
    ConstBasic basic;
    byte b = input.readByte();
    int slots = 1;
    switch (b) {
    case CONSTANTUtf8:
      basic = ConstUtf8.read(input);
      break;
    case CONSTANTUnicode:
      basic = ConstUnicode.read(input);
      break;
    case CONSTANTInteger:
      basic = ConstInteger.read(input);
      break;
    case CONSTANTFloat:
      basic = ConstFloat.read(input);
      break;
    case CONSTANTLong:
      basic = ConstLong.read(input);
      slots = 2;
      break;
    case CONSTANTDouble:
      basic = ConstDouble.read(input);
      slots = 2;
      break;
    case CONSTANTClass:
      basic = ConstClass.read(input);
      break;
    case CONSTANTString:
      basic = ConstString.read(input);
      break;
    case CONSTANTFieldRef:
      basic = ConstFieldRef.read(input);
      break;
    case CONSTANTMethodRef:
      basic = ConstMethodRef.read(input);
      break;
    case CONSTANTInterfaceMethodRef:
      basic = ConstInterfaceMethodRef.read(input);
      break;
    case CONSTANTNameAndType:
      basic = ConstNameAndType.read(input);
      break;
    default:
        throw new ClassFormatError("Don't know this constant type: " +//NOI18N
			 Integer.toString(b));
    }

    pool.addElement(basic);
    if (slots > 1)
      pool.addElement(null);   
    return slots;
  }

  private void internConstant (ConstBasic c) {
    if (c != null) {
      c.setIndex(pool.size());
      recordConstant(c);
    }
    pool.addElement(c);
  }

  private void recordConstant (ConstBasic c) {
    if (c != null) {
      switch (c.tag()) {
      case CONSTANTUtf8:
	utfTable.put(((ConstUtf8)c).asString(), c);
	break;
      case CONSTANTUnicode:
	unicodeTable.put(((ConstUnicode)c).asString(), c);
	break;
      case CONSTANTInteger:
	intTable.put(new Integer(((ConstInteger)c).value()), c);
	break;
      case CONSTANTFloat:
	floatTable.put(new Float(((ConstFloat)c).value()), c);
	break;
      case CONSTANTLong:
	longTable.put(new Long(((ConstLong)c).value()), c);
	break;
      case CONSTANTDouble:
	doubleTable.put(new Double(((ConstDouble)c).value()), c);
	break;
      case CONSTANTClass:
	classTable.put(((ConstClass)c).asString(), c);
	break;
      case CONSTANTString:
	stringTable.put(((ConstString)c).value().asString(), c);
	break;
      case CONSTANTFieldRef:
	fieldRefTable.addElement(c);
	break;
      case CONSTANTMethodRef:
	methodRefTable.addElement(c);
	break;
      case CONSTANTInterfaceMethodRef:
	ifaceMethodRefTable.addElement(c);
	break;
      case CONSTANTNameAndType:
	nameAndTypeTable.addElement(c);
	break;
      }
    }
  }

  private ConstBasicMemberRef searchTable(Vector table, String cname,
					  String mname, String sig) {
    for (int i=0; i<table.size(); i++) {
      ConstBasicMemberRef memRef = (ConstBasicMemberRef) table.elementAt(i);
      if (memRef.className().asString().equals(cname) &&
	  memRef.nameAndType().name().asString().equals(mname) &&
	  memRef.nameAndType().signature().asString().equals(sig))
	return memRef;
    }
    return null;
  }


}

