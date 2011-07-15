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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;
import java.io.DataOutputStream;


/**
 * ClassFile models the structure of a class as represented within
 * a class file.
 */

final public class ClassFile implements VMConstants {

  /* Class file constants */
  public static final int magic = 0xcafebabe;

  //@olsen: added more flexible version checking.
  public static final short[][] jdkMajorMinorVersions = new short[][]{
    new short[]{45, 3}, // jdk 1.1
    new short[]{46, 0}, // jdk 1.2
    new short[]{47, 0}, // jdk 1.3
    new short[]{48, 0}, // jdk 1.4
    new short[]{49, 0}, // jdk 1.5
    new short[]{50, 0}  // jdk 1.6      
  };
  public static final List jdkVersions =
    convertMajorMinorVersions(jdkMajorMinorVersions);
  public static final String supportedVersions = printSupportedVersions();

  private int majorVersion = 0;
  private int minorVersion = 0;

  /* The constant pool for the class file */
  private ConstantPool constantPool = new ConstantPool();

  /* access flag bit mask - see VMConstants */
  private int accessFlags = 0;

  /* The name of the class */
  private ConstClass thisClassName;

  /* The name of the super class */
  private ConstClass superClassName;

  /* A list of the interfaces which the class implements
   * The contents are ConstClass objects
   */
  private Vector classInterfaces = new Vector();

  /* A list of the fields which the class contains
   * The contents are ClassField objects
   */
  private Vector classFields = new Vector();

  /* A list of the methods which the class defines
   * The contents are ClassMethod objects
   */
  private Vector classMethods = new Vector();

  /* A list of the attributes associated with the class */
  private AttributeVector classAttributes = new AttributeVector();



  /* public accessors */



  /**
   * Return the constant pool for the class file
   */
  public ConstantPool pool() {
    return constantPool;
  }

  /**
   * Return the access flags for the class - see VMConstants
   */
  public int access() {
    return accessFlags;
  }

  /**
   * Is the class final?
   */
  final public boolean isFinal() {
    return (accessFlags & ACCFinal) != 0;
  }

  /**
   * Is the class an interface?
   */
  final public boolean isInterface() {
    return (accessFlags & ACCInterface) != 0;
  }

  /**
   * Is the class public?
   */
  final public boolean isPublic() {
    return (accessFlags & ACCPublic) != 0;
  }

  /**
   * Is the class abstract?
   */
  final public boolean isAbstract() {
    return (accessFlags & ACCAbstract) != 0;
  }


  /**
   * Set the access flags for the class - see VMConstants
   */
  public void setAccessFlags (int flags) {
    accessFlags = flags;
  }

  /**
   * Return the name of the class
   */
  public ConstClass className() {
    return thisClassName;
  }

  /**
   * Return the name of the super class
   */
  public ConstClass superName() {
    return superClassName;
  }

  /**
   * Return the name of the super class as a string
   */
  public String superNameString() {
    return (superClassName == null) ? null : superClassName.asString();
  }

  /**
   * Set the name of the super class
   */
  public void setSuperName(ConstClass superCl) {
    superClassName = superCl;
  }

  /**
   * Return the list of the interfaces which the class implements
   * The contents are ConstClass objects
   */
  public Vector interfaces() {
    return classInterfaces;
  }

  /**
   * Add an interface to the list of the interfaces which the class implements
   */
  public void addInterface (ConstClass iface) {
    classInterfaces.addElement(iface);
  }

  /**
   * Return the list of the fields which the class contains
   * The contents are ClassField objects
   */
  public Vector fields() {
    return classFields;
  }

  /**
   * Add a field to the list of the fields which the class contains
   */
  public void addField (ClassField field) {
    classFields.addElement(field);
  }

  /**
   * Add a field to the list of the fields which the class contains,
   * at the index'th position.
   */
  public void addField(ClassField field, int index) {
    classFields.insertElementAt(field, index);
  }

  /**
   * Return the list of the methods which the class defines
   * The contents are ClassMethod objects
   */
  public Vector methods() {
    return classMethods;
  }

