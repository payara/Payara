/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package com.sun.ts.tests.common.webclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sun.ts.lib.util.BASE64Encoder;

public class Util {

  /**
   * Zeros
   */
  private static final String ZEROS = "00000000";

  /**
   * short pad size
   */
  private static final int SHORTPADSIZE = 4;

  /**
   * byte pad size
   */
  private static final int BYTEPADSIZE = 2;

  /** Creates new Util */
  private Util() {
  }

  /*
   * public methods
   * ========================================================================
   */

  /**
   * BASE64 encodes the provided string.
   * 
   * @return BASE64 encoded string.
   */
  public static String getBase64EncodedString(String value) {
    BASE64Encoder encoder = new BASE64Encoder();
    return encoder.encode(value.getBytes());
  }

  /**
   * Returns a charset encoded string based on the provided InputStream and
   * charset encoding.
   *
   * @return encoding string
   */
  public static String getEncodedStringFromStream(InputStream in, String enc)
      throws IOException {
    BufferedReader bin = new BufferedReader(new InputStreamReader(in, enc));
    StringBuffer sb = new StringBuffer(128);
    for (int ch = bin.read(); ch != -1; ch = bin.read()) {
      sb.append((char) ch);
    }
    return sb.toString();
  }

  /**
   * <code>getHexValue</code> displays a formatted hex representation of the
   * passed byte array. It also allows for only a specified offset and length of
   * a particular array to be returned.
   *
   * @param bytes
   *          <code>byte[]</code> array to process.
   * @param pos
   *          <code>int</code> specifies offset to begin processing.
   * @param len
   *          <code>int</code> specifies the number of bytes to process.
   * @return <code>String</code> formatted hex representation of processed
   *         array.
   */
  public static String getHexValue(byte[] bytes, int pos, int len) {
    StringBuffer outBuf = new StringBuffer(bytes.length * 2);
    int bytesPerLine = 36;
    int cnt = 1;
    int groups = 4;
    int curPos = pos;
    int linePos = 1;
    boolean displayOffset = true;

    while (len-- > 0) {
      if (displayOffset) {

        outBuf.append("\n" + paddedHexString(pos, SHORTPADSIZE, true) + ": ");
        displayOffset = false;
      }

      outBuf.append(paddedHexString((int) bytes[pos], BYTEPADSIZE, false));
      linePos += 2; // Byte is padded to 2 characters

      if ((cnt % 4) == 0) {
        outBuf.append(" ");
        linePos++;
      }

      // Now display the characters that are printable
      if ((cnt % (groups * 4)) == 0) {
        outBuf.append(" ");

        while (curPos <= pos) {
          if (!Character.isWhitespace((char) bytes[curPos])) {
            outBuf.append((char) bytes[curPos]);
          } else {
            outBuf.append(".");
          }

          curPos++;
        }

        curPos = pos + 1;
        linePos = 1;
        displayOffset = true;
      }

      cnt++;
      pos++;
    }

    // pad out the line with spaces
    while (linePos++ <= bytesPerLine) {
      outBuf.append(" ");
    }

    outBuf.append(" ");
    // Now display the printable characters for the trailing bytes
    while (curPos < pos) {
      if (!Character.isWhitespace((char) bytes[curPos])) {
        outBuf.append((char) bytes[curPos]);
      } else {
        outBuf.append(".");
      }

      curPos++;
    }

    return outBuf.toString();
  }

  /*
   * private methods
   * ========================================================================
   */

  /**
   * <code>paddedHexString</code> pads the passed value based on the specified
   * wordsize and the value of the prefixFlag.
   *
   * @param val
   *          an <code>int</code> value
   * @param wordsize
   *          an <code>int</code> value
   * @param prefixFlag
   *          a <code>boolean</code> value
   * @return a <code>String</code> value
   */
  private static String paddedHexString(int val, int wordsize,
      boolean prefixFlag) {

    String prefix = prefixFlag ? "0x" : "";
    String hexVal = Integer.toHexString(val);

    if (hexVal.length() > wordsize)
      hexVal = hexVal.substring(hexVal.length() - wordsize);

    return (prefix + (wordsize > hexVal.length()
        ? ZEROS.substring(0, wordsize - hexVal.length())
        : "") + hexVal);
  }

}
