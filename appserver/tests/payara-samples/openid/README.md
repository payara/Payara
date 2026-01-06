# A sample OpenId Connect client and server

This sample application demonstrates the usage of the Payara OpenId Connect API, namely the `@OpenIdAuthenticationDefinition` annotation.

This project builds 2 distinct applications:

* the client application - the main artifact, built in `target/openid.war`
* the server application openid-server - built in `target/server/openid.war`

## Build

Build both applications with the following command:

```
mvn clean install -DskipTests=true
```

## Deploy

Deploy the server app in `target/server/openid.war` under the openid-server context root and name. You can use the following asadmin command:

```
asadmin deploy  --contextroot openid-server --name openid-server target/server/openid.war
```

Deploy the client app. You can use the following asadmin command or deploy from your IDE:

```
asadmin deploy  target/openid.war
```

Then you can access http://localhost:8080/oauthtest/Unsecured to open the unsecured page. You can access http://localhost:8080/oauthtest/Secured to get authenticated via OpenID Connect automatically under a demo user account. You'll first see a page about receiving an access token. Then load the secured page again to see it.