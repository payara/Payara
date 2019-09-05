# Payara Samples: Custom LoginModule/Realm - Implementation #

This sample implements a basic `LoginModule/Realm` that just recognizes a hardcoded user/password. 

A `LoginModule/Realm` is a Payara proprietary "identity store", which is essentially a database of users and optionally (hashes of) credentials and groups, with the ability to validate a user/credential.

A `LoginModule/Realm` in Payara is primarily used by the Servlet build-in authentication mechanisms and remote EJB.

A `LoginModule/Realm` consists out of two clases; the `LoginModule` and the `Realm`. 

The `LoginModule` is a JAAS artifact, though it uses Payara proprietary APIs to get the user and credentials and to commit the result. Therefor, despite using the standard interface and depending upon the standard `LoginContext`, this is NOT compatible with non-Payara login modules. In Payara, a `LoginModule` *MUST* delegate to a `Realm`.

The `Realm` is Payara proprietary type, which is essentially a `LoginModule`, but with extra methods for user management (adding, updating, deleting users). Not all (in practice actually very few) realms actually make use of the user management features. Note that a Payara `Realm` is not to be mistaken with a Tomcat `Realm`, which despite the same name and also used for security is a different thing.




