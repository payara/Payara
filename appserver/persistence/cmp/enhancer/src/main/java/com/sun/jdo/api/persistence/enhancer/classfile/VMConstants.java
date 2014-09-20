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
 * VMConstants is a collection of the constants defined in the
 *   virtual machine spec.
 * Also included are several assorted constants of our own which
 *   seem logical to define here.
 */

public interface VMConstants {
  /* Access types */
  static final int ACCPublic    = 0x0001;
  static final int ACCPrivate   = 0x0002;
  static final int ACCProtected = 0x0004;
  static final int ACCStatic    = 0x0008;
  static final int ACCFinal     = 0x0010;
  static final int ACCSuper         = 0x0020;   /* For Class file */
  static final int ACCSynchronized  = 0x0020;   /* For methods    */
  static final int ACCVolatile  = 0x0040;
  static final int ACCTransient = 0x0080;
  static final int ACCNative    = 0x0100;
  static final int ACCInterface = 0x0200;
  static final int ACCAbstract  = 0x0400;


  /* Primitive Types */
  /* These correspond to the values used by newarray */
  static final int T_BOOLEAN = 4;
  static final int T_CHAR = 5;
  static final int T_FLOAT = 6;
  static final int T_DOUBLE = 7;
  static final int T_BYTE = 8;
  static final int T_SHORT = 9;
  static final int T_INT = 10;
  static final int T_LONG = 11;

  /* Class types - Not really part of the VM spec */
  static final int TC_OBJECT = 12;
  static final int TC_INTERFACE = 13;
  static final int TC_STRING = 14;

  /* Special pseudo types - Not really part of the VM spec */
  static final int T_UNKNOWN = 15;
  static final int T_WORD = 16;
  static final int T_TWOWORD = 17;

  /* Constant pool types */
  static final int CONSTANTUtf8 = 1;
  static final int CONSTANTUnicode = 2; /* Def.in Beta doc - not in 1.0.2 */
  static final int CONSTANTInteger = 3;
  static final int CONSTANTFloat = 4;
  static final int CONSTANTLong = 5;
  static final int CONSTANTDouble = 6;
  static final int CONSTANTClass = 7;
  static final int CONSTANTString = 8;
  static final int CONSTANTFieldRef = 9;
  static final int CONSTANTMethodRef = 10;
  static final int CONSTANTInterfaceMethodRef = 11;
  static final int CONSTANTNameAndType = 12;



