/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import org.omnifaces.jaccprovider.TestPolicy;

/**
 * Test policy used for easy testing that the policy is indeed used.
 *
 * <p>
 * This inherits from {@link TestPolicy}, which comes from an external dependency, and is a standalone JACC policy
 * used for testing as well. It implements the default Servlet authorization rules and is compatible with various
 * application servers.
 *
 * @author Arjan Tijms
 *
 */
public class LoggingTestPolicy extends TestPolicy {

    public static final ConcurrentLinkedQueue<Permission> loggedPermissions = new ConcurrentLinkedQueue<>();

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {

        if (permission instanceof WebResourcePermission || permission instanceof WebRoleRefPermission || permission instanceof WebUserDataPermission) {
            // Only for test! Don't log like this in an actual application!
            loggedPermissions.add(permission);
            System.out.println(permission);
        }

        return super.implies(domain, permission);
    }

}
