package fish.payara.tests;

import javax.enterprise.context.*;
import javax.security.enterprise.credential.*;
import javax.security.enterprise.identitystore.*;
import java.util.*;

import static javax.security.enterprise.identitystore.CredentialValidationResult.*;

@ApplicationScoped
public class InMemoryIdentityStore implements IdentityStore
{
  public CredentialValidationResult validate(
    UsernamePasswordCredential usernamePasswordCredential)
  {
    return usernamePasswordCredential.compareTo("admin", "admin") ?
      new CredentialValidationResult("admin",
      new HashSet<>(Arrays.asList("admin"))) : INVALID_RESULT;
  }
}
