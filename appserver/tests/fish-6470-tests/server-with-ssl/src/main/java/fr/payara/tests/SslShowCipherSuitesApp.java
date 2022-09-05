package fr.payara.tests;

import javax.annotation.security.*;
import javax.enterprise.context.*;
import javax.security.enterprise.authentication.mechanism.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@ApplicationScoped
@ApplicationPath("/ss")
@DeclareRoles({ "admin" })
@BasicAuthenticationMechanismDefinition(realmName = "admin-realm")
public class SslShowCipherSuitesApp extends Application
{
}
