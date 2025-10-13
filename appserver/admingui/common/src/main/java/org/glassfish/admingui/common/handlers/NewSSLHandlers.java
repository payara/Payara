/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

/*
 * SSLHandlers.java
 *
 * Created on June 25, 2009, 11:30 PM
 *
 */

package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import org.glassfish.admingui.common.util.GuiUtil;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author anilam
 *
 */
public class NewSSLHandlers {

    private static Set<String> COMMON_CIPHERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
    )));

    private static Set<String> BIT_CIPHERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
    )));

    public NewSSLHandlers() {
    }

    @Handler(id="convertToDifferentCiphersGroup",
    input={
        @HandlerInput(name="ciphers")},
    output={
        @HandlerOutput(name="CommonCiphersList",    type=String[].class),
        @HandlerOutput(name="EphemeralCiphersList", type=String[].class),
        @HandlerOutput(name="OtherCiphersList",     type=String[].class),
        @HandlerOutput(name="EccCiphersList",       type=String[].class)}
    )
    public static void convertToDifferentCiphersGroup(HandlerContext handlerCtx) {
        List<String> ciphersList = null;
        Object ciphers = handlerCtx.getInputValue("ciphers");
        if (ciphers != null) {
            if (ciphers instanceof String) {
                ciphersList = Arrays.asList(getSelectedCiphersList((String) ciphers));
            } else {
                ciphersList = Arrays.asList((String[]) ciphers);
            }
        }
        handlerCtx.setOutputValue("CommonCiphersList", getCommonCiphers(ciphersList));
        handlerCtx.setOutputValue("EphemeralCiphersList", getEphemeralCiphers(ciphersList));
        handlerCtx.setOutputValue("OtherCiphersList", getOtherCiphers(ciphersList));
        handlerCtx.setOutputValue("EccCiphersList", getEccCiphers(ciphersList));
    }


    @Handler(id="convertCiphersItemsToStr",
    input={
        @HandlerInput(name="common",    type=String[].class),
        @HandlerInput(name="ephemeral", type=String[].class),
        @HandlerInput(name="other",     type=String[].class),
        @HandlerInput(name="ecc",       type=String[].class)},
    output={
        @HandlerOutput(name="ciphers")}
    )
    public static void convertCiphersItemsToStr(HandlerContext handlerCtx) {

        String[] common = (String[])handlerCtx.getInputValue("common");
        String[] ephemeral = (String[])handlerCtx.getInputValue("ephemeral");
        String[] other = (String[])handlerCtx.getInputValue("other");
        String[] ecc = (String[])handlerCtx.getInputValue("ecc");

        String ciphers = processSelectedCiphers(common, "");
        ciphers = processSelectedCiphers(ephemeral, ciphers);
        ciphers = processSelectedCiphers(other, ciphers);
        ciphers = processSelectedCiphers(ecc, ciphers);

        handlerCtx.setOutputValue("ciphers", ciphers);
    }

    private static String[] getSelectedCiphersList(String selectedCiphers){
        if(selectedCiphers != null){
            return filterCiphers(
                    Arrays.asList(selectedCiphers.split(",")),
                    cipher -> cipher.startsWith("+"),
                    cipher -> cipher.substring(1));
        }
        return new String[0];
    }

    private static String processSelectedCiphers(String[] selectedCiphers, String ciphers){
        StringBuilder sb = new StringBuilder();
        String sep = "";
        if ( ! GuiUtil.isEmpty(ciphers)){
            sb.append(ciphers);
            sep = ",";
        }
        if(selectedCiphers != null){
            for (String selectedCipher : selectedCiphers) {
                sb.append(sep).append("+").append(selectedCipher);
                sep = ",";
            }
        }
        return sb.toString();
    }

    private static String[] getCommonCiphers(List<String> ciphers){
        return filterCiphers(ciphers, cypher -> COMMON_CIPHERS.contains(cypher));
    }

    private static String[] getEccCiphers(List<String> ciphers){
        return filterCiphers(
                ciphers,
                cipher -> !BIT_CIPHERS.contains(cipher) && (cipher.contains("_ECDH_") || cipher.contains("_ECDHE_"))
        );
    }

    private static String[] getEphemeralCiphers(List<String> ciphers){
        return filterCiphers(
                ciphers,
                cipher -> !BIT_CIPHERS.contains(cipher) && (cipher.contains("_DHE_RSA_") || cipher.contains("_DHE_DSS_"))
        );
    }

    private static String[] getOtherCiphers(List<String> ciphers){
        return filterCiphers(ciphers, cypher -> BIT_CIPHERS.contains(cypher));
    }

    private static String[] filterCiphers(List<String> ciphers, Predicate<String> filter){
        return filterCiphers(ciphers, filter, str -> str);
    }

    private static String[] filterCiphers(List<String> ciphers, Predicate<String> filter, Function<String, String> transformation){
        if (ciphers != null) {
            return ciphers.stream()
                    .filter(filter)
                    .map(transformation)
                    .toArray(String[]::new);
        }
        return new String[0];
    }

}
