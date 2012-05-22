package org.glassfish.security.services.impl.context;


import org.glassfish.security.services.api.common.Attributes;
import org.glassfish.security.services.api.context.SecurityContextService;
import org.glassfish.security.services.impl.common.AttributesImpl;

import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class SecurityContextServiceImpl implements SecurityContextService {

	private Attributes envAttributes = new AttributesImpl();
	
	public Attributes getEnvironmentAttributes() {
		return envAttributes;
	}

}