  /**
   * Look for a method with the specified name and type signature
   */
  public ClassMethod findMethod(String methodName, String methodSig) {
    for (Enumeration e = methods().elements(); e.hasMoreElements();) {
      ClassMethod method = (ClassMethod) e.nextElement();
      if (method.name().asString().equals(methodName) &&
	  method.signature().asString().equals(methodSig))
	return method;
    }
    return null;
  }

  /**
   * Add a method to the list of the methods which the class defines
   */
  public void addMethod(ClassMethod method) {
    classMethods.addElement(method);
  }

  /**
   * Look for a field with the specified name
   */
  public ClassField findField(String fieldName) {
    for (Enumeration e = fields().elements(); e.hasMoreElements();) {
      ClassField field = (ClassField) e.nextElement();
      if (field.name().asString().equals(fieldName))
	return field;
    }
    return null;
  }

  /**
   * Return the list of the attributes associated with the class
   */
  public AttributeVector attributes() {
    return classAttributes;
  }

  /**
   * Construct a ClassFile from an input stream
   */
  public ClassFile(DataInputStream data) throws ClassFormatError {
    try {
      int thisMagic = data.readInt();
      if (thisMagic != magic)
        throw new ClassFormatError("Bad magic value for input");//NOI18N

      short thisMinorVersion = data.readShort();
      short thisMajorVersion = data.readShort();
      //@olsen: changed checking only target 1.1 and 1.2 to more
      //general check for a list of versions.

      if (isSupportedVersion(thisMajorVersion, thisMinorVersion)) {
         minorVersion = thisMinorVersion;
         majorVersion = thisMajorVersion;
      } else {
        throw new ClassFormatError("Bad version number: {" + //NOI18N
                  thisMajorVersion + "," + //NOI18N
                  thisMinorVersion +
                  "} expected one of: " + //NOI18N
                  supportedVersions);
      }
      readConstants(data);
      accessFlags = data.readUnsignedShort();
      thisClassName = (ConstClass)
      constantPool.constantAt(data.readUnsignedShort());
      superClassName = (ConstClass)
      constantPool.constantAt(data.readUnsignedShort());
      readInterfaces(data);
      readFields(data);
      readMethods(data);
      classAttributes = AttributeVector.readAttributes(data, constantPool);
    } catch (IOException e) {
      ClassFormatError cfe = new ClassFormatError("IOException during reading");//NOI18N
      cfe.initCause(e);
      throw cfe;
    }
    //@olsen: added println() for debugging
    //System.out.println("ClassFile(): new class = " + thisClassName.asString());
  }

  /**
   * Construct a bare bones class, ready for additions
   */
  public ClassFile(String cname, String supername) {
    thisClassName = constantPool.addClass(cname);
    superClassName = constantPool.addClass(supername);
    //@olsen: added println() for debugging
    //System.out.println("ClassFile(): new bare class file = " + thisClassName);
  }

  /**
   * Write the Class file to the data output stream
   */
  public
  void write (DataOutputStream buff) throws IOException {
    buff.writeInt(magic);
    buff.writeShort(minorVersion);
    buff.writeShort(majorVersion);
    constantPool.write(buff);
    buff.writeShort(accessFlags);
    buff.writeShort(thisClassName.getIndex());
    //@lars: superclass may be null (java.lang.Object); VMSpec 2nd ed., section 4.1
    buff.writeShort(superClassName == null ? 0 : superClassName.getIndex());
//    buff.writeShort(superClassName.getIndex());
    writeInterfaces(buff);
    writeFields(buff);
    writeMethods(buff);
    classAttributes.write(buff);
  }

  /**
   * Returns a byte array representation of this class.
   */
  public byte[] getBytes() throws java.io.IOException {
    /* Write the class bytes to a file, for debugging. */

    String writeClassToDirectory =
      System.getProperty("filter.writeClassToDirectory");
    if (writeClassToDirectory != null) {
      String filename = writeClassToDirectory + java.io.File.separator +
          thisClassName.asString() + ".class";//NOI18N
      System.err.println("Writing class to file " + filename);
      DataOutputStream stream = new DataOutputStream(
	  new java.io.FileOutputStream(filename));
      write(stream);
      stream.close();
    }

    /* Get the class bytes and return them. */

    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    write(new DataOutputStream(byteStream));

    return byteStream.toByteArray();
  }



