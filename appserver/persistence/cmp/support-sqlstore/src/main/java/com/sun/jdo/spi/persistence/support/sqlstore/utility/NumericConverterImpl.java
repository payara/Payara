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

/*
 * NumericConverterImpl.java
 *
 * Created on March 21, 2003
 */

package com.sun.jdo.spi.persistence.support.sqlstore.utility;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This is a concrete implementation class for numeric conversion to BigDecimal
 * or BigInteger. For conversion to BigInteger, we truncate the fraction
 * part of the number.
 *
 * @author Shing Wai Chan
 */
public class NumericConverterImpl implements NumericConverter {
     /**
      *
      */
     public NumericConverterImpl() {
     }

     /**
      * To convert BigInteger to BigDecimal.
      * @param bInteger the BigInteger to be converted
      * @return converted BigDecimal
      */
     @Override
     public BigDecimal toBigDecimal(BigInteger bInteger) {
          return (bInteger == null) ? null : new BigDecimal(bInteger);
     }

     /**
      * To convert Double to BigDecimal.
      * @param d the Double to be converted
      * @return converted BigDecimal
      */
     @Override
     public BigDecimal toBigDecimal(Double d) {
          return (d == null) ? null : new BigDecimal(d.toString());
     }

     /**
      * To convert Float to BigDecimal.
      * @param f the Float to be converted
      * @return converted BigDecimal
      */
     @Override
     public BigDecimal toBigDecimal(Float f) {
          return (f == null) ? null : new BigDecimal(f.toString());
     }

     /**
      * To convert Number other than BigInteger, Double and Float to BigDecimal.
      * @param n the Number to be converted
      * @return converted BigDecimal
      */
     @Override
     public BigDecimal toBigDecimal(Number n) {
          return (n == null) ? null : new BigDecimal(n.toString());
     }

     /**
      * To convert BigDecimal to BigInteger.
      * @param bDecimal the BigDecimal to be converted
      * @return converted BigInteger
      */
     @Override
     public BigInteger toBigInteger(BigDecimal bDecimal) {
          return (bDecimal == null) ? null : bDecimal.toBigInteger();
     }

     /**
      * To convert Double to BigInteger.
      * @param d the Double to be converted
      * @return converted BigInteger
      */
     @Override
     public BigInteger toBigInteger(Double d) {
          return (d == null) ? null : (new BigDecimal(d.toString())).toBigInteger();
     }

     /**
      * To convert Float to BigInteger.
      * @param f the Float to be converted
      * @return converted BigInteger
      */
     @Override
     public BigInteger toBigInteger(Float f) {
          return (f == null) ? null : (new BigDecimal(f.toString())).toBigInteger();
     }

     /**
      * To convert Number other than BigDecimal, Double and Float to BigInteger.
      * @param n the Number to be converted
      * @return converted BigInteger
      */
     @Override
     public BigInteger toBigInteger(Number n) {
          return (n == null) ? null : BigInteger.valueOf(n.longValue());
     }
}
