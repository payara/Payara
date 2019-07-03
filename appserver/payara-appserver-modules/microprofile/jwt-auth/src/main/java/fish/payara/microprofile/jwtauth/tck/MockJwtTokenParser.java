/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.jwtauth.tck;

import static com.nimbusds.jose.JWSAlgorithm.RS256;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import java.io.StringReader;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import static javax.json.Json.createObjectBuilder;
import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.eclipse.microprofile.jwt.Claims;
import static org.eclipse.microprofile.jwt.Claims.exp;
import static org.eclipse.microprofile.jwt.Claims.groups;
import static org.eclipse.microprofile.jwt.Claims.iat;
import static org.eclipse.microprofile.jwt.Claims.iss;
import static org.eclipse.microprofile.jwt.Claims.jti;
import static org.eclipse.microprofile.jwt.Claims.preferred_username;
import static org.eclipse.microprofile.jwt.Claims.raw_token;
import static org.eclipse.microprofile.jwt.Claims.sub;
import static org.eclipse.microprofile.jwt.Claims.upn;

/**
 *
 * @author AlanRoth
 */
public class MockJwtTokenParser {
    private final static String DEFAULT_NAMESPACE = "https://payara.fish/mp-jwt/";
    
    private final List<Claims> requiredClaims = asList(iss, sub, exp, iat, jti, groups);
    
    private final boolean enableNamespacedClaims;
    private final Optional<String> customNamespace;

    private String rawToken;
    private SignedJWT signedJWT;
    
    public MockJwtTokenParser(Optional<Boolean> enableNamespacedClaims, Optional<String> customNamespace) {
        this.enableNamespacedClaims = enableNamespacedClaims.orElse(false);
        this.customNamespace = customNamespace;
    }

    public MockJwtTokenParser() {
        this(Optional.empty(), Optional.empty());
    }
    
    public void parse(String bearerToken) throws Exception {
        rawToken = bearerToken;
        signedJWT = SignedJWT.parse(rawToken);
        
        if(!checkIsJWT(signedJWT.getHeader())){
            throw new IllegalStateException("Not JWT");
        }
    }
    
    public JsonWebTokenImpl verify(String issuer, PublicKey publicKey) throws Exception {
        if(signedJWT == null){
            parse(rawToken);
        }

        // MP-JWT 1.0 4.1 typ
        if (!checkIsJWT(signedJWT.getHeader())) {
            throw new IllegalStateException("Not JWT");
        }

        // 1.0 4.1 alg + MP-JWT 1.0 6.1 1
        if (!signedJWT.getHeader().getAlgorithm().equals(RS256)) {
            throw new IllegalStateException("Not RS256");
        }

        try (JsonReader reader = Json.createReader(new StringReader(signedJWT.getPayload().toString()))) {
            Map<String, JsonValue> rawClaims = new HashMap<>(reader.readObject());
            
            // Vendor - Process namespaced claims
            rawClaims = handleNamespacedClaims(rawClaims);
            
            // MP-JWT 1.0 4.1 Minimum MP-JWT Required Claims
            if (!checkRequiredClaimsPresent(rawClaims)) {
                throw new IllegalStateException("Not all required claims present");
            }

            // MP-JWT 1.0 4.1 upn - has fallbacks
            String callerPrincipalName = getCallerPrincipalName(rawClaims);
            if (callerPrincipalName == null) {
                throw new IllegalStateException("One of upn, preferred_username or sub is required to be non null");
            }

            // MP-JWT 1.0 6.1 2
            if (!checkIssuer(rawClaims, issuer)) {
                throw new IllegalStateException("Bad issuer");
            }

            if (!checkNotExpired(rawClaims)) {
                throw new IllegalStateException("Expired");
            }

            // MP-JWT 1.0 6.1 2
            if (!signedJWT.verify(new RSASSAVerifier((RSAPublicKey) publicKey))) {
                throw new IllegalStateException("Signature invalid");
            }

            rawClaims.put(
                    raw_token.name(),
                    createObjectBuilder().add("token", rawToken).build().get("token"));

            return new JsonWebTokenImpl(callerPrincipalName, rawClaims);
        }
    }
    
    private Map<String, JsonValue> handleNamespacedClaims(Map<String, JsonValue> currentClaims){
        if(this.enableNamespacedClaims){
            final String namespace = customNamespace.orElse(DEFAULT_NAMESPACE);
            Map<String, JsonValue> processedClaims = new HashMap<>(currentClaims.size());
            for(String claimName : currentClaims.keySet()){
                JsonValue value = currentClaims.get(claimName);
                if(claimName.startsWith(namespace)){
                    claimName = claimName.substring(namespace.length());
                }
                processedClaims.put(claimName, value);
            }
            return processedClaims;
        }else{
            return currentClaims;
        }
    }
        
    private boolean checkRequiredClaimsPresent(Map<String, JsonValue> presentedClaims) {
        for (Claims requiredClaim : requiredClaims) {
            if (presentedClaims.get(requiredClaim.name()) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean checkNotExpired(Map<String, JsonValue> presentedClaims) {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        int expiredTime = ((JsonNumber) presentedClaims.get(exp.name())).intValue();

        return currentTime < expiredTime;
    }

    private boolean checkIssuer(Map<String, JsonValue> presentedClaims, String acceptedIssuer) {
        if (!(presentedClaims.get(iss.name()) instanceof JsonString)) {
            return false;
        }

        String issuer = ((JsonString) presentedClaims.get(iss.name())).getString();

        // TODO: make acceptedIssuers (set)
        return acceptedIssuer.equals(issuer);
    }

    private boolean checkIsJWT(JWSHeader header) {
        return header.getType().toString().equals("JWT");
    }

    private String getCallerPrincipalName(Map<String, JsonValue> rawClaims) {
        JsonString callerPrincipalClaim = (JsonString) rawClaims.get(upn.name());

        if (callerPrincipalClaim == null) {
            callerPrincipalClaim = (JsonString) rawClaims.get(preferred_username.name());
        }

        if (callerPrincipalClaim == null) {
            callerPrincipalClaim = (JsonString) rawClaims.get(sub.name());
        }

        if (callerPrincipalClaim == null) {
            return null;
        }

        return callerPrincipalClaim.getString();
    }
}
