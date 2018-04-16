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
  static final int opc_nop = 0;
  static final int opc_aconst_null = 1;
  static final int opc_iconst_m1 = 2;
  static final int opc_iconst_0 = 3;
  static final int opc_iconst_1 = 4;
  static final int opc_iconst_2 = 5;
  static final int opc_iconst_3 = 6;
  static final int opc_iconst_4 = 7;
  static final int opc_iconst_5 = 8;
  static final int opc_lconst_0 = 9;
  static final int opc_lconst_1 = 10;
  static final int opc_fconst_0 = 11;
  static final int opc_fconst_1 = 12;
  static final int opc_fconst_2 = 13;
  static final int opc_dconst_0 = 14;
  static final int opc_dconst_1 = 15;
  static final int opc_bipush = 16;
  static final int opc_sipush = 17;
  static final int opc_ldc = 18;
  static final int opc_ldc_w = 19;
  static final int opc_ldc2_w = 20;
  static final int opc_iload = 21;
  static final int opc_lload = 22;
  static final int opc_fload = 23;
  static final int opc_dload = 24;
  static final int opc_aload = 25;
  static final int opc_iload_0 = 26;
  static final int opc_iload_1 = 27;
  static final int opc_iload_2 = 28;
  static final int opc_iload_3 = 29;
  static final int opc_lload_0 = 30;
  static final int opc_lload_1 = 31;
  static final int opc_lload_2 = 32;
  static final int opc_lload_3 = 33;
  static final int opc_fload_0 = 34;
  static final int opc_fload_1 = 35;
  static final int opc_fload_2 = 36;
  static final int opc_fload_3 = 37;
  static final int opc_dload_0 = 38;
  static final int opc_dload_1 = 39;
  static final int opc_dload_2 = 40;
  static final int opc_dload_3 = 41;
  static final int opc_aload_0 = 42;
  static final int opc_aload_1 = 43;
  static final int opc_aload_2 = 44;
  static final int opc_aload_3 = 45;
  static final int opc_iaload = 46;
  static final int opc_laload = 47;
  static final int opc_faload = 48;
  static final int opc_daload = 49;
  static final int opc_aaload = 50;
  static final int opc_baload = 51;
  static final int opc_caload = 52;
  static final int opc_saload = 53;
  static final int opc_istore = 54;
  static final int opc_lstore = 55;
  static final int opc_fstore = 56;
  static final int opc_dstore = 57;
  static final int opc_astore = 58;
  static final int opc_istore_0 = 59;
  static final int opc_istore_1 = 60;
  static final int opc_istore_2 = 61;
  static final int opc_istore_3 = 62;
  static final int opc_lstore_0 = 63;
  static final int opc_lstore_1 = 64;
  static final int opc_lstore_2 = 65;
  static final int opc_lstore_3 = 66;
  static final int opc_fstore_0 = 67;
  static final int opc_fstore_1 = 68;
  static final int opc_fstore_2 = 69;
  static final int opc_fstore_3 = 70;
  static final int opc_dstore_0 = 71;
  static final int opc_dstore_1 = 72;
  static final int opc_dstore_2 = 73;
  static final int opc_dstore_3 = 74;
  static final int opc_astore_0 = 75;
  static final int opc_astore_1 = 76;
  static final int opc_astore_2 = 77;
  static final int opc_astore_3 = 78;
  static final int opc_iastore = 79;
  static final int opc_lastore = 80;
  static final int opc_fastore = 81;
  static final int opc_dastore = 82;
  static final int opc_aastore = 83;
  static final int opc_bastore = 84;
  static final int opc_castore = 85;
  static final int opc_sastore = 86;
  static final int opc_pop = 87;
  static final int opc_pop2 = 88;
  static final int opc_dup = 89;
  static final int opc_dup_x1 = 90;
  static final int opc_dup_x2 = 91;
  static final int opc_dup2 = 92;
  static final int opc_dup2_x1 = 93;
  static final int opc_dup2_x2 = 94;
  static final int opc_swap = 95;
  static final int opc_iadd = 96;
  static final int opc_ladd = 97;
  static final int opc_fadd = 98;
  static final int opc_dadd = 99;
  static final int opc_isub = 100;
  static final int opc_lsub = 101;
  static final int opc_fsub = 102;
  static final int opc_dsub = 103;
  static final int opc_imul = 104;
  static final int opc_lmul = 105;
  static final int opc_fmul = 106;
  static final int opc_dmul = 107;
  static final int opc_idiv = 108;
  static final int opc_ldiv = 109;
  static final int opc_fdiv = 110;
  static final int opc_ddiv = 111;
  static final int opc_irem = 112;
  static final int opc_lrem = 113;
  static final int opc_frem = 114;
  static final int opc_drem = 115;
  static final int opc_ineg = 116;
  static final int opc_lneg = 117;
  static final int opc_fneg = 118;
  static final int opc_dneg = 119;
  static final int opc_ishl = 120;
  static final int opc_lshl = 121;
  static final int opc_ishr = 122;
  static final int opc_lshr = 123;
  static final int opc_iushr = 124;
  static final int opc_lushr = 125;
  static final int opc_iand = 126;
  static final int opc_land = 127;
  static final int opc_ior = 128;
  static final int opc_lor = 129;
  static final int opc_ixor = 130;
  static final int opc_lxor = 131;
  static final int opc_iinc = 132;
  static final int opc_i2l = 133;
  static final int opc_i2f = 134;
  static final int opc_i2d = 135;
  static final int opc_l2i = 136;
  static final int opc_l2f = 137;
  static final int opc_l2d = 138;
  static final int opc_f2i = 139;
  static final int opc_f2l = 140;
  static final int opc_f2d = 141;
  static final int opc_d2i = 142;
  static final int opc_d2l = 143;
  static final int opc_d2f = 144;
  static final int opc_i2b = 145;
  static final int opc_i2c = 146;
  static final int opc_i2s = 147;
  static final int opc_lcmp = 148;
  static final int opc_fcmpl = 149;
  static final int opc_fcmpg = 150;
  static final int opc_dcmpl = 151;
  static final int opc_dcmpg = 152;
  static final int opc_ifeq = 153;
  static final int opc_ifne = 154;
  static final int opc_iflt = 155;
  static final int opc_ifge = 156;
  static final int opc_ifgt = 157;
  static final int opc_ifle = 158;
  static final int opc_if_icmpeq = 159;
  static final int opc_if_icmpne = 160;
  static final int opc_if_icmplt = 161;
  static final int opc_if_icmpge = 162;
  static final int opc_if_icmpgt = 163;
  static final int opc_if_icmple = 164;
  static final int opc_if_acmpeq = 165;
  static final int opc_if_acmpne = 166;
  static final int opc_goto = 167;
  static final int opc_jsr = 168;
  static final int opc_ret = 169;
  static final int opc_tableswitch = 170;
  static final int opc_lookupswitch = 171;
  static final int opc_ireturn = 172;
  static final int opc_lreturn = 173;
  static final int opc_freturn = 174;
  static final int opc_dreturn = 175;
  static final int opc_areturn = 176;
  static final int opc_return = 177;
  static final int opc_getstatic = 178;
  static final int opc_putstatic = 179;
  static final int opc_getfield = 180;
  static final int opc_putfield = 181;
  static final int opc_invokevirtual = 182;
  static final int opc_invokespecial = 183;
  static final int opc_invokestatic = 184;
  static final int opc_invokeinterface = 185;
  static final int opc_xxxunusedxxx = 186;
  static final int opc_new = 187;
  static final int opc_newarray = 188;
  static final int opc_anewarray = 189;
  static final int opc_arraylength = 190;
  static final int opc_athrow = 191;
  static final int opc_checkcast = 192;
  static final int opc_instanceof = 193;
  static final int opc_monitorenter = 194;
  static final int opc_monitorexit = 195;
  static final int opc_wide = 196;
  static final int opc_multianewarray = 197;
  static final int opc_ifnull = 198;
  static final int opc_ifnonnull = 199;
  static final int opc_goto_w = 200;
  static final int opc_jsr_w = 201;
}
