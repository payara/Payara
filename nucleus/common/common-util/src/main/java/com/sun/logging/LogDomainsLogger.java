// Portions Copyright [2015] [C2B2 Consulting Limited]
package com.sun.logging;

import java.util.ResourceBundle;
import java.util.logging.Logger;

class LogDomainsLogger extends Logger {
	private final ResourceBundle resourceBundle;

	LogDomainsLogger(final String loggerName, final ResourceBundle resourceBundle) {
		super(loggerName, null);
		this.resourceBundle = resourceBundle;
	}

	@Override
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
}
