package org.glassfish.security.services.impl.authorization;

import org.glassfish.security.services.api.authorization.AzObligations;
import org.glassfish.security.services.api.authorization.AzResult;

public final class AzResultImpl implements AzResult {
	
	private Decision decision;
	private Status status;
	private AzObligations obligations = null;
	
	private AzResultImpl() {}
	
	protected AzResultImpl(Decision d, Status s, AzObligations o) {
		decision = d;
		status = s;
		obligations = o;
	}

	public Decision getDecision() {
		return decision;
	}

	public Status getStatus() {
		return status;
	}

	public AzObligations getObligations() {
		return obligations;
	}

}
