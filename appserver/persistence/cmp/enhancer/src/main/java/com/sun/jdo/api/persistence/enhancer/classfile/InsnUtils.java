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
 * InsnUtils provides a set of static methods which serve to 
 * select vm instructions during code annotation.
 */

public
class InsnUtils implements VMConstants {

  /**
   * Return the best instruction for loading the specified integer 
   * constant onto the stack - hopefully use short form
   */
  public static Insn integerConstant(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_iconst_0);
    else if (i == 1)
      return Insn.create(opc_iconst_1);
    else if (i == 2)
      return Insn.create(opc_iconst_2);
    else if (i == 3)
      return Insn.create(opc_iconst_3);
    else if (i == 4)
      return Insn.create(opc_iconst_4);
    else if (i == 5)
      return Insn.create(opc_iconst_5);
    else if (i >= -128 && i < 128)
      return Insn.create(opc_bipush, i);
    return Insn.create(opc_ldc, pool.addInteger(i));
  }

  /**
   * Return the best instruction for loading the specified long constant onto
   * the stack.
   */
  public static Insn longConstant(long l, ConstantPool pool) {
    if (l == 0)
      return Insn.create(opc_lconst_0);
    else if (l == 1)
      return Insn.create(opc_lconst_1);
    else
      return Insn.create(opc_ldc2_w, pool.addLong(l));
  }

  /**
   * Return the best instruction for loading the specified float constant onto
   * the stack.
   */
  public static Insn floatConstant(float f, ConstantPool pool) {
    if (f == 0)
      return Insn.create(opc_fconst_0);
    else if (f == 1)
      return Insn.create(opc_fconst_1);
    else if (f == 2)
      return Insn.create(opc_fconst_2);
    else
      return Insn.create(opc_ldc, pool.addFloat(f));
  }

  /**
   * Return the best instruction for loading the specified double constant onto
   * the stack.
   */
  public static Insn doubleConstant(double d, ConstantPool pool) {
    if (d == 0)
      return Insn.create(opc_dconst_0);
    else if (d == 1)
      return Insn.create(opc_dconst_1);
    else
      return Insn.create(opc_ldc2_w, pool.addDouble(d));
  }

  /**
   * Return the best instruction for storing a reference to a local
   * variable slot
   */
  public static Insn aStore(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_astore_0);
    else if (i == 1)
      return Insn.create(opc_astore_1);
    else if (i == 2)
      return Insn.create(opc_astore_2);
    else if (i == 3)
      return Insn.create(opc_astore_3);
    return Insn.create(opc_astore, i);
  }

  /**
   * Return the best instruction for storing an int to a local
   * variable slot
   */
  public static Insn iStore(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_istore_0);
    else if (i == 1)
      return Insn.create(opc_istore_1);
    else if (i == 2)
      return Insn.create(opc_istore_2);
    else if (i == 3)
      return Insn.create(opc_istore_3);
    return Insn.create(opc_istore, i);
  }

  /**
   * Return the best instruction for storing a float to a local
   * variable slot
   */
  public static Insn fStore(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_fstore_0);
    else if (i == 1)
      return Insn.create(opc_fstore_1);
    else if (i == 2)
      return Insn.create(opc_fstore_2);
    else if (i == 3)
      return Insn.create(opc_fstore_3);
    return Insn.create(opc_fstore, i);
  }

  /**
   * Return the best instruction for storing a long to a local
   * variable slot
   */
  public static Insn lStore(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_lstore_0);
    else if (i == 1)
      return Insn.create(opc_lstore_1);
    else if (i == 2)
      return Insn.create(opc_lstore_2);
    else if (i == 3)
      return Insn.create(opc_lstore_3);
    return Insn.create(opc_lstore, i);
  }

  /**
   * Return the best instruction for storing a double to a local
   * variable slot
   */
  public static Insn dStore(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_dstore_0);
    else if (i == 1)
      return Insn.create(opc_dstore_1);
    else if (i == 2)
      return Insn.create(opc_dstore_2);
    else if (i == 3)
      return Insn.create(opc_dstore_3);
    return Insn.create(opc_dstore, i);
  }

  /**
   * Return the best instruction for loading a reference from a local
   * variable slot
   */
  public static Insn aLoad(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_aload_0);
    else if (i == 1)
      return Insn.create(opc_aload_1);
    else if (i == 2)
      return Insn.create(opc_aload_2);
    else if (i == 3)
      return Insn.create(opc_aload_3);
    return Insn.create(opc_aload, i);
  }

  /**
   * Return the best instruction for loading an int from a local
   * variable slot
   */
  public static Insn iLoad(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_iload_0);
    else if (i == 1)
      return Insn.create(opc_iload_1);
    else if (i == 2)
      return Insn.create(opc_iload_2);
    else if (i == 3)
      return Insn.create(opc_iload_3);
    return Insn.create(opc_iload, i);
  }

  /**
   * Return the best instruction for loading a float from a local
   * variable slot
   */
  public static Insn fLoad(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_fload_0);
    else if (i == 1)
      return Insn.create(opc_fload_1);
    else if (i == 2)
      return Insn.create(opc_fload_2);
    else if (i == 3)
      return Insn.create(opc_fload_3);
    return Insn.create(opc_fload, i);
  }

  /**
   * Return the best instruction for loading a long from a local
   * variable slot
   */
  public static Insn lLoad(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_lload_0);
    else if (i == 1)
      return Insn.create(opc_lload_1);
    else if (i == 2)
      return Insn.create(opc_lload_2);
    else if (i == 3)
      return Insn.create(opc_lload_3);
    return Insn.create(opc_lload, i);
  }

  /**
   * Return the best instruction for loading a double from a local
   * variable slot
   */
  public static Insn dLoad(int i, ConstantPool pool) {
    if (i == 0)
      return Insn.create(opc_dload_0);
    else if (i == 1)
      return Insn.create(opc_dload_1);
    else if (i == 2)
      return Insn.create(opc_dload_2);
    else if (i == 3)
      return Insn.create(opc_dload_3);
    return Insn.create(opc_dload, i);
  }

  /**
   * Return the best instruction for loading a value from a local
   * variable slot
   */
  public static Insn load(int tp, int i, ConstantPool pool) {
    switch(tp) {
    //@olsen: added these cases:
    case T_BOOLEAN:
    case T_CHAR:
    case T_BYTE:
    case T_SHORT:
    //@olsen: end added cases
    case T_INT:
      return iLoad(i, pool);
    case T_FLOAT:
      return fLoad(i, pool);
    case T_DOUBLE:
      return dLoad(i, pool);
    case T_LONG:
      return lLoad(i, pool);
    case TC_OBJECT:
      return aLoad(i, pool);
    default:
        throw new InsnError("bad load type");//NOI18N
    }
  }


  /**
   * Return the best instruction for storing a value to a local
   * variable slot
   */
  public static Insn store(int tp, int i, ConstantPool pool) {
    switch(tp) {
    //@olsen: added these cases:
    case T_BOOLEAN:
    case T_CHAR:
    case T_BYTE:
    case T_SHORT:
    //@olsen: end added cases
    case T_INT:
      return iStore(i, pool);
    case T_FLOAT:
      return fStore(i, pool);
    case T_DOUBLE:
      return dStore(i, pool);
    case T_LONG:
      return lStore(i, pool);
    case TC_OBJECT:
      return aStore(i, pool);
    default:
        throw new InsnError("bad store type");//NOI18N
    }
  }
}

