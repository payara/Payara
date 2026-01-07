# Payara Samples: Custom LoginModule/Realm #

This sample shows how to set a `LoginModule/Realm` in Payara using an asadmin command, and how to implement a minimal
`LoginModule/Realm`.

A `LoginModule/Realm` is a Payara proprietary "identity store", which is essentially a database of users and optionally (hashes of) credentials and groups, with the ability to validate a user/credential.

An identity store essentially provides the `{caller,credential} -> authenticated` function. Note that an identity store *does not*
interact with the caller, which is the responsibility of the authentication mechanism (e.g. BASIC or FORM).

A LoginModule/Realm in Payara is primarily used by the Servlet build-in authentication mechanisms and remote EJB. 


## Samples ##

 - loginmodule-realm-impl - Implements a basic LoginModule/Realm that just recognizes a hardcoded user/password
 - loginmodule-realm-test - Uses asadmin to add the LoginModule/Realm from `loginmodule-realm-impl` to a remote Payara server, and calls a simple protected Servlet to validate that the `LoginModule/Realm` is indeed used.

