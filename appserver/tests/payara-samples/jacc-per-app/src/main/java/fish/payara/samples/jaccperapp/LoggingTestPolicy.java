/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebRoleRefPermission;
import jakarta.security.jacc.WebUserDataPermission;

import jakarta.security.jacc.Policy;
import javax.security.auth.Subject;

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
public class LoggingTestPolicy implements Policy {

    public static final ConcurrentLinkedQueue<Permission> loggedPermissions = new ConcurrentLinkedQueue<>();

    private final Policy originalPolicy;
    
    public LoggingTestPolicy(Policy originalPolicy) {
        this.originalPolicy = originalPolicy;
    }

    @Override
    public boolean implies(Permission permission, Subject subject) {

        if (permission instanceof WebResourcePermission || permission instanceof WebRoleRefPermission || permission instanceof WebUserDataPermission) {
            // Only for test! Don't log like this in an actual application!
            loggedPermissions.add(permission);
            System.out.println(permission);
        }

        return originalPolicy.implies(permission, subject);
    }

    @Override
    public PermissionCollection getPermissionCollection(Subject subject) {
        return originalPolicy.getPermissionCollection(subject);
    }

}
