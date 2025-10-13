/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.universal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Base64;

import org.junit.Test;

/**
 *
 * @author bnevins
 */
public class BASE64DecoderTest {
    
    private static final String[] testStrings = new String[] { "foo", "QQ234bbVVc", "\n\n\r\f\n" };
    
    /* 
     * Make sure the JDK base64 encoding/decoding works
     */
    @Test
    public void testEncodeDecode() throws IOException{
        Base64.Encoder encoder = Base64.getMimeEncoder();
        Base64.Decoder decoder = Base64.getMimeDecoder();
        
        for (String testString : testStrings) {
            String enc = new String(encoder.encode(testString.getBytes()), UTF_8);
            assertFalse(enc.equals(testString));
            
            String dec = new String(decoder.decode(enc), UTF_8);
            assertEquals(dec, testString);
        }
    }
   
    @Test
    public void testEncodeDecodeAgainstSun() throws IOException{
       
    }
    
    
}