  /* package local methods */

  public void print (PrintStream out) {
    constantPool.print(out);
    out.println();

    out.println("majorVersion = " + Integer.toString(majorVersion));//NOI18N
    out.println("minorVersion = " + Integer.toString(minorVersion));//NOI18N
    out.println("accessFlags = " + Integer.toString(accessFlags));//NOI18N
    out.println("className = " + thisClassName.asString());//NOI18N
    out.println("superClassName = " + superClassName.asString());//NOI18N
    out.print("Interfaces =");//NOI18N
    for (int i=0; i<classInterfaces.size(); i++) {
        out.print(" " + ((ConstClass) classInterfaces.elementAt(i)).asString());//NOI18N
    }
    out.println();

    out.println("fields =");//NOI18N
    for (int i=0; i<classFields.size(); i++) {
      ((ClassField) classFields.elementAt(i)).print(out, 3);
    }

    out.println("methods =");//NOI18N
    for (int i=0; i<classMethods.size(); i++) {
      ((ClassMethod) classMethods.elementAt(i)).print(out, 3);
    }

    out.println("attributes =");//NOI18N
    classAttributes.print(out, 3);

  }

  public void summarize () {
    PrintStream os = System.out;

    constantPool.summarize();
    int codeSize = 0;
    for (int i=0; i<classMethods.size(); i++) {
      codeSize += ((ClassMethod) classMethods.elementAt(i)).codeSize();
    }
    System.out.println(classMethods.size() + " methods in " + codeSize + " bytes");//NOI18N
  }

  /*
   * class file reading helpers
   */
  private void readConstants (DataInputStream data) throws IOException {
    constantPool = new ConstantPool(data);
  }

  private void readInterfaces(DataInputStream data) throws IOException {
    int nInterfaces = data.readUnsignedShort();
    while (nInterfaces-- > 0) {
      int interfaceIndex = data.readUnsignedShort();
      ConstClass ci = null;
      if (interfaceIndex != 0)
	ci = (ConstClass) constantPool.constantAt(interfaceIndex);
      classInterfaces.addElement(ci);
    }
  }

  private void writeInterfaces(DataOutputStream data) throws IOException {
    data.writeShort(classInterfaces.size());
    for (int i=0; i<classInterfaces.size(); i++) {
      ConstClass ci = (ConstClass) classInterfaces.elementAt(i);
      int interfaceIndex = 0;
      if (ci != null)
	interfaceIndex = ci.getIndex();
      data.writeShort(interfaceIndex);
    }
  }

  private void readFields(DataInputStream data) throws IOException {
    int nFields = data.readUnsignedShort();
    while (nFields-- > 0) {
      classFields.addElement (ClassField.read(data, constantPool));
    }
  }

  private void writeFields (DataOutputStream data) throws IOException {
    data.writeShort(classFields.size());
    for (int i=0; i<classFields.size(); i++)
      ((ClassField)classFields.elementAt(i)).write(data);
  }

  private void readMethods (DataInputStream data) throws IOException {
    int nMethods = data.readUnsignedShort();
    while (nMethods-- > 0) {
      classMethods.addElement (ClassMethod.read(data, constantPool));
    }
  }

  private void writeMethods (DataOutputStream data) throws IOException {
    data.writeShort(classMethods.size());
    for (int i=0; i<classMethods.size(); i++)
      ((ClassMethod)classMethods.elementAt(i)).write(data);
  }


    //@olsen: Static methods added for major.minor compatibility checking
    private static List convertMajorMinorVersions(short[][] majorMinor) {
      int length = majorMinor.length;
      List result = new ArrayList(length);
      for (int i = 0; i < length; i++) {
        result.add(getVersionInt(majorMinor[i][0], majorMinor[i][1]));
      }
      return result;
    }

