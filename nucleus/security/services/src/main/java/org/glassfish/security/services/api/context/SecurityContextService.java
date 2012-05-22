package org.glassfish.security.services.api.context;


import org.glassfish.security.services.api.common.Attributes;

import org.jvnet.hk2.annotations.Contract;

/**
 * The Security Context Service maintains context needed by various security
 * services.  It is scoped per-thread (though this does not preclude it from
 * providing access to context that has different scope).
 */

@Contract
public interface SecurityContextService {
	
	/**
	 * Return the Environment attributes collection associated with the current thread.
	 * 
	 * @return The environment attributes.
	 */
	public Attributes getEnvironmentAttributes();
	
}
