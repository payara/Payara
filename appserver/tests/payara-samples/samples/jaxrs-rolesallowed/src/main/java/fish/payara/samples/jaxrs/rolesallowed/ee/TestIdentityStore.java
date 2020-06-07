/** Copyright Payara Services Limited **/

package fish.payara.samples.jaxrs.rolesallowed.ee;

import static java.util.Arrays.asList;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;

import java.util.HashSet;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

/**
 * Test identity store that just provides a build-in user with name/password test/secret and roles a and b.
 *
 * @author Arjan Tijms
 */
@ApplicationScoped
public class TestIdentityStore implements IdentityStore {

    public CredentialValidationResult validate(UsernamePasswordCredential usernamePasswordCredential) {

        if (usernamePasswordCredential.compareTo("test", "secret")) {
            return new CredentialValidationResult("test", new HashSet<>(asList("a", "b")));
        }

        return INVALID_RESULT;
    }

}
