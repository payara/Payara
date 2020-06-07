/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.catalina.util;

import org.w3c.dom.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * A sample DOM writer. This sample program illustrates how to
 * traverse a DOM tree in order to print a document that is parsed.
 */
public class DOMWriter {

   //
   // Data
   //

   /** Default Encoding */
   private static  String
   PRINTWRITER_ENCODING = "UTF8";

   private static String MIME2JAVA_ENCODINGS[] =
    { "Default", "UTF-8", "US-ASCII", "ISO-8859-1", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4",
      "ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8", "ISO-8859-9", "ISO-2022-JP",
      "SHIFT_JIS", "EUC-JP","GB2312", "BIG5", "EUC-KR", "ISO-2022-KR", "KOI8-R", "EBCDIC-CP-US",
      "EBCDIC-CP-CA", "EBCDIC-CP-NL", "EBCDIC-CP-DK", "EBCDIC-CP-NO", "EBCDIC-CP-FI", "EBCDIC-CP-SE",
      "EBCDIC-CP-IT", "EBCDIC-CP-ES", "EBCDIC-CP-GB", "EBCDIC-CP-FR", "EBCDIC-CP-AR1",
      "EBCDIC-CP-HE", "EBCDIC-CP-CH", "EBCDIC-CP-ROECE","EBCDIC-CP-YU",
      "EBCDIC-CP-IS", "EBCDIC-CP-AR2", "UTF-16"
    };

   /** Output qualified names */
   private boolean qualifiedNames = true;

   /** Print writer. */
   protected PrintWriter out;

   /** Canonical output. */
   protected boolean canonical;


   public DOMWriter(String encoding, boolean canonical)
   throws UnsupportedEncodingException {
      out = new PrintWriter(new OutputStreamWriter(System.out, encoding));
      this.canonical = canonical;
   } // <init>(String,boolean)

   //
   // Constructors
   //

   /** Default constructor. */
   public DOMWriter(boolean canonical) throws UnsupportedEncodingException {
      this( getWriterEncoding(), canonical);
   }

    public DOMWriter(Writer writer, boolean canonical) {
        out = new PrintWriter(writer);
        this.canonical = canonical;
    }

   public boolean getQualifiedNames() {
      return this.qualifiedNames;
   }

   public void setQualifiedNames(boolean qualifiedNames) {
      this.qualifiedNames = qualifiedNames;
   }

   public static String getWriterEncoding( ) {
      return (PRINTWRITER_ENCODING);
   }// getWriterEncoding

   public static void  setWriterEncoding( String encoding ) {
      if( encoding.equalsIgnoreCase( "DEFAULT" ) )
         PRINTWRITER_ENCODING  = "UTF8";
      else if( encoding.equalsIgnoreCase( "UTF-16" ) )
         PRINTWRITER_ENCODING  = "Unicode";
      else
         PRINTWRITER_ENCODING = MIME2Java.convert( encoding );
   }// setWriterEncoding


   public static boolean isValidJavaEncoding( String encoding ) {
       for (String MIME2JAVA_ENCODINGS1 : MIME2JAVA_ENCODINGS) {
           if (encoding.equals(MIME2JAVA_ENCODINGS1)) {
               return (true);
           }
       }

      return (false);
   }// isValidJavaEncoding


