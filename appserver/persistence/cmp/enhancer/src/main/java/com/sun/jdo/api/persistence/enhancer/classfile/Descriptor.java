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

import java.util.Stack;
import java.util.Map;

//@olsen: subst: Hashtable -> Map, HashMap


/**
 * A collection of static methods which manipulate type descriptors
 */

public class Descriptor implements VMConstants {
  /** 
   * Return the number of words of arguments to the method 
   * based on the method signature
   */
  public static int countMethodArgWords(String sig) {
    if (sig.charAt(0) != '(')
        throw new InsnError ("not a method signature");//NOI18N
    int count = 0;
    for (int idx = 1; sig.charAt(idx) != ')'; idx++) {
      switch (sig.charAt(idx)) {
      case 'B': /* byte */
      case 'C': /* char */
      case 'S': /* short */
      case 'I': /* int */
      case 'F': /* float */
      case 'Z': /* boolean */
	count++;
	break;
      case 'J': /* long */
      case 'D': /* double */
	count += 2;
	break;
      case 'L':
	count++;
	idx = sig.indexOf(';', idx);
	break;
      case '[':
	count++;
	while (sig.charAt(idx) == '[' || sig.charAt(idx) == ']')
	  idx++;
	if (sig.charAt(idx) == 'L')
	  idx = sig.indexOf(';', idx);
	/* else, let idx++ at loop iteration skip primitive descriptor */
	break;
      default:
          throw new InsnError("missing case");//NOI18N
      }
    }
    return count;
  }

  /** 
   * Return the number of words of return value for the method
   * based on the method signature
   */
  public static int countMethodReturnWords(String sig) {
    int idx = sig.lastIndexOf(')') + 1;
    if (idx == 0)
        throw new InsnError ("not a method signature");//NOI18N
    switch (sig.charAt(idx)) {
    case 'J': /* long */
    case 'D': /* double */
      return 2;
    case 'B': /* byte */
    case 'C': /* char */
    case 'S': /* short */
    case 'I': /* int */
    case 'F': /* float */
    case 'Z': /* boolean */
    case 'L': /* object */
    case '[': /* array */
      return 1;
    case 'V': /* void */
      return 0;
    default:
        throw new InsnError("missing case");//NOI18N
    }
  }

  /**
   * Return the stack descriptor for the result of a method
   * invocation.  Void return values yield "V".
   */
  public static String extractResultSig(String methodSig) {
    return methodSig.substring(methodSig.indexOf(')')+1);
  }

  /**
   * Return the stack descriptor for the arguments to a method
   * invocation (not including any "this" argument)
   */
  public static String extractArgSig(String methodSig) {
    return methodSig.substring(1, methodSig.indexOf(')'));
  }

  /**
   * Return the reversed stack descriptor for the arguments to a method
   * invocation (not including any "this" argument).  The top of stack
   * element will be first.
   */
  public static String extractReversedArgSig(String methodSig) {
    StringBuffer buf = new StringBuffer();;
    reverseArgSig(buf, methodSig, 1);
    return buf.toString();
  }

  /**
   * Given a StringBuffer, a method descriptor, and a index to the 
   * start of an argument descriptor, append the arguments to the
   * string buffer in reverse order.
   */
  private static void reverseArgSig(StringBuffer buf, String methodSig, 
				    int idx) {
    char c = methodSig.charAt(idx);
    if (c == ')')
      return;
    int startIdx = idx;

    switch(c) {
    case 'B':
    case 'C':
    case 'S':
    case 'I':
    case 'F':
    case 'J':
    case 'D':
    case 'Z':
      idx = idx+1;
      break;
    case '[':
      while (methodSig.charAt(idx) == '[' || methodSig.charAt(idx) == ']')
	idx++;
      if (methodSig.charAt(idx) != 'L') {
	idx++;
	break;
      }
      /* fall through */
    case 'L':
      idx = methodSig.indexOf(';', idx) + 1;
      break;
    default:
        throw new InsnError("bad signature char");//NOI18N
    }

    reverseArgSig(buf, methodSig, idx);
    while (startIdx < idx)
      buf.append(methodSig.charAt(startIdx++));
  }

  /** 
   * Return the number of words of a field based on its signature.
   */
  //@olsen: added method
  public static int countFieldWords(String sig) {
    if (sig == null || sig.length() < 1)
        throw new InsnError ("not a field signature");//NOI18N
    switch (sig.charAt(0)) {
    case 'J': /* long */
    case 'D': /* double */
      return 2;
    case 'B': /* byte */
    case 'C': /* char */
    case 'S': /* short */
    case 'I': /* int */
    case 'F': /* float */
    case 'Z': /* boolean */
    case 'L': /* object */
    case '[': /* array */
      return 1;
    default:
        throw new InsnError("missing case");//NOI18N
    }
  }

