/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.jwtauth.jwt;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.security.enterprise.CallerPrincipal;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * A default implementation of {@link JsonWebToken}.
 * 
 * @author Arjan Tijms
 */
public class JsonWebTokenImpl extends CallerPrincipal implements JsonWebToken {
    
    private final Map<String, JsonValue> claims;

    public JsonWebTokenImpl(String callerName, Map<String, JsonValue> claims) {
        super(callerName);
        this.claims = claims;
    }
    
    public Map<String, JsonValue> getClaims() {
        return claims;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClaim(String claimName) {
        
        JsonValue claimValue = getClaims().get(claimName);
        if (claimValue == null) {
            return null;
        }
        
        try {
            Claims claim = Claims.valueOf(claimName);
            
            if (claim.getType().equals(Long.class)) {
                return (T) (Long) ((JsonNumber) claimValue).longValue();
            }
            
            if (claim.getType().equals(Set.class)) {
                if (claimValue instanceof JsonString) {
                    return (T) singleton(((JsonString) claimValue).getString());
                } else {
                    return (T) asStringSet((JsonArray) claimValue);
                }
            }
            
        } catch (IllegalArgumentException e) {
            // ignore, not an enum
        }
        
        // All JsonValue are returned as their JsonValue sub-type, except for JsonString, which
        // is always returned as string.
        // See org.eclipse.microprofile.jwt.tck.parsing.TestTokenClaimTypesTest.validateCustomString(TestTokenClaimTypesTest.java:180)
        if (claimValue instanceof JsonString) {
            return (T) ((JsonString) claimValue).getString();
        }
        
        return (T) getClaims().get(claimName);
    }
    
    @Override
    public Set<String> getClaimNames() {
        return getClaims().keySet();
    }
    
    private static Set<String> asStringSet(JsonArray jsonArray) {
        return new HashSet<>((jsonArray).getValuesAs(JsonString.class))
                                        .stream().map(t -> t.getString())
                                        .collect(toSet());
    }

}
