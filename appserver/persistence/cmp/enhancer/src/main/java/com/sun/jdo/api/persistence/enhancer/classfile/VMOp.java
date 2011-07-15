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
 * Description of the VM opcodes
 */
public class VMOp implements VMConstants {
  /* The opcode value */
  private int opcodeValue;

  /* The name of the opcode */
  private String opcodeName;

  /* The number of stack argument words */
  private int stackArgs;

  /* The number of stack result words */
  private int stackResults;

    /* The "type" signature of the stack argument words *///NOI18N
  private String stackArgTypes;

    /* The "type" signature of the stack result words *///NOI18N
  private String stackResultTypes;

  /* public accessors */

  /**
   * Return the opcode value 
   */
  final public int opcode() {
    return opcodeValue;
  }

  /**
   * Return the opcode name
   */
  final public String name() {
    return opcodeName;
  }

  /**
   * Return the number of words of stack arguments expected by this operation.
   * If the number is not a fixed value, return -1;
   */
  final public int nStackArgs() {
    return stackArgs;
  }

  /**
   * Return the number of words of stack results produced by this operation.
   * If the number is not a fixed value, return -1;
   */
  final public int nStackResults() {
    return stackResults;
  }

  /**
   * Return the type descriptor for the stack arguments to the operation.
   */
  final public String argTypes() {
    return stackArgTypes;
  }

  /**
   * Return the type descriptor for the stack results of the operation.
   */
  final public String resultTypes() {
    return stackResultTypes;
  }

  /**
   * constructor for a VMOp
   */
   
  public VMOp(int theOpcode, String theOpcodeName, int nArgs, int nResults,
	      String argDescr, String resultDescr) {
    opcodeValue = theOpcode;
    opcodeName = theOpcodeName;
    stackArgs = nArgs;
    stackResults = nResults;
    stackArgTypes = argDescr;
    stackResultTypes = resultDescr;
  }

  /* package local methods */

