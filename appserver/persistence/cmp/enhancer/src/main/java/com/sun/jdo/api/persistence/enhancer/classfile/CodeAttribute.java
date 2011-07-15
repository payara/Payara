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
import java.io.*;

/**
 * Subtype of ClassAttribute which describes the "Code" attribute
 * associated with a method.
 */

public class CodeAttribute extends ClassAttribute {
    public final static String expectedAttrName = "Code";//NOI18N

  /* The java class file contents defining this code attribute.
     If non-null, this must be disassembled before the remaining 
     fields of this instance may be accessed. */
  private byte theDataBytes[];

  /* The maximum number of stack entries used by this method */
  private int maxStack;

  /* The maximum number of local variables used by this method */
  private int maxLocals;

  /* The java VM byte code sequence for this method - null for native
     and abstract methods */
  private byte theCodeBytes[];

  /* The instruction sequence for this method - initially derived from
     the byte code array, but may later be modified */
  private Insn theCode;

  /* The exception ranges and handlers which apply to the code in this
     method */
  private ExceptionTable exceptionTable;

  /* The attributes which apply to this method */
  private AttributeVector codeAttributes;

  /* The method environment used for decompiling this code attribute */
  CodeEnv codeEnv;

  /* public accessors */


  /**
   * Return the maximum number of stack entries used by this method
   */
  public int stackUsed() {
    makeValid();
    return maxStack;
  }

  /**
   * Set the maximum number of stack entries used by this method
   */
  public void setStackUsed(int used) {
    makeValid();
    maxStack = used;
  }

  /**
   * Return the maximum number of local variables used by this method
   */
  public int localsUsed() {
    makeValid();
    return maxLocals;
  }

  /**
   * Set the maximum number of local variables used by this method
   */
  public void setLocalsUsed(int used) {
    makeValid();
    maxLocals = used;
  }

  /**
   * Return the java VM byte code sequence for this method - null for
   * native and abstract methods
   */
  public byte[] byteCodes() {
    makeValid();
    return theCodeBytes;
  }

  /**
   * Return the instruction sequence for this method - initially derived
   * from the byte code array, but may later be modified
   */
  public Insn theCode() {
    makeValid();
    if (theCode == null && codeEnv != null) {
      buildInstructions(codeEnv);
    }
    return theCode;
  }

  /**
   * Install the instruction sequence for this method - the byte code array
   * is later updated.
   */
  public void setTheCode(Insn insn) {
    makeValid();
    if (insn != null && insn.opcode() != Insn.opc_target)
      throw new InsnError(
          "The initial instruction in all methods must be a target");//NOI18N
    theCode = insn;
  }

  /**
   * Return the exception ranges and handlers which apply to the code in
   * this method.
   */
  public ExceptionTable exceptionHandlers() {
    makeValid();
    return exceptionTable;
  }

  /**
   * Return the attributes which apply to this code
   */
  public AttributeVector attributes() {
    makeValid();
    return codeAttributes;
  }


  /**
   * Constructs a CodeAttribute object for construction from scratch
   */
  public CodeAttribute(ConstUtf8 attrName,
		       int maxStack, int maxLocals,
		       Insn code, 
		       ExceptionTable excTable,
		       AttributeVector codeAttrs) {
    this(attrName, maxStack, maxLocals, code, null, /* byteCodes */
	 excTable, codeAttrs, null /* CodeEnv */ );
  }

  /**
   * Constructs a CodeAttribute object 
   */
  public CodeAttribute(ConstUtf8 attrName,
		       int maxStack, int maxLocals,
		       Insn code, byte[] codeBytes,
		       ExceptionTable excTable,
		       AttributeVector codeAttrs,
		       CodeEnv codeEnv) {
    super(attrName);
    this.maxStack = maxStack;
    this.maxLocals = maxLocals;
    theCode = code;
    theCodeBytes = codeBytes;
    exceptionTable = excTable;
    codeAttributes = codeAttrs;
    this.codeEnv = codeEnv;
  }


  /**
   * Constructs a CodeAttribute object for later disassembly
   */
  public CodeAttribute(ConstUtf8 attrName, byte[] dataBytes, CodeEnv codeEnv) {
    super(attrName);
    this.theDataBytes = dataBytes;
    this.codeEnv = codeEnv;
  }


  /* package local methods */


  static CodeAttribute read(ConstUtf8 attrName,
			    DataInputStream data, ConstantPool pool)
    throws IOException {
    int maxStack = data.readUnsignedShort();
    int maxLocals = data.readUnsignedShort();
    int codeLength = data.readInt();
    byte codeBytes[] = new byte[codeLength];
    data.readFully(codeBytes);
    Insn code = null;
    CodeEnv codeEnv = new CodeEnv(pool);

    ExceptionTable excTable = ExceptionTable.read(data, codeEnv);
    
    AttributeVector codeAttrs = 
      AttributeVector.readAttributes(data, codeEnv);

    return new CodeAttribute(attrName, maxStack, maxLocals, code, codeBytes,
			     excTable, codeAttrs, codeEnv);
  } 

