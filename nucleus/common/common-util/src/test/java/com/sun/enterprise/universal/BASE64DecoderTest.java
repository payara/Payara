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

package com.sun.enterprise.universal;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class BASE64DecoderTest {
    /* 
     * make sure the Universal base64 works
     */
    @Test
    public void testEncodeDecode() throws IOException{
        GFBase64Encoder encoder = new GFBase64Encoder();
        GFBase64Decoder decoder = new GFBase64Decoder();
        
        for(String s : ss) {
            byte[] stringAsByteBuf = s.getBytes();
            String enc = encoder.encode(stringAsByteBuf);
            assertFalse(enc.equals(s));
            byte[] decodedByteBuf = decoder.decodeBuffer(enc);
            String dec = new String(decodedByteBuf);
            assertEquals(dec, s);
        }
    }
    
    /* make sure the Universal base64 results match sun.misc
     */
    @Test
    public void testEncodeDecodeAgainstSun() throws IOException{
        com.sun.enterprise.universal.GFBase64Encoder gfEncoder = 
                new com.sun.enterprise.universal.GFBase64Encoder();
        com.sun.enterprise.universal.GFBase64Decoder gfDecoder = 
                new com.sun.enterprise.universal.GFBase64Decoder();
        sun.misc.BASE64Decoder sunDecoder = new sun.misc.BASE64Decoder();
        sun.misc.BASE64Encoder sunEncoder = new sun.misc.BASE64Encoder();
        
        for(String s : ss) {
            byte[] stringAsByteBuf = s.getBytes();
            String gfEnc = gfEncoder.encode(stringAsByteBuf);
            String sunEnc = sunEncoder.encode(stringAsByteBuf);
            
            assertEquals(gfEnc, sunEnc);
            
            byte[] gfDecodedByteBuf = gfDecoder.decodeBuffer(gfEnc);
            byte[] sunDecodedByteBuf = sunDecoder.decodeBuffer(sunEnc);
            
            assertTrue(gfDecodedByteBuf.length == sunDecodedByteBuf.length);
            
            for(int i = 0; i < gfDecodedByteBuf.length; i++)
                assertEquals(gfDecodedByteBuf[i], sunDecodedByteBuf[i]);

            String gfDec = new String(gfDecodedByteBuf);
            String sunDec = new String(sunDecodedByteBuf);
            assertEquals(gfDec, s);
            assertEquals(gfDec, sunDec);
        }
    }
    
    private static final String[] ss = new String[]
    {
        "foo", "QQ234bbVVc", "\n\n\r\f\n"
    };
}
