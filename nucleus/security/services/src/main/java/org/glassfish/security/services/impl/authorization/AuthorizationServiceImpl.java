package org.glassfish.security.services.impl.authorization;

import java.net.URI;
import java.security.Permission;

import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzEnvironment;
import org.glassfish.security.services.api.authorization.AzObligations;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.impl.AuthenticationServiceFactory;

import javax.security.auth.Subject;

import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class AuthorizationServiceImpl implements AuthorizationService {

	public boolean isPermissionGranted(Subject subject, Permission permission) {
		return true;
	}

	public boolean isAuthorized(Subject subject, URI resource) {
		return true;
	}

	public boolean isAuthorized(Subject subject, URI resource, String action) {
		return true;
	}

	public AzResult getAuthorizationDecision(AzSubject subject,
			AzResource resource, AzAction action) {
		return null;
	}

	public AzSubject makeAzSubject(Subject subject) {
		return null;
	}

	public AzResource makeAzResource(URI resource) {
		return null;
	}

	public AzAction makeAzAction(String action) {
		return null;
	}

	public PolicyDeploymentContext findOrCreateDeploymentContext(
			String appContext) {
		return null;
	}
	

}