  /* Java VM opcodes */
  final static int opc_nop = 0;
  final static int opc_aconst_null = 1;
  final static int opc_iconst_m1 = 2;
  final static int opc_iconst_0 = 3;
  final static int opc_iconst_1 = 4;
  final static int opc_iconst_2 = 5;
  final static int opc_iconst_3 = 6;
  final static int opc_iconst_4 = 7;
  final static int opc_iconst_5 = 8;
  final static int opc_lconst_0 = 9;
  final static int opc_lconst_1 = 10;
  final static int opc_fconst_0 = 11;
  final static int opc_fconst_1 = 12;
  final static int opc_fconst_2 = 13;
  final static int opc_dconst_0 = 14;
  final static int opc_dconst_1 = 15;
  final static int opc_bipush = 16;
  final static int opc_sipush = 17;
  final static int opc_ldc = 18;
  final static int opc_ldc_w = 19;
  final static int opc_ldc2_w = 20;
  final static int opc_iload = 21;
  final static int opc_lload = 22;
  final static int opc_fload = 23;
  final static int opc_dload = 24;
  final static int opc_aload = 25;
  final static int opc_iload_0 = 26;
  final static int opc_iload_1 = 27;
  final static int opc_iload_2 = 28;
  final static int opc_iload_3 = 29;
  final static int opc_lload_0 = 30;
  final static int opc_lload_1 = 31;
  final static int opc_lload_2 = 32;
  final static int opc_lload_3 = 33;
  final static int opc_fload_0 = 34;
  final static int opc_fload_1 = 35;
  final static int opc_fload_2 = 36;
  final static int opc_fload_3 = 37;
  final static int opc_dload_0 = 38;
  final static int opc_dload_1 = 39;
  final static int opc_dload_2 = 40;
  final static int opc_dload_3 = 41;
  final static int opc_aload_0 = 42;
  final static int opc_aload_1 = 43;
  final static int opc_aload_2 = 44;
  final static int opc_aload_3 = 45;
  final static int opc_iaload = 46;
  final static int opc_laload = 47;
  final static int opc_faload = 48;
  final static int opc_daload = 49;
  final static int opc_aaload = 50;
  final static int opc_baload = 51;
  final static int opc_caload = 52;
  final static int opc_saload = 53;
  final static int opc_istore = 54;
  final static int opc_lstore = 55;
  final static int opc_fstore = 56;
  final static int opc_dstore = 57;
  final static int opc_astore = 58;
  final static int opc_istore_0 = 59;
  final static int opc_istore_1 = 60;
  final static int opc_istore_2 = 61;
  final static int opc_istore_3 = 62;
  final static int opc_lstore_0 = 63;
  final static int opc_lstore_1 = 64;
  final static int opc_lstore_2 = 65;
  final static int opc_lstore_3 = 66;
  final static int opc_fstore_0 = 67;
  final static int opc_fstore_1 = 68;
  final static int opc_fstore_2 = 69;
  final static int opc_fstore_3 = 70;
  final static int opc_dstore_0 = 71;
  final static int opc_dstore_1 = 72;
  final static int opc_dstore_2 = 73;
  final static int opc_dstore_3 = 74;
  final static int opc_astore_0 = 75;
  final static int opc_astore_1 = 76;
  final static int opc_astore_2 = 77;
  final static int opc_astore_3 = 78;
  final static int opc_iastore = 79;
  final static int opc_lastore = 80;
  final static int opc_fastore = 81;
  final static int opc_dastore = 82;
  final static int opc_aastore = 83;
  final static int opc_bastore = 84;
  final static int opc_castore = 85;
  final static int opc_sastore = 86;
  final static int opc_pop = 87;
  final static int opc_pop2 = 88;
  final static int opc_dup = 89;
  final static int opc_dup_x1 = 90;
  final static int opc_dup_x2 = 91;
  final static int opc_dup2 = 92;
  final static int opc_dup2_x1 = 93;
  final static int opc_dup2_x2 = 94;
  final static int opc_swap = 95;
  final static int opc_iadd = 96;
  final static int opc_ladd = 97;
  final static int opc_fadd = 98;
  final static int opc_dadd = 99;
  final static int opc_isub = 100;
  final static int opc_lsub = 101;
  final static int opc_fsub = 102;
  final static int opc_dsub = 103;
  final static int opc_imul = 104;
  final static int opc_lmul = 105;
  final static int opc_fmul = 106;
  final static int opc_dmul = 107;
  final static int opc_idiv = 108;
  final static int opc_ldiv = 109;
  final static int opc_fdiv = 110;
  final static int opc_ddiv = 111;
  final static int opc_irem = 112;
  final static int opc_lrem = 113;
  final static int opc_frem = 114;
  final static int opc_drem = 115;
  final static int opc_ineg = 116;
  final static int opc_lneg = 117;
  final static int opc_fneg = 118;
  final static int opc_dneg = 119;
  final static int opc_ishl = 120;
  final static int opc_lshl = 121;
  final static int opc_ishr = 122;
  final static int opc_lshr = 123;
  final static int opc_iushr = 124;
  final static int opc_lushr = 125;
  final static int opc_iand = 126;
  final static int opc_land = 127;
  final static int opc_ior = 128;
  final static int opc_lor = 129;
  final static int opc_ixor = 130;
  final static int opc_lxor = 131;
  final static int opc_iinc = 132;
  final static int opc_i2l = 133;
  final static int opc_i2f = 134;
  final static int opc_i2d = 135;
  final static int opc_l2i = 136;
  final static int opc_l2f = 137;
  final static int opc_l2d = 138;
  final static int opc_f2i = 139;
  final static int opc_f2l = 140;
  final static int opc_f2d = 141;
  final static int opc_d2i = 142;
  final static int opc_d2l = 143;
  final static int opc_d2f = 144;
  final static int opc_i2b = 145;
  final static int opc_i2c = 146;
  final static int opc_i2s = 147;
  final static int opc_lcmp = 148;
  final static int opc_fcmpl = 149;
  final static int opc_fcmpg = 150;
  final static int opc_dcmpl = 151;
  final static int opc_dcmpg = 152;
  final static int opc_ifeq = 153;
  final static int opc_ifne = 154;
  final static int opc_iflt = 155;
  final static int opc_ifge = 156;
  final static int opc_ifgt = 157;
  final static int opc_ifle = 158;
  final static int opc_if_icmpeq = 159;
  final static int opc_if_icmpne = 160;
  final static int opc_if_icmplt = 161;
  final static int opc_if_icmpge = 162;
  final static int opc_if_icmpgt = 163;
  final static int opc_if_icmple = 164;
  final static int opc_if_acmpeq = 165;
  final static int opc_if_acmpne = 166;
  final static int opc_goto = 167;
  final static int opc_jsr = 168;
  final static int opc_ret = 169;
  final static int opc_tableswitch = 170;
  final static int opc_lookupswitch = 171;
  final static int opc_ireturn = 172;
  final static int opc_lreturn = 173;
  final static int opc_freturn = 174;
  final static int opc_dreturn = 175;
  final static int opc_areturn = 176;
  final static int opc_return = 177;
  final static int opc_getstatic = 178;
  final static int opc_putstatic = 179;
  final static int opc_getfield = 180;
  final static int opc_putfield = 181;
  final static int opc_invokevirtual = 182;
  final static int opc_invokespecial = 183;
  final static int opc_invokestatic = 184;
  final static int opc_invokeinterface = 185;
  final static int opc_xxxunusedxxx = 186;
  final static int opc_new = 187;
  final static int opc_newarray = 188;
  final static int opc_anewarray = 189;
  final static int opc_arraylength = 190;
  final static int opc_athrow = 191;
  final static int opc_checkcast = 192;
  final static int opc_instanceof = 193;
  final static int opc_monitorenter = 194;
  final static int opc_monitorexit = 195;
  final static int opc_wide = 196;
  final static int opc_multianewarray = 197;
  final static int opc_ifnull = 198;
  final static int opc_ifnonnull = 199;
  final static int opc_goto_w = 200;
  final static int opc_jsr_w = 201;
}
