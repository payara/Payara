/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

if (typeof(admingui) == 'undefined') {
    admingui = {};
}
admingui.gadget = {
    noop: function() {
    },

    setResponse: function(response, rawData) {
	admingui.gadget.response = response;
	admingui.gadget.responseRaw = rawData;
    },

    /**
     *	handler - The name of the handler to invoke.
     *	args - An object containing properties / values for the parameters.
     *	callback - A JS function that should be notified.
     */
    invoke: function(handler, args, callback) {
	if ((callback == null) || (typeof(callback) === 'undefined')) {
	    callback = admingui.gadget.setResponse;
	}
	//return window.top.admingui.ajax.invoke(handler, args, callback, 3, false);
	//For now pass in true (asynchronous) b/c JSF2 Ajax is broken
	window.top.admingui.ajax.invoke(handler, args, callback, 3, true);
	return false;
    },

    getResponse: function() {
	return admingui.gadget.response;
    }
};

if (typeof(gadgets) == 'undefined') {
    // FIXME: I have to solve how to make these functions appear synchronous
    // FIXME: when they are asynchronous when backed by Ajax
    gadgets = {
	Prefs: function(moduleId) {
	    if (typeof(moduleId) == 'undefined') {
		moduleId = 'default';
	    }
	    // outputs (i.e. "value") are not used, but must be supplied
	    this.prefsHandlerOpts = {root: '/glassfish/gadget/' + moduleId, value:'gadgetPrefs'}
	    this.moduleId = (typeof(moduleId) == 'undefined') ? 'GlassFish' : moduleId;
	    this.getArray =
		function(key) {
		    // invoke Ajax to get preference
		    this.prefsHandlerOpts.key = key;
		    admingui.gadget.invoke("getPreference", this.prefsHandlerOpts);
		    var resp = admingui.gadget.getResponse().value;
		    if (typeof(resp) != 'object') {
			// Array's show up as Objects...
			resp = [resp];
		    }
		    return resp;
		};
	    this.getBool =
		/* true iff preference == 'true' */
		function(key) {
		    // invoke Ajax to get preference
		    this.prefsHandlerOpts.key = key;
		    admingui.gadget.invoke("getPreference", this.prefsHandlerOpts);
		    var resp = admingui.gadget.getResponse().value;
		    resp = (resp == 'true');
		    return resp;
		};
	    this.getCountry =
		function() {
		    // FIXME: Not implemented!
		    return "US";
		};
	    this.getFloat =
		function(key) {
		    // invoke Ajax to get preference
		    this.prefsHandlerOpts.key = key;
		    admingui.gadget.invoke("getPreference", this.prefsHandlerOpts);
		    var resp = admingui.gadget.getResponse().value;
		    resp = parseFloat(resp);
		    return resp;
		};
	    this.getInt =
		function(key) {
		    // invoke Ajax to get preference
		    this.prefsHandlerOpts.key = key;
		    admingui.gadget.invoke("getPreference", this.prefsHandlerOpts);
		    var resp = admingui.gadget.getResponse().value;
		    resp = parseInt(resp);
		    alert(typeof(resp));
		    return resp;
		};
	    this.getLang =
		function() {
		    // FIXME: Not implemented!
		    return "en";
		};
	    this.getModuleId =
		function() {
		    return this.moduleId;
		};
	    this.getMsg =
		function(key) {
		    alert('Prefs.getMsg() tbd...');
		    // FIXME: TBD...
		    // invoke Ajax to get the message
		    return key;
		};
	    this.getString =
		function(key) {
		    // invoke Ajax to get preference
		    this.prefsHandlerOpts.key = key;
		    admingui.gadget.invoke("getPreference", this.prefsHandlerOpts);
		    var resp = admingui.gadget.getResponse()
		    if (typeof(resp) != 'undefined') {
			resp = resp.value;
		    }
		    return resp;
		};
	    this.set =
		function(key, val) {
		    // invoke Ajax to set preference
		    this.prefsHandlerOpts.key = key;
		    this.prefsHandlerOpts.value = val;
		    admingui.gadget.invoke("setPreference", this.prefsHandlerOpts);
		};
	    this.setArray =
		function(key, val) {
		    // invoke Ajax to get preference
		    if (typeof(val) != 'object') {
			val = [val];
		    }
		    this.prefsHandlerOpts.key = key;
		    this.prefsHandlerOpts.value = val;
		    admingui.gadget.invoke("setPreference", this.prefsHandlerOpts);
		};
	}
    };
}