  /* This version reads the attribute into a byte array for later 
     consumption */
  static CodeAttribute read(ConstUtf8 attrName, int attrLength,
			    DataInputStream data, ConstantPool pool)
    throws IOException {
    byte dataBytes[] = new byte[attrLength];
    data.readFully(dataBytes);
    return new CodeAttribute(attrName, dataBytes, new CodeEnv(pool));
  } 

  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    if (theDataBytes == null) {
      buildInstructionBytes();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream tmpOut = new DataOutputStream(baos);
      tmpOut.writeShort(maxStack);
      tmpOut.writeShort(maxLocals);
      tmpOut.writeInt(theCodeBytes.length);
      tmpOut.write(theCodeBytes, 0, theCodeBytes.length);
      exceptionTable.write(tmpOut);
      codeAttributes.write(tmpOut);

      tmpOut.flush();
      byte tmpBytes[] = baos.toByteArray();
      out.writeInt(tmpBytes.length);
      out.write(tmpBytes, 0, tmpBytes.length);
    } else {
      out.writeInt(theDataBytes.length);
      out.write(theDataBytes, 0, theDataBytes.length);
    }
  }

  void print(PrintStream out, int indent) {
    makeValid();
    ClassPrint.spaces(out, indent);
    out.print("Code:");//NOI18N
    out.print(" max_stack = " + Integer.toString(maxStack));//NOI18N
    out.print(" max_locals = " + Integer.toString(maxLocals));//NOI18N
    out.println(" Exceptions:");//NOI18N
    exceptionTable.print(out, indent+2);
    ClassPrint.spaces(out, indent);
    out.println("Code Attributes:");//NOI18N
    codeAttributes.print(out, indent+2);

    Insn insn = theCode();
    if (insn != null) {
      ClassPrint.spaces(out, indent);
      out.println("Instructions:");//NOI18N
      while (insn != null) {
	insn.print(out, indent+2);
	insn = insn.next();
      }
    }
  }

  /**
   *  Assign offsets to instructions and return the number of bytes.
   *  theCode must be non-null.
   */
  private int resolveOffsets() {
    Insn insn = theCode;
    int currPC = 0;
    while (insn != null) {
      currPC = insn.resolveOffset(currPC);
      insn = insn.next();
    }
    return currPC;
  }

  int codeSize() {
    makeValid();
    return theCodeBytes.length;
  }

  /**
   * Derive the instruction list from the instruction byte codes
   */
  private void buildInstructions(CodeEnv codeEnv) {
    if (theCodeBytes != null) {
      InsnReadEnv insnEnv = new InsnReadEnv(theCodeBytes, codeEnv);
      theCode = insnEnv.getTarget(0);
      Insn currInsn = theCode;

      /* First, create instructions */
      while (insnEnv.more()) {
	Insn newInsn = Insn.read(insnEnv);
	currInsn.setNext(newInsn);
	currInsn = newInsn;
      }

      /* Now, insert targets */
      InsnTarget targ;
      currInsn = theCode;
      Insn prevInsn = null;
      while (currInsn != null) {
	int off = currInsn.offset();

	/* We always insert a target a 0 to start so ignore that one */
	if (off > 0) {
	  targ = codeEnv.findTarget(off);
	  if (targ != null)
	    prevInsn.setNext(targ);
	}
	prevInsn = currInsn;
	currInsn = currInsn.next();
      }

      /* And follow up with a final target if needed */
      targ = codeEnv.findTarget(insnEnv.currentPC());
      if (targ != null)
	prevInsn.setNext(targ);
    }
  }

  /**
   * Derive the instruction byte codes from the instruction list
   * This should also recompute stack and variables but for now we
   * assume that this isn't needed
   */
  private void buildInstructionBytes() {
    if (theCode != null) {
      /* Make sure instructions have correct offsets */
      int size = resolveOffsets();
      theCodeBytes = new byte[size];

      Insn insn = theCode;
      int index = 0;
      while (insn != null) {
	index = insn.store(theCodeBytes, index);
	insn = insn.next();
      }
    }
  }

  /** If theDataBytes is non-null, disassemble this code attribute
   *  from the data bytes. */
  private void makeValid() {
    if (theDataBytes != null) {
      DataInputStream dis = new DataInputStream(
		new ByteArrayInputStream(theDataBytes));
      try {
	maxStack = dis.readUnsignedShort();
	maxLocals = dis.readUnsignedShort();
	int codeLength = dis.readInt();
	theCodeBytes = new byte[codeLength];
	dis.readFully(theCodeBytes);
	exceptionTable = ExceptionTable.read(dis, codeEnv);
	codeAttributes = AttributeVector.readAttributes(dis, codeEnv);
      } catch (java.io.IOException ioe) {
          ClassFormatError cfe = new ClassFormatError(
		"IOException while reading code attribute");//NOI18N
          cfe.initCause(ioe);
          throw cfe;
      }

      theDataBytes = null;
    }
  }

}

