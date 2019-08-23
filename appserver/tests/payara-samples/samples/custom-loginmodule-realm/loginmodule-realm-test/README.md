# Payara Samples: Custom LoginModule/Realm - Test #

This sample installs a `LoginModule/Realm` using the asadmin command.

The procedure to fully install such `LoginModule/Realm` is by copying a jar file containing its class files to the `[payara home]/glassfish/lib` folder, registering the `LoginModule` part in `[payara home]/glassfish/domains[domain]/config/login.conf` and the `Realm` part in `[payara home]/glassfish/domains[domain]/config/domain.xml`. The two are linked via the `jaas-context` property that is set on the realm, and which refers to the name the `LoginModule` is registered under in `login.conf`.

The following shows an example:

**login.conf**

```
customRealm {
	fish.payara.samples.loginmodule.realm.custom.CustomLoginModule required;
};
```

**domain.xml**

```xml
<auth-realm name="custom" classname="fish.payara.samples.loginmodule.realm.custom.CustomRealm" >
    <property name="jaas-context" value="customRealm"></property>
</auth-realm>
```

Note that a `Realm` can be removed again, but that the `LoginModule` registration sticks around. The user has to manually remove this from `login.conf`.