  /**
   * Return the element type for the first char in the type descriptor string.
   */
  //@olsen: added method
  public static int elementType(String sig) {
    if (sig == null || sig.length() < 1)
        throw new InsnError ("not a value signature");//NOI18N
    switch(sig.charAt(0)) {
      case 'B':
        return T_BOOLEAN;
      case 'C':
	return T_CHAR;
      case 'Z':
	return T_BYTE;
      case 'S':
	return T_SHORT;
      case 'I':
	return T_INT;
      case 'J':
	return T_LONG;
      case 'F':
	return T_FLOAT;
      case 'D':
	return T_DOUBLE;
      case '[':
	return TC_OBJECT;
      case 'L':
	return TC_OBJECT;
      default:
          throw new InsnError("bad signature char");//NOI18N
    }
  }

  /**
   * Return the element type descriptor char for the element type.
   * The element type must be one of the T_ or TC_OBJECT.
   */
  public static String elementSig(int valueType) {
    switch(valueType) {
    case T_BYTE:
        return "B";//NOI18N
    case T_CHAR:
        return "C";//NOI18N
    case T_BOOLEAN:
        return "Z";//NOI18N
    case T_SHORT:
        return "S";//NOI18N
    case T_INT:
        return "I";//NOI18N
    case T_LONG:
        return "J";//NOI18N
    case T_FLOAT:
        return "F";//NOI18N
    case T_DOUBLE:
        return "D";//NOI18N
    case TC_OBJECT:
        return "Ljava/lang/Object;";//NOI18N
    default:
        throw new InsnError("bad element type");//NOI18N
    }
  }

  /**
   * Return the number of stack words required for a value of the specified
   * type on the operand stack.
   */
  public static int elementSize(int elementType) {
    switch(elementType) {
    case T_LONG:
    case T_DOUBLE:
    case T_TWOWORD:
      return 2;
    default:
      return 1;
    }
  }

  /**
   * stackSig is a signature for a list of types on the JVM stack with the
   * last type in the signature intended to be on the top of JVM stack.
   * For each type in the signature, pushes an Integer objects identifying
   * the types on top of the input Stack object.
   */
  public static void computeStackTypes(String stackSig, Stack stack) {
    for (int idx = 0; idx < stackSig.length(); idx++) {
      int tp = 0;
      switch(stackSig.charAt(idx)) {
      case 'B':
      case 'C':
      case 'Z':
      case 'S':
      case 'I':
	tp = T_INT;
	break;
      case 'F':
	tp = T_FLOAT;
	break;
      case 'J':
	tp = T_LONG;
	break;
      case 'D':
	tp = T_DOUBLE;
	break;
      case '?':
	tp = T_UNKNOWN;
	break;
      case 'W':
	tp = T_WORD;
	break;
      case 'X':
	tp = T_TWOWORD;
	break;
      case 'A':
	/* This isn't a real type, but any object refrence */
	tp = TC_OBJECT;
	break;
      case '[':
	tp = TC_OBJECT;
	while (stackSig.charAt(idx) == '[' || stackSig.charAt(idx) == ']')
	  idx++;
	if (stackSig.charAt(idx) != 'L')
	    break;
	/* fall through */
      case 'L':
	tp = TC_OBJECT;
	idx = stackSig.indexOf(';', idx);
	break;
      default:
          throw new InsnError("bad signature char");//NOI18N
      }
      stack.push(new Integer(tp));
    }
  }

  /**
   * stackSig is a signature for the types on the stack with the last
   * type in the signature on the top of stack.  idx is the index of
   * the start of a valid signature type element.  Return the index of
   * the next element (which may be past the end of the string).
   */
  public static int nextSigElement(String stackSig, int idx) {
    switch(stackSig.charAt(idx)) {
    case 'B':
    case 'C':
    case 'Z':
    case 'S':
    case 'I':
    case 'F':
    case 'J':
    case 'D':
      break;
    case '[':
      while (stackSig.charAt(idx) == '[' || stackSig.charAt(idx) == ']')
	idx++;
      if (stackSig.charAt(idx) != 'L')
	break;
      /* fall through */
    case 'L':
      idx = stackSig.indexOf(';', idx);
      break;
    default:
        throw new InsnError("bad signature char");//NOI18N
    }

    idx++;
    return idx;
  }