  static VMOp[] ops =  {
    /* | no change*/
      new VMOp(opc_nop, "nop", 0, 0, "", ""),//NOI18N
    /* | ... -> ..., null */
      new VMOp(opc_aconst_null, "aconst_null", 0, 1, "", "A"),//NOI18N
    /* | ... -> ..., -1 */
      new VMOp(opc_iconst_m1, "iconst_m1", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 0 */
      new VMOp(opc_iconst_0, "iconst_0", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 1 */
      new VMOp(opc_iconst_1, "iconst_1", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 2 */
      new VMOp(opc_iconst_2, "iconst_2", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 3 */
      new VMOp(opc_iconst_3, "iconst_3", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 4 */
      new VMOp(opc_iconst_4, "iconst_4", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 5 */
      new VMOp(opc_iconst_5, "iconst_5", 0, 1, "", "I"),//NOI18N
    /* | ... -> ..., 0<high/low>, 0<high/low> */
      new VMOp(opc_lconst_0, "lconst_0", 0, 2, "", "J"),//NOI18N
    /* | ... -> ..., 1<high/low>, 1<high/low> */
      new VMOp(opc_lconst_1, "lconst_1", 0, 2, "", "J"),//NOI18N
    /* | ... -> ..., 0.0f */
      new VMOp(opc_fconst_0, "fconst_0", 0, 1, "", "F"),//NOI18N
    /* | ... -> ..., 1.0f */
      new VMOp(opc_fconst_1, "fconst_1", 0, 1, "", "F"),//NOI18N
    /* | ... -> ..., 2.0f */
      new VMOp(opc_fconst_2, "fconst_2", 0, 1, "", "F"),//NOI18N
    /* | ... -> ..., 0.0<high/low>, 0.0<high/low> */
      new VMOp(opc_dconst_0, "dconst_0", 0, 2, "", "D"),//NOI18N
    /* | ... -> ..., 1.0<high/low>, 1.0<high/low> */
      new VMOp(opc_dconst_1, "dconst_1", 0, 2, "", "D"),//NOI18N
    /* byte1 | ... => ..., value */
      new VMOp(opc_bipush, "bipush", 0, 1, "", "I"),//NOI18N
    /* byte1 byte2 | ... => ..., value */
      new VMOp(opc_sipush, "sipush", 0, 1, "", "I"),//NOI18N
    /* indexbyte1 | ... => ..., item */
      new VMOp(opc_ldc, "ldc", 0, 1, "", "W"),//NOI18N
    /* indexbyte1 indexbyte2 | ... => ..., item */
      new VMOp(opc_ldc_w, "ldc_w", 0, 1, "", "W"),//NOI18N
    /* indexbyte1 indexbyte2 | ... => ..., item1, item2 */
      new VMOp(opc_ldc2_w, "ldc2_w", 0, 2, "", "X"),//NOI18N
    /* vindex | ... => ..., value<vindex> */
      new VMOp(opc_iload, "iload", 0, 1, "", "I"),//NOI18N
    /* vindex | ... => ..., value<vindex><h/l>, value<vindex><h/l> */
      new VMOp(opc_lload, "lload", 0, 2, "", "J"),//NOI18N
    /* vindex | ... => ..., value<vindex> */
      new VMOp(opc_fload, "fload", 0, 1, "", "F"),//NOI18N
    /* vindex | ... => ..., value<vindex><h/l>, value<vindex><h/l> */
      new VMOp(opc_dload, "dload", 0, 2, "", "D"),//NOI18N
    /* vindex | ... => ..., value<vindex> */
      new VMOp(opc_aload, "aload", 0, 1, "", "A"),//NOI18N
    /* | ... => ..., value<0> */
      new VMOp(opc_iload_0, "iload_0", 0, 1, "", "I"),//NOI18N
    /* | ... => ..., value<1> */
      new VMOp(opc_iload_1, "iload_1", 0, 1, "", "I"),//NOI18N
    /* | ... => ..., value<2> */
      new VMOp(opc_iload_2, "iload_2", 0, 1, "", "I"),//NOI18N
    /* | ... => ..., value<3> */
      new VMOp(opc_iload_3, "iload_3", 0, 1, "", "I"),//NOI18N
    /* | ... => ..., value<0><h/l>, value<0><h/l> */
      new VMOp(opc_lload_0, "lload_0", 0, 2, "", "J"),//NOI18N
    /* | ... => ..., value<1><h/l>, value<1><h/l> */
      new VMOp(opc_lload_1, "lload_1", 0, 2, "", "J"),//NOI18N
    /* | ... => ..., value<2><h/l>, value<2><h/l> */
      new VMOp(opc_lload_2, "lload_2", 0, 2, "", "J"),//NOI18N
    /* | ... => ..., value<3><h/l>, value<3><h/l> */
      new VMOp(opc_lload_3, "lload_3", 0, 2, "", "J"),//NOI18N
    /* | ... => ..., value<0> */
      new VMOp(opc_fload_0, "fload_0", 0, 1, "", "F"),//NOI18N
    /* | ... => ..., value<1> */
      new VMOp(opc_fload_1, "fload_1", 0, 1, "", "F"),//NOI18N
    /* | ... => ..., value<2> */
      new VMOp(opc_fload_2, "fload_2", 0, 1, "", "F"),//NOI18N
    /* | ... => ..., value<3> */
      new VMOp(opc_fload_3, "fload_3", 0, 1, "", "F"),//NOI18N
    /* | ... => ..., value<0><h/l>, value<0><h/l> */
      new VMOp(opc_dload_0, "dload_0", 0, 2, "", "D"),//NOI18N
    /* | ... => ..., value<1><h/l>, value<1><h/l> */
      new VMOp(opc_dload_1, "dload_1", 0, 2, "", "D"),//NOI18N
    /* | ... => ..., value<2><h/l>, value<2><h/l> */
      new VMOp(opc_dload_2, "dload_2", 0, 2, "", "D"),//NOI18N
    /* | ... => ..., value<3><h/l>, value<3><h/l> */
      new VMOp(opc_dload_3, "dload_3", 0, 2, "", "D"),//NOI18N
    /* | ... => ..., value<0> */
      new VMOp(opc_aload_0, "aload_0", 0, 1, "", "A"),//NOI18N
    /* | ... => ..., value<1> */
      new VMOp(opc_aload_1, "aload_1", 0, 1, "", "A"),//NOI18N
    /* | ... => ..., value<2> */
      new VMOp(opc_aload_2, "aload_2", 0, 1, "", "A"),//NOI18N
    /* | ... => ..., value<3> */
      new VMOp(opc_aload_3, "aload_3", 0, 1, "", "A"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_iaload, "iaload", 2, 1, "AI", "I"),//NOI18N
    /* | ..., arrayref, index => ..., value<h/l>, value<h/l> */
      new VMOp(opc_laload, "laload", 2, 2, "AI", "J"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_faload, "faload", 2, 1, "AI", "F"),//NOI18N
    /* | ..., arrayref, index => ..., value<h/l>, value<h/l> */
      new VMOp(opc_daload, "daload", 2, 2, "AI", "D"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_aaload, "aaload", 2, 1, "AI", "A"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_baload, "baload", 2, 1, "AI", "I"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_caload, "caload", 2, 1, "AI", "I"),//NOI18N
    /* | ..., arrayref, index => ..., value */
      new VMOp(opc_saload, "saload", 2, 1, "AI", "I"),//NOI18N
    /* vindex | ..., value => ... */
      new VMOp(opc_istore, "istore", 1, 0, "I", ""),//NOI18N
    /* vindex | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_lstore, "lstore", 2, 0, "J", ""),//NOI18N
    /* vindex | ..., value => ... */
      new VMOp(opc_fstore, "fstore", 1, 0, "F", ""),//NOI18N
    /* vindex | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_dstore, "dstore", 2, 0, "D", ""),//NOI18N
    /* vindex | ..., value => ... */
      new VMOp(opc_astore, "astore", 1, 0, "A", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_istore_0, "istore_0", 1, 0, "I", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_istore_1, "istore_1", 1, 0, "I", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_istore_2, "istore_2", 1, 0, "I", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_istore_3, "istore_3", 1, 0, "I", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_lstore_0, "lstore_0", 2, 0, "J", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_lstore_1, "lstore_1", 2, 0, "J", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_lstore_2, "lstore_2", 2, 0, "J", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_lstore_3, "lstore_3", 2, 0, "J", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_fstore_0, "fstore_0", 1, 0, "F", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_fstore_1, "fstore_1", 1, 0, "F", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_fstore_2, "fstore_2", 1, 0, "F", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_fstore_3, "fstore_3", 1, 0, "F", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_dstore_0, "dstore_0", 2, 0, "D", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_dstore_1, "dstore_1", 2, 0, "D", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_dstore_2, "dstore_2", 2, 0, "D", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ... */
      new VMOp(opc_dstore_3, "dstore_3", 2, 0, "D", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_astore_0, "astore_0", 1, 0, "A", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_astore_1, "astore_1", 1, 0, "A", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_astore_2, "astore_2", 1, 0, "A", ""),//NOI18N
    /* | ..., value => ... */
      new VMOp(opc_astore_3, "astore_3", 1, 0, "A", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_iastore, "iastore", 3, 0, "AII", ""),//NOI18N
    /* | ..., arrayref, index, value<h/l>, value<h/l> => ... */
      new VMOp(opc_lastore, "lastore", 4, 0, "AIJ", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_fastore, "fastore", 3, 0, "AIF", ""),//NOI18N
    /* | ..., arrayref, index, value<h/l>, value<h/l> => ... */
      new VMOp(opc_dastore, "dastore", 4, 0, "AID", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_aastore, "aastore", 3, 0, "AIA", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_bastore, "bastore", 3, 0, "AII", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_castore, "castore", 3, 0, "AII", ""),//NOI18N
    /* | ..., arrayref, index, value => ... */
      new VMOp(opc_sastore, "sastore", 3, 0, "AII", ""),//NOI18N
    /* | ..., any => ... */
      new VMOp(opc_pop, "pop", 1, 0, "W", ""),//NOI18N
    /* | ..., any1, any2 => ... */
      new VMOp(opc_pop2, "pop2", 2, 0, "WW", ""),//NOI18N
    /* | ..., any => ..., any, any */
      new VMOp(opc_dup, "dup", 1, 2, "W", "WW"),//NOI18N
    /* | ..., any1, any2 => ..., any2, any1, any2 */
      new VMOp(opc_dup_x1, "dup_x1", 2, 3, "WW", "WWW"),//NOI18N
    /* | ..., any1, any2, any3 => ..., any3, any1, any2, any3 */
      new VMOp(opc_dup_x2, "dup_x2", 3, 4, "WWW", "WWWW"),//NOI18N
    /* | ..., any1, any2 => ..., any1, any2, any1, any2 */
      new VMOp(opc_dup2, "dup2", 2, 4, "WW", "WWWW"),//NOI18N
    /* | ..., any1, any2, any3 => ..., any2, any3, any1, any2, any3 */
      new VMOp(opc_dup2_x1, "dup2_x1", 3, 5, "WWW", "WWWWW"),//NOI18N
    /* | ..., any1, any2, any3, any4 => ..., any3, any4, any1, any2, any3, any4 */
      new VMOp(opc_dup2_x2, "dup2_x2", 4, 6, "WWWW", "WWWWWW"),//NOI18N
    /* | ..., any1, any2 => ..., any2, any1 */
      new VMOp(opc_swap, "swap", 2, 2, "WW", "WW"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_iadd, "iadd", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_ladd, "ladd", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_fadd, "fadd", 2, 1, "FF", "F"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_dadd, "dadd", 4, 2, "DD", "D"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_isub, "isub", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lsub, "lsub", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_fsub, "fsub", 2, 1, "FF", "F"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_dsub, "dsub", 4, 2, "DD", "D"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_imul, "imul", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lmul, "lmul", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_fmul, "fmul", 2, 1, "FF", "F"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_dmul, "dmul", 4, 2, "DD", "D"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_idiv, "idiv", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_ldiv, "ldiv", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_fdiv, "fdiv", 2, 1, "FF", "F"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_ddiv, "ddiv", 4, 2, "DD", "D"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_irem, "irem", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lrem, "lrem", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_frem, "frem", 2, 1, "FF", "F"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_drem, "drem", 4, 2, "DD", "D"),//NOI18N
    /* | ..., value => ..., result */
      new VMOp(opc_ineg, "ineg", 1, 1, "I", "I"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lneg, "lneg", 2, 2, "J", "J"),//NOI18N
    /* | ..., value => ..., result */
      new VMOp(opc_fneg, "fneg", 1, 1, "F", "F"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_dneg, "dneg", 2, 2, "D", "D"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_ishl, "ishl", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2 => ..., result */
      new VMOp(opc_lshl, "lshl", 3, 2, "JI", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_ishr, "ishr", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2 => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lshr, "lshr", 3, 2, "JI", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_iushr, "iushr", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2 => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lushr, "lushr", 3, 2, "JI", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_iand, "iand", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_land, "land", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_ior, "ior", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lor, "lor", 4, 2, "JJ", "J"),//NOI18N
    /* | ..., value1, value2 => ..., result */
      new VMOp(opc_ixor, "ixor", 2, 1, "II", "I"),//NOI18N
    /* | ..., value1<h/l>, value1<h/l>, value2<h/l>, value2<h/l> => ..., result<h/l>, result<h/l> */
      new VMOp(opc_lxor, "lxor", 4, 2, "JJ", "J"),//NOI18N
    /* vindex, const | no change */
      new VMOp(opc_iinc, "iinc", 0, 0, "", ""),//NOI18N
    /* | ..., value => ..., value<h/l>, value<h/l> */
      new VMOp(opc_i2l, "i2l", 1, 2, "I", "J"),//NOI18N
    /* | ..., value => ..., value */
      new VMOp(opc_i2f, "i2f", 1, 1, "I", "F"),//NOI18N
    /* | ..., value => ..., value<h/l>, value<h/l> */
      new VMOp(opc_i2d, "i2d", 1, 2, "I", "D"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value */
      new VMOp(opc_l2i, "l2i", 2, 1, "J", "I"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value */
      new VMOp(opc_l2f, "l2f", 2, 1, "J", "F"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value<h/l>, value<h/l> */
      new VMOp(opc_l2d, "l2d", 2, 2, "J", "D"),//NOI18N
    /* | ..., value => ..., value */
      new VMOp(opc_f2i, "f2i", 1, 1, "F", "I"),//NOI18N
    /* | ..., value => ..., value<h/l>, value<h/l> */
      new VMOp(opc_f2l, "f2l", 1, 2, "F", "J"),//NOI18N
    /* | ..., value => ..., value<h/l>, value<h/l> */
      new VMOp(opc_f2d, "f2d", 1, 2, "F", "D"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value */
      new VMOp(opc_d2i, "d2i", 2, 1, "D", "I"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value<h/l>, value<h/l> */
      new VMOp(opc_d2l, "d2l", 2, 2, "D", "J"),//NOI18N
    /* | ..., value<h/l>, value<h/l> => ..., value */
      new VMOp(opc_d2f, "d2f", 2, 1, "D", "F"),//NOI18N
    /* | ..., value => ..., result */
      new VMOp(opc_i2b, "i2b", 1, 1, "I", "I"),//NOI18N
    /* | ..., value => ..., result */
      new VMOp(opc_i2c, "i2c", 1, 1, "I", "I"),//NOI18N
    /* | ..., value => ..., result */
      new VMOp(opc_i2s, "i2s", 1, 1, "I", "I"),//NOI18N
    /* | ..., v1<h/l>, v1<h/l>, v2<h/l>, v2<h/l> => ..., result */
      new VMOp(opc_lcmp, "lcmp", 4, 1, "JJ", "I"),//NOI18N
    /*  | ..., v1<h/l>, v1<h/l>, v2<h/l>, v2<h/l> => ..., result */
      new VMOp(opc_fcmpl, "fcmpl", 2, 1, "FF", "I"),//NOI18N
    /*  | ..., v1, v2 => ..., result */
      new VMOp(opc_fcmpg, "fcmpg", 2, 1, "FF", "I"),//NOI18N
    /* | ..., v1<h/l>, v1<h/l>, v2<h/l>, v2<h/l> => ..., result */
      new VMOp(opc_dcmpl, "dcmpl", 4, 1, "DD", "I"),//NOI18N
    /* | ..., v1<h/l>, v1<h/l>, v2<h/l>, v2<h/l> => ..., result */
      new VMOp(opc_dcmpg, "dcmpg", 4, 1, "DD", "I"),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifeq, "ifeq", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifne, "ifne", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_iflt, "iflt", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifge, "ifge", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifgt, "ifgt", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifle, "ifle", 1, 0, "I", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmpeq, "if_icmpeq", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmpne, "if_icmpne", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmplt, "if_icmplt", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmpge, "if_icmpge", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmpgt, "if_icmpgt", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_icmple, "if_icmple", 2, 0, "II", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_acmpeq, "if_acmpeq", 2, 0, "AA", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value1, value2 => ... */
      new VMOp(opc_if_acmpne, "if_acmpne", 2, 0, "AA", ""),//NOI18N
    /* brbyte1, brbyte2 | no change */
      new VMOp(opc_goto, "goto", 0, 0, "", ""),//NOI18N
    /* brbyte1, brbyte2 | ... => ..., return_addr */
      new VMOp(opc_jsr, "jsr", 0, 1, "", "W"),//NOI18N
    /* vindex | no change */
      new VMOp(opc_ret, "ret", 0, 0, "", ""),//NOI18N
    /* ??? | ..., index => ... */
      new VMOp(opc_tableswitch, "tableswitch", 1, 0, "I", ""),//NOI18N
    /* ??? | ..., key => ... */
      new VMOp(opc_lookupswitch, "lookupswitch", 1, 0, "I", ""),//NOI18N
    /* | ..., value => [empty] */
      new VMOp(opc_ireturn, "ireturn", 1, 0, "I", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => [empty] */
      new VMOp(opc_lreturn, "lreturn", 2, 0, "J", ""),//NOI18N
    /* | ..., value => [empty] */
      new VMOp(opc_freturn, "freturn", 1, 0, "F", ""),//NOI18N
    /* | ..., value<h/l>, value<h/l> => [empty] */
      new VMOp(opc_dreturn, "dreturn", 2, 0, "D", ""),//NOI18N
    /* | ..., value => [empty] */
      new VMOp(opc_areturn, "areturn", 1, 0, "A", ""),//NOI18N
    /* | ... => [empty] */
      new VMOp(opc_return, "return", 0, 0, "", ""),//NOI18N
    /* idxbyte1, idxbyte2 | ... => ..., value [ value2 ] */
      new VMOp(opc_getstatic, "getstatic", 0, -1, "", "?"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., value [ value2 ] => ... */
      new VMOp(opc_putstatic, "putstatic", -1, 0, "?", ""),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref => ..., value [ value2 ] */
      new VMOp(opc_getfield, "getfield", 1, -1, "A", "?"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref, value [ value2 ] => ... */
      new VMOp(opc_putfield, "putfield", -1, 0, "A?", ""),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref, [args] => ... */
      new VMOp(opc_invokevirtual, "invokevirtual", -1, -1, "A?", "?"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref, [args] => ... */
      new VMOp(opc_invokespecial, "invokespecial", -1, -1, "A?", "?"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., [args] => ... */
      new VMOp(opc_invokestatic, "invokestatic", -1, -1, "?", "?"),//NOI18N
    /* idxbyte1, idxbyte2, nargs, rsvd | ..., objectref, [args] => ... */
      new VMOp(opc_invokeinterface, "invokeinterface", -1, -1, "A?", "?"),//NOI18N
    /* */
      new VMOp(opc_xxxunusedxxx, "xxxunusedxxx", 0, 0, "", ""),//NOI18N
    /* idxbyte1, idxbyte2 | ... => ..., objectref */
      new VMOp(opc_new, "new", 0, 1, "", "A"),//NOI18N
    /* atype | ..., size => ..., result */
      new VMOp(opc_newarray, "newarray", 1, 1, "I", "A"),//NOI18N
    /* indexbyte1, indexbyte2 | ..., size => ..., result */
      new VMOp(opc_anewarray, "anewarray", 1, 1, "I", "A"),//NOI18N
    /* | ..., objectref => ..., length */
      new VMOp(opc_arraylength, "arraylength", 1, 1, "A", "I"),//NOI18N
    /* | ..., objectref => [undefined] */
      new VMOp(opc_athrow, "athrow", 1, 0, "A", "?"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref => ..., objectref */
      new VMOp(opc_checkcast, "checkcast", 1, 1, "A", "A"),//NOI18N
    /* idxbyte1, idxbyte2 | ..., objectref => ..., result */
      new VMOp(opc_instanceof, "instanceof", 1, 1, "A", "I"),//NOI18N
    /* | ..., objectref => ... */
      new VMOp(opc_monitorenter, "monitorenter", 1, 0, "A", ""),//NOI18N
    /* | ..., objectref => ... */
      new VMOp(opc_monitorexit, "monitorexit", 1, 0, "A", ""),//NOI18N
    /* an instruction | special */
      new VMOp(opc_wide, "wide", 0, 0, "", ""),//NOI18N
    /* indexbyte1, indexbyte2, dimensions | ..., size1, ..., sizen => ..., result*/
      new VMOp(opc_multianewarray, "multianewarray", -1, 1, "?", "A"),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifnull, "ifnull", 1, 0, "A", ""),//NOI18N
    /* brbyte1, brbyte2 | ..., value => ... */
      new VMOp(opc_ifnonnull, "ifnonnull", 1, 0, "A", ""),//NOI18N
    /* brbyte1, brbyte2, brbyte3, brbyte4 | no change */
      new VMOp(opc_goto_w, "goto_w", 0, 0, "", ""),//NOI18N
    /* brbyte1, brbyte2, brbyte3, brbyte4 | ... => ..., return_addr */
      new VMOp(opc_jsr_w, "jsr_w", 0, 1, "", "W") };//NOI18N

  /**
   * Check that each entry in the ops array has a valid VMOp entry
   */
  private static void check() {
    for (int i=0; i<=opc_jsr_w; i++) {
      VMOp op = ops[i];
      if (op == null)
          throw new InsnError ("null VMOp for " + i);//NOI18N
      if (op.opcode() != i)
          throw new InsnError ("bad opcode for " + i);//NOI18N

      if (1 == 0) {
	/* check arg/result data */
	checkTypes(op.argTypes(), op.nStackArgs(), op);
	checkTypes(op.resultTypes(), op.nStackResults(), op);
      }
    }
  }

  private static void checkTypes(String types, int n, VMOp op) {
    for (int i=0; i<types.length(); i++) {
      char c = types.charAt(i);
      if (c == '?')
	return;
      if (c == 'J' || c == 'X' || c == 'D')
	n -= 2;
      else
	n -= 1;
    }
    if (n != 0)
        throw new InsnError ("Bad arg/result for VMOp " + op.opcodeName);//NOI18N
  }

  static {
    check();
  }
}