   /** Prints the specified node, recursively. */
   public void print(Node node) {

      // is there anything to do?
      if ( node == null ) {
         return;
      }

      int type = node.getNodeType();
      switch ( type ) {
         // print document
         case Node.DOCUMENT_NODE: {
               if ( !canonical ) {
                  String  Encoding = getWriterEncoding();
                  if( Encoding.equalsIgnoreCase( "DEFAULT" ) )
                     Encoding = "UTF-8";
                  else if( Encoding.equalsIgnoreCase( "Unicode" ) )
                     Encoding = "UTF-16";
                  else
                     Encoding = MIME2Java.reverse( Encoding );

                  out.println("<?xml version=\"1.0\" encoding=\""+
                           Encoding + "\"?>");
               }
               print(((Document)node).getDocumentElement());
               out.flush();
               break;
            }

            // print element with attributes
         case Node.ELEMENT_NODE: {
               out.print('<');
               if (this.qualifiedNames) { 
                  out.print(node.getNodeName());
               } else {
                  out.print(node.getLocalName());
               }
               Attr attrs[] = sortAttributes(node.getAttributes());
               for ( int i = 0; i < attrs.length; i++ ) {
                  Attr attr = attrs[i];
                  out.print(' ');
                  if (this.qualifiedNames) {
                     out.print(attr.getNodeName());
                  } else {
                     out.print(attr.getLocalName());
                  }
                  
                  out.print("=\"");
                  out.print(normalize(attr.getNodeValue()));
                  out.print('"');
               }
               out.print('>');
               NodeList children = node.getChildNodes();
               if ( children != null ) {
                  int len = children.getLength();
                  for ( int i = 0; i < len; i++ ) {
                     print(children.item(i));
                  }
               }
               break;
            }

            // handle entity reference nodes
         case Node.ENTITY_REFERENCE_NODE: {
               if ( canonical ) {
                  NodeList children = node.getChildNodes();
                  if ( children != null ) {
                     int len = children.getLength();
                     for ( int i = 0; i < len; i++ ) {
                        print(children.item(i));
                     }
                  }
               } else {
                  out.print('&');
                  if (this.qualifiedNames) {
                     out.print(node.getNodeName());
                  } else {
                     out.print(node.getLocalName());
                  }
                  out.print(';');
               }
               break;
            }

            // print cdata sections
         case Node.CDATA_SECTION_NODE: {
               if ( canonical ) {
                  out.print(normalize(node.getNodeValue()));
               } else {
                  out.print("<![CDATA[");
                  out.print(node.getNodeValue());
                  out.print("]]>");
               }
               break;
            }

            // print text
         case Node.TEXT_NODE: {
               out.print(normalize(node.getNodeValue()));
               break;
            }

            // print processing instruction
         case Node.PROCESSING_INSTRUCTION_NODE: {
               out.print("<?");
               if (this.qualifiedNames) {
                  out.print(node.getNodeName());
               } else {
                  out.print(node.getLocalName());
               }
               
               String data = node.getNodeValue();
               if ( data != null && data.length() > 0 ) {
                  out.print(' ');
                  out.print(data);
               }
               out.print("?>");
               break;
            }
      }

      if ( type == Node.ELEMENT_NODE ) {
         out.print("</");
         if (this.qualifiedNames) {
            out.print(node.getNodeName());
         } else {
            out.print(node.getLocalName());
         }
         out.print('>');
      }

      out.flush();

   } // print(Node)

   /** Returns a sorted list of attributes. */
   protected Attr[] sortAttributes(NamedNodeMap attrs) {

      int len = (attrs != null) ? attrs.getLength() : 0;
      Attr array[] = new Attr[len];
      for ( int i = 0; i < len; i++ ) {
         array[i] = (Attr)attrs.item(i);
      }
      for ( int i = 0; i < len - 1; i++ ) {
         String name = null;
         if (this.qualifiedNames) {
            name  = array[i].getNodeName();
         } else {
            name  = array[i].getLocalName();
         }
         int    index = i;
         for ( int j = i + 1; j < len; j++ ) {
            String curName = null;
            if (this.qualifiedNames) {
               curName = array[j].getNodeName();
            } else {
               curName = array[j].getLocalName();
            }
            if ( curName.compareTo(name) < 0 ) {
               name  = curName;
               index = j;
            }
         }
         if ( index != i ) {
            Attr temp    = array[i];
            array[i]     = array[index];
            array[index] = temp;
         }
      }

      return (array);

   } // sortAttributes(NamedNodeMap):Attr[]


   /** Normalizes the given string. */
   protected String normalize(String s) {
      StringBuilder str = new StringBuilder();

      int len = (s != null) ? s.length() : 0;
      for ( int i = 0; i < len; i++ ) {
         char ch = s.charAt(i);
         switch ( ch ) {
            case '<': {
                  str.append("&lt;");
                  break;
               }
            case '>': {
                  str.append("&gt;");
                  break;
               }
            case '&': {
                  str.append("&amp;");
                  break;
               }
            case '"': {
                  str.append("&quot;");
                  break;
               }
            case '\r':
            case '\n': {
                  if ( canonical ) {
                     str.append("&#");
                     str.append(Integer.toString(ch));
                     str.append(';');
                     break;
                  }
                  // else, default append char
               }
            default: {
                  str.append(ch);
               }
         }
      }

      return (str.toString());

   } // normalize(String):String

   /*
   private static void printValidJavaEncoding() {
      System.err.println( "    ENCODINGS:" );
      System.err.print( "   " );
      for( int i = 0;
                     i < MIME2JAVA_ENCODINGS.length; i++) {
         System.err.print( MIME2JAVA_ENCODINGS[i] + " " );
      if( (i % 7 ) == 0 ){
         System.err.println();
         System.err.print( "   " );
         }
      }

   } // printJavaEncoding()
   */

}