    private static boolean isSupportedVersion(short major, short minor) {
      Integer version = getVersionInt(major, minor);
      return jdkVersions.contains(version);
    }

    private static Integer getVersionInt(short major, short minor) {
        return  new Integer(major * 65536 + minor);
    }

    public static final String printSupportedVersions() {
      StringBuffer buf = new StringBuffer("{"); //NOI18N
      int length = jdkMajorMinorVersions.length;
      for (int i = 0; i < length; i++) {
        int major = jdkMajorMinorVersions[i][0];
        int minor = jdkMajorMinorVersions[i][1];
        buf.append("{"); //NOI18N
        buf.append(major);
        buf.append(","); //NOI18N
        buf.append(minor);
        buf.append("}"); //NOI18N
        }
        buf.append("}"); //NOI18N
        return buf.toString();
    }

}

abstract class ArraySorter {
  protected ArraySorter() {}

  /* return the size of the array being sorted */
  abstract int size();

  /* return -1 if o1 < o2, 0 if o1 == o2, 1 if o1 > o2 */
  abstract int compare(int o1Index, int o2Index);

  /* Swap the elements at index o1Index and o2Index */
  abstract void swap(int o1Index, int o2Index);

  void sortArray() {
    sortArray(0, size()-1);
  }

  private void sortArray(int start, int end) {
    if (end > start) {
      swap(start, (start+end)/2);
      int last = start;
      for (int i = start+1; i<=end; i++) {
	if (compare(i, start) < 0)
	  swap (++last, i);
      }
      swap(start, last);
      sortArray(start, last-1);
      sortArray(last+1, end);
    }
  }
}

class InterfaceArraySorter extends ArraySorter {
  private ConstClass theArray[];

  InterfaceArraySorter(ConstClass[] interfaces) {
    theArray = interfaces;
  }

  /* return the size of the array being sorted */
  int size() { return theArray.length; }

  /* return -1 if o1 < o2, 0 if o1 == o2, 1 if o1 > o2 */
  int compare(int o1Index, int o2Index) {
    return theArray[o1Index].asString().compareTo(
	theArray[o2Index].asString());
  }

  /* Swap the elements at index o1Index and o2Index */
  void swap(int o1Index, int o2Index) {
    ConstClass tmp = theArray[o1Index];
    theArray[o1Index] = theArray[o2Index];
    theArray[o2Index] = tmp;
  }
}

class FieldArraySorter extends ArraySorter {
  private ClassField theArray[];

  FieldArraySorter(ClassField[] fields) {
    theArray = fields;
  }

  /* return the size of the array being sorted */
  int size() { return theArray.length; }

  /* return -1 if o1 < o2, 0 if o1 == o2, 1 if o1 > o2 */
  int compare(int o1Index, int o2Index) {
    return theArray[o1Index].name().asString().compareTo(
	theArray[o2Index].name().asString());
  }

  /* Swap the elements at index o1Index and o2Index */
  void swap(int o1Index, int o2Index) {
    ClassField tmp = theArray[o1Index];
    theArray[o1Index] = theArray[o2Index];
    theArray[o2Index] = tmp;
  }
}

class MethodArraySorter extends ArraySorter {
  private ClassMethod theArray[];

  MethodArraySorter(ClassMethod[] methods) {
    theArray = methods;
  }

  /* return the size of the array being sorted */
  int size() { return theArray.length; }

  /* return -1 if o1 < o2, 0 if o1 == o2, 1 if o1 > o2 */
  int compare(int o1Index, int o2Index) {
    int cmp = theArray[o1Index].name().asString().compareTo(
	theArray[o2Index].name().asString());
    if (cmp == 0) {
      cmp = theArray[o1Index].signature().asString().compareTo(
	theArray[o2Index].signature().asString());
    }
    return cmp;
  }

  /* Swap the elements at index o1Index and o2Index */
  void swap(int o1Index, int o2Index) {
    ClassMethod tmp = theArray[o1Index];
    theArray[o1Index] = theArray[o2Index];
    theArray[o2Index] = tmp;
  }


}

