/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.identitystores;

import javax.enterprise.util.AnnotationLiteral;
import org.glassfish.soteria.cdi.AnnotationELPProcessor;
import static org.glassfish.soteria.cdi.AnnotationELPProcessor.evalImmediate;

/**
 * A literal object of the YubikeyCloudIdentityStoreDefinition annotation
 * @author Mark Wareham
 */
public class YubikeyIdentityStoreDefinitionAnnotationLiteral
        extends AnnotationLiteral<YubikeyIdentityStoreDefinition>
        implements YubikeyIdentityStoreDefinition {

    private String yubikeyAPIClientID;
    private String yubikeyAPIKey;
    private String serverType;
    private String serverEndpoint;
    private boolean hasDeferredExpressions;

    public YubikeyIdentityStoreDefinitionAnnotationLiteral(
            String yubikeyAPIClientID, String yubikeyAPIKey, String serverType, 
            String serverEndpoint) {
        this.yubikeyAPIClientID = yubikeyAPIClientID;
        this.yubikeyAPIKey = yubikeyAPIKey;
        this.serverType = serverType;
        this.serverEndpoint = serverEndpoint;
    }

    /**
     * Evaluates the provided object's fields for EL expressions
     * @param in the object to be evaluated
     * @return an evaluated object
     */
    public static YubikeyIdentityStoreDefinition eval(YubikeyIdentityStoreDefinition in) {
        //If any of the fields do not contain an EL expression
        if (!hasAnyELExpression(in)) {
            return in;
        }

        YubikeyIdentityStoreDefinitionAnnotationLiteral out
                = new YubikeyIdentityStoreDefinitionAnnotationLiteral(
                        evalImmediate(in.yubikeyAPIClientID()), 
                        evalImmediate(in.yubikeyAPIKey()),
                        evalImmediate(in.serverType()),
                        evalImmediate(in.serverEnpoint()));

        out.setHasDeferredExpressions(hasAnyELExpression(out));

        return out;
    }

    public static boolean hasAnyELExpression(YubikeyIdentityStoreDefinition in) {
        return AnnotationELPProcessor.hasAnyELExpression(in.yubikeyAPIClientID()) 
                &&
                AnnotationELPProcessor.hasAnyELExpression(in.yubikeyAPIKey());
    }

    public void setHasDeferredExpressions(boolean hasDeferredExpressions) {
        this.hasDeferredExpressions = hasDeferredExpressions;
    }

    @Override
    public String yubikeyAPIClientID() {
        return yubikeyAPIClientID;
    }

    @Override
    public String yubikeyAPIKey() {
        return yubikeyAPIKey;
    }

    @Override
    public String serverType() {
        return serverType;
    }

    @Override
    public String serverEnpoint() {
        return serverEndpoint;
    }

}