  /**
   * classTranslations contains a set of mappings of class names.
   * For any types within the input signature which appear as keys
   * in the translation table, change the signature to replace the
   * original type with the translation.  Return a string containing
   * the original signature with any translations applied.
   */
  public static String remapTypes(String sig, Map classTranslations) {
    /* Defer allocation of the string buffer until it's needed */
    StringBuffer buf = null;

    for (int idx = 0; idx < sig.length(); idx++) {
      char c;
      switch(c = sig.charAt(idx)) {
      case '[':
	/* An array - skip through the [] pairs, copying to buf if not null */
	while ((c = sig.charAt(idx)) == '[' || c == ']') {
	  idx++;
	  if (buf != null)
	    buf.append(c);
	}

	/* If the next char isnt 'L', the next char is a simple type and
	   will be handled by the default 1 char translation */
	if (sig.charAt(idx) != 'L')
	  break;
	/* fall through to type name translation */
      case 'L':
	/* This is a type name */
	idx++;
	int endIdx = sig.indexOf(';', idx);
	String typeName = sig.substring(idx, endIdx);
	String mapTo = (String) classTranslations.get(typeName);
	if (mapTo != null) {
	  /* This type needs translation - allocate the string buffer
	     now if needed and copy in all up to this type name. */
	  if (buf == null) {
	    buf = new StringBuffer(sig.length() + 20);
	    buf.append(sig.substring(0,idx-1));
	  }
	  typeName = mapTo;
	}

	if (buf != null) {
	  buf.append('L');
	  buf.append(typeName);
	}
	idx = endIdx;
	c = ';';
	break;
      }

      if (buf != null)
	buf.append(c);
    }
    return (buf == null) ? sig : (buf.toString());
  }

  /**
   * classTranslations contains a set of mappings of class names.
   * Translate the class name (which may be an array class) according
   * to the entries in the translation table.
   * Return either the original string if no translation applies or
   * else the translated string.
   */
  public static String translateClass(
	String cls, Map classTranslations) {
    if (cls.charAt(0) == '[')
      return remapTypes(cls, classTranslations);
    else {
      String mapTo = (String) classTranslations.get(cls);
      if (mapTo != null)
	return mapTo;
      return cls;
    }
  }

  /**
   * Translates a VM type field signature into a  user-format signature.
   * Just a front for the two argument overload of this method.
   */
  public static String userFieldSig(String vmSig) {
    return userFieldSig(vmSig, 0);
  }

  /**
   * Translates a VM type field signature into a  user-format signature.
   */
  public static String userFieldSig(String vmSig, int idx) {
      String sigElement = "";//NOI18N
    int arrayDims = 0;
    boolean moreSig = true;
    while (moreSig) {
      moreSig = false;
      char c = vmSig.charAt(idx);
      switch (c) {
      case 'B':
          sigElement = "byte";//NOI18N
	break;
      case 'C':
          sigElement = "char";//NOI18N
	break;
      case 'Z':
          sigElement = "boolean";//NOI18N
	break;
      case 'S':
          sigElement = "short";//NOI18N
	break;
      case 'I':
          sigElement = "int";//NOI18N
	break;
      case 'F':
          sigElement = "float";//NOI18N
	break;
      case 'J':
          sigElement = "long";//NOI18N
	break;
      case 'D':
          sigElement = "double";//NOI18N
	break;
      case 'V':
	/* void isn't really valid as a field signature but this method
	   might be useful in implementing method signature conversion and
	   void is a valid return type. */
          sigElement = "void";//NOI18N
	break;
      case '[':
	idx++;
	arrayDims++;
	moreSig = true;
	break;
      case 'L':
	int nextIdx = vmSig.indexOf(';', idx);
	sigElement = vmSig.substring(idx+1,nextIdx).replace('/','.');
	break;
      default:
          throw new InsnError("bad signature char");//NOI18N
      }
    }

    /* If a non-array type, we already have the answer */
    if (arrayDims == 0)
      return sigElement;

    /* array types need a little more work */
    StringBuffer buf = new StringBuffer(sigElement.length() + 2 * arrayDims);
    buf.append(sigElement);
    while (arrayDims-- > 0) 
        buf.append("[]");//NOI18N

    return buf.toString();
  }

  /**
   * Produce a user consumable representation of a method argument list
   * from the method signature.  The return value is ignored.
   */
  public static String userMethodArgs(String methodSig) {
    /* This better be a method signature */
    if (methodSig.charAt(0) != '(')
        throw new InsnError("Invalid method signature");//NOI18N

    StringBuffer buf = new StringBuffer();

    buf.append('(');

    int idx = 1;
    boolean firstArg = true;
    while (methodSig.charAt(idx) != ')') {
      if (firstArg)
	firstArg = false;
      else
          buf.append(", ");//NOI18N
	
      buf.append(userFieldSig(methodSig, idx));
      idx = nextSigElement(methodSig, idx);
    }

    buf.append(')');
    return buf.toString();
  }

}
