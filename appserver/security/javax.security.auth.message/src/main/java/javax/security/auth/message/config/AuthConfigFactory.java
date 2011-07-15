/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.message.config;

import java.security.Permission;
import java.security.Security;
import java.security.SecurityPermission;

import java.util.Map;
// for @see
import java.util.Properties;

import javax.security.auth.message.*;

/**
 * This class is used to obtain <code>AuthConfigProvider</code> objects 
 * that can be used to obtain authentication context configuration objects,
 * that is, <code>ClientAuthConfig</code> and <code>ServerAuthConfig</code>
 * objects.
 * <p>Authentication context configuration objects are used to obtain
 * authentication context objects. Authentication context objects,
 * that is, <code>ClientAuthContext</code> and <code>ServerAuthContex</code>
 * objects, encapsulate authentication modules. Authentication modules are 
 * pluggable components that perform security-related processing of
 * request and response messages.
 *
 * <p> Callers do not operate on modules directly.
 * Instead they rely on an authentication context to manage the 
 * invocation of modules.  A caller obtains an authentication context
 * by calling the <code>getAuthContext</code> method on a 
 * <code>ClientAuthConfig</code> or <code>ServerAuthConfig</code> 
 * obtained from an AuthConfigProvider.
 *
 * <p> The following represents a typical sequence of calls for obtaining
 * a client authentication context, and then using it to secure 
 * a request.
 * <ol>
 * <li>AuthConfigFactory factory = AuthConfigFactory.getFactory();
 * <li>AuthConfigProvider provider = factory.getConfigProvider(layer,appID,listener);
 * <li>ClientAuthConfig config = provider.getClientAuthConfig(layer,appID,cbh)
 * <li>String authContextID = config.getAuthContextID(messageInfo);
 * <li>ClientAuthContext context = config.getAuthContext(authContextID,subject,properties);
 * <li>context.secureRequest(messageInfo,subject);
 * </ol>
 *
 * <p> A system-wide AuthConfigFactory implementation can be set by invoking
 * <code>setFactory</code>, and retrieved using <code>getFactory</code>.
 *
 * <p> Every implementation of this abstract class must offer a public,
 * zero argument constructor. This constructor must support the construction
 * and registration (including self-registration) of AuthConfigProviders 
 * from a persistent declarative representation.
 * For example, a factory implementation class could interpret the contents
 * of a file containing a sequence of configuration entries, with one entry per
 * AuthConfigProvider, and with each entry representing:
 * <ul>
 * <li> The fully qualified name of the provider implementation class (or null)
 * <li> The list of provider initialization properties (which could be empty)
 * </ul>
 * Any provider initialization properties must be specified in a 
 * form that can be passed to the provider constructor within a Map of key, 
 * value pairs, and where all keys and values within the Map are of type 
 * String.
 * <p>
 * The entry syntax must also provide for the optional inclusion of information
 * sufficient to define a RegistrationContext. This information would only 
 * be present when the factory will register the provider. 
 * For example, each entry could provide for the inclusion of one or more
 * RegistrationContext objects of the following form: 
 * <ul>
 * <li> The message layer name (or null)
 * <li> The application context identifier (or null)
 * <li> The registration description (or null)
 * </ul>
 * When a RegistrationContext is not included, the factory must make it 
 * convenient for the provider to self-register with the factory during 
 * the provider construction (see 
 * <code>registerConfigProvider(AuthConfigProvider provider, ...)</code>).
 * <P> An AuthConfigFactory implementation is free to choose is own 
 * persistent declarative syntax as long as it conforms to the requirements 
 * defined by this class.
 *
 * @version %I%, %G%
 *
 * @see ClientAuthContext
 * @see ServerAuthContext
 * @see ClientAuthConfig
 * @see ServerAuthConfig
 * @see Properties
 */
public abstract class AuthConfigFactory {

    private static AuthConfigFactory factory = null;

    /**
     * The name of the Security property used to define the
     * default AuthConfigFactory implementation class.
     */
    public static final String DEFAULT_FACTORY_SECURITY_PROPERTY = 
    "authconfigprovider.factory";

    // XXX verify that key need not include java.security.
 
    private static SecurityPermission getPerm = 
	new SecurityPermission
    ("getProperty."+DEFAULT_FACTORY_SECURITY_PROPERTY);

    private static SecurityPermission setPerm = 
	new SecurityPermission
    ("setProperty."+DEFAULT_FACTORY_SECURITY_PROPERTY);

    /**
     * utility method to check for permission to operate on the factory.
     *
     * @exception SecurityException if the caller does not have
     *		permission to operate on the factory.
     */
    private static void checkPermission(Permission p) 
	throws SecurityException {
	SecurityManager sm = System.getSecurityManager();
	if (sm != null) {
	    sm.checkPermission(p);
	}
    }

    /**
     * Get the system-wide AuthConfigFactory implementation.
     *
     * <p> If a non-null system-wide factory instance is defined at the time 
     * of the call,
     * for example, with <code>setFactory</code>, it will be returned. Otherwise, an
     * attempt will be made to construct an instance of the default
     * AuthConfigFactory implementation class. The fully qualified class name
     * of the default factory implementation class is obtained from the
     * value of the authconfigprovider.factory security property. When an
     * instance of the default factory implementation class is successfully
     * constructed by this method, this method will set it as the system-wide 
     * factory instance.
     * 
     * <p> The absolute pathname of the Java security properties file is
     * JAVA_HOME/lib/security/java.security, where JAVA_HOME
     * refers to the directory where the JDK was installed.
     *
     * @return The non-null system-wide AuthConfigFactory instance set at the 
     *          time of the call, or if that value was null, the value of the
     *          system-wide factory instance established by this method. 
     *          This method returns null when the system-wide factory was not 
     *          defined when this method was called and no default factory name
     *          was defined via the security property. 
     *
     * @exception AuthException If an error occurred during the class loading, 
     *          or construction of the default AuthConfigFactory
     *          implementation class.
     *
     * @exception SecurityException If the caller does not have permission
     *		to retrieve the factory, or set it as the system-wide 
     *          instance.
     *
     */
    public static synchronized AuthConfigFactory getFactory() {
	checkPermission(getPerm);
	if (AuthConfigFactory.factory == null) {
	    final String className = Security.getProperty
		(DEFAULT_FACTORY_SECURITY_PROPERTY);
	    if (className != null) {
		checkPermission(setPerm);
		try {
		    AuthConfigFactory.factory = (AuthConfigFactory)
			java.security.AccessController.doPrivileged
			(new java.security.PrivilegedExceptionAction() {
				public Object run() 
				    throws ClassNotFoundException,
					   InstantiationException,
				           IllegalAccessException {
				    ClassLoader loader =
					Thread.currentThread().
					getContextClassLoader();
				    
				    Class clazz = Class.forName
					(className,true,loader);

				    return clazz.newInstance();
				}
			    });
		} catch (java.security.PrivilegedActionException pae) {
		    throw (SecurityException)
			new SecurityException().initCause(pae.getException());
		} 
	    }
	} 
	return AuthConfigFactory.factory;
    }

    /**
     * Set the system-wide AuthConfigFactory implementation.
     *
     * <p> If an implementation was set previously, it will be replaced.
     * <p> Listeners are not notified of a change to the registered factory.
     *
     * @param factory The AuthConfigFactory instance, which may be null. 
     *
     * @exception SecurityException If the caller does not have
     *		permission to set the factory.
     */
    public static synchronized void setFactory(AuthConfigFactory factory) {
	checkPermission(setPerm);
	AuthConfigFactory.factory = factory;
    }

    /**
     * Get a registered AuthConfigProvider from the factory.
     *
     * Get the provider of ServerAuthConfig and
     * ClientAuthConfig objects registered for the identified message 
     * layer and application context.
     *
     * <p>All factories shall employ the following precedence rules to select
     * the registered AuthConfigProvider that matches the layer and appContext 
     * arguments:
     *<ul>
     * <li> The provider specifically registered for the values 
     * passed as the layer and appContext arguments 
     * shall be selected.
     * <li> If no provider is selected according to the preceding rule,
     * the provider specifically registered for the value passed as the 
     * appContext argument and for all (that is, null) layers
     * shall be selected.
     * <li> If no provider is selected according to the preceding rules,
     * the provider specifically registered for the value passed as the
     * layer argument and for all (that is, null) appContexts
     * shall be selected.
     * <li> If no provider is selected according to the preceding rules,
     * the provider registered for all (that is, null) layers 
     * and for all (that is, null) appContexts shall be selected.
     * <li> If no provider is selected according to the preceding rules,
     * the factory shall terminate its search for a registered provider.
     *</ul>
     * <p>
     * The above precedence rules apply equivalently to registrations created
     * with a null or non-null <code>className</code> argument.
     *
     * @param layer A String identifying the message layer
     *		for which the registered AuthConfigProvider is
     *          to be returned. The value of this argument may be null.
     *
     * @param appContext A String that identifies the application messaging 
     *          context for which the registered AuthConfigProvider
     *          is to be returned. The value of this argument may be null.
     *
     * @param listener The RegistrationListener whose 
     *          <code>notify</code> method is to be invoked 
     *          if the corresponding registration is unregistered or 
     *          replaced. The value of this argument may be null.
     *
     * @return The implementation of the AuthConfigProvider interface 
     *          registered at the factory for the layer and appContext,
     *          or null if no AuthConfigProvider is selected. An argument
     *          listener is attached even if the return value is null.
     */
    public abstract AuthConfigProvider 
    getConfigProvider(String layer, String appContext,
		      RegistrationListener listener);

    /**
     * Registers within the factory and records within the factory's  
     * persistent declarative representation of provider registrations
     * a provider of ServerAuthConfig and/or ClientAuthConfig objects for a
     * message layer and application context identifier.
     * 
     * This method typically constructs an instance of the provider
     * before registering it with the factory. Factories may extend or modify 
     * the persisted registrations of existing provider instances, if 
     * those instances were registered with ClassName and 
     * properties arguments equivalent to those passed in the current call. 
     * <P>
     * This method employs the two argument constructor required to
     * be supported by every implementation of the AuthConfigProvider 
     * interface, and this method must pass a null value for the factory 
     * argument of the constructor.
     * <code>AuthConfigProviderImpl AuthConfigProviderImpl(Map properties, 
     * AuthConfigFactory factory)</code>. 
     *
     * <P>At most one registration may exist within the factory for a 
     * given combination of message layer 
     * and appContext. Any pre-existing
     * registration with identical values for layer and appContext is replaced 
     * by a subsequent registration. When replacement occurs, the registration
     * identifier, layer, and appContext identifier remain unchanged,
     * and the AuthConfigProvider (with initialization properties) and 
     * description are replaced. 
     *
     *<p>Within the lifetime of its Java process, a factory must assign unique
     * registration identifiers to registrations, and must never 
     * assign a previously used registration identifier to a registration 
     * whose message layer and or appContext identifier differ from 
     * the previous use.
     * 
     * <p>Programmatic registrations performed by using this method must update
     * (according to the replacement rules described above) the persistent 
     * declarative representation of provider registrations employed by the 
     * factory constructor.
     *
     * @param className The fully qualified name of an AuthConfigProvider
     *          implementation class (or null). Calling this method with a 
     *          null value for this parameter shall cause 
     *          <code>getConfigProvider</code> to return null when it is
     *          called with layer and appContext values for which the 
     *          resulting registration is the best match.
     *
     * @param properties A Map object containing the initialization 
     *          properties to be passed to the properties argument of the
     *          provider constructor.
     *          This argument may be null. When this argument is not null, 
     *          all the values and keys occuring in the Map must be of 
     *          type String.
     *
     * @param layer A String identifying the message layer
     *		for which the provider will be registered at the factory.
     *          A null value may be passed as an argument for this parameter,
     *          in which case the provider is registered at all layers.
     *
     * @param appContext A String value that may be used by a runtime
     *          to request a configuration object from this provider.
     *          A null value may be passed as an argument for this parameter,
     *          in which case the provider is registered for all 
     *          configuration ids (at the indicated layers).
     *
     * @param description A text String describing the provider.
     *          This value may be null.
     *
     * @return A String identifier assigned by 
     *          the factory to the provider registration, and that may be 
     *          used to remove the registration from the factory.
     *
     * @exception SecurityException If the caller does not have
     *		permission to register a provider at the factory.
     *
     * @exception AuthException If the provider 
     *          construction (given a non-null <code>className</code>) 
     *          or registration fails.
     */
    public abstract String 
    registerConfigProvider(String className,
			   Map properties,
			   String layer,
			   String appContext,
			   String description);

    /**
     * Registers within the (in-memory) factory, a provider 
     * of ServerAuthConfig and/or ClientAuthConfig objects for a 
     * message layer and application context identifier.
     * This method does NOT effect the factory's persistent 
     * declarative representation of provider registrations, and is intended
     * to be used by providers to perform self-Registration.
     *
     * <P>At most one registration may exist within the factory for a 
     * given combination of message layer 
     * and appContext. Any pre-existing
     * registration with identical values for layer and appContext is replaced 
     * by a subsequent registration. When replacement occurs, the registration
     * identifier, layer, and appContext identifier remain unchanged,
     * and the AuthConfigProvider (with initialization properties) and 
     * description are replaced. 
     *
     *<p>Within the lifetime of its Java process, a factory must assign unique
     * registration identifiers to registrations, and must never 
     * assign a previously used registration identifier to a registration 
     * whose message layer and or appContext identifier differ from 
     * the previous use.
     * 
     * @param provider The AuthConfigProvider to be registered at the factory
     *          (or null). Calling this method with a null value for this 
     *          parameter shall cause <code>getConfigProvider</code> to 
     *          return null when it is called with layer and appContext values 
     *          for which the resulting registration is the best match.
     *
     * @param layer A String identifying the message layer
     *		for which the provider will be registered at the factory.
     *          A null value may be passed as an argument for this parameter,
     *          in which case the provider is registered at all layers.
     *
     * @param appContext A String value that may be used by a runtime
     *          to request a configuration object from this provider.
     *          A null value may be passed as an argument for this parameter,
     *          in which case the provider is registered for all 
     *          configuration ids (at the indicated layers).
     *
     * @param description A text String describing the provider.
     *          This value may be null.
     *
     * @return A String identifier assigned by 
     *          the factory to the provider registration, and that may be 
     *          used to remove the registration from the factory.
     *
     * @exception SecurityException If the caller does not have
     *		permission to register a provider at the factory.
     *
     * @exception AuthException If the provider registration fails.
     */
    public abstract String
    registerConfigProvider(AuthConfigProvider provider,
			   String layer,
			   String appContext,
			   String description);

    /**
     * Remove the identified provider registration from the factory (and from
     * the persistent declarative representation of provider registrations, 
     * if appropriate) and invoke any listeners associated with the removed 
     * registration.
     *
     * @param registrationID A String that identifies a provider registration
     *          at the factory
     *
     * @return True if there was a registration with the specified identifier 
     *          and it was removed. Return false if the registrationID was
     *          invalid.
     *
     * @exception SecurityException If the caller does not have
     *		permission to unregister the provider at the factory.
     *
     */
    public abstract boolean
    removeRegistration(String registrationID);

    /**
     * Disassociate the listener from all the provider
     * registrations whose layer and appContext values are matched
     * by the corresponding arguments to this method.
     * <p>
     * Factories should periodically notify Listeners to effectively
     * detach listeners that are no longer in use.
     *
     * @param listener The RegistrationListener to be detached.
     *
     * @param layer A String identifying the message layer or null.
     *
     * @param appContext A String value identifying the application contex
     *          or null.
     *
     * @return An array of String values where each value identifies a 
     *          provider registration from which the listener was removed.
     *          This method never returns null; it returns an empty array if 
     *          the listener was not removed from any registrations.
     *
     * @exception SecurityException If the caller does not have
     *		permission to detach the listener from the factory.
     *
     */
    public abstract String[]
    detachListener(RegistrationListener listener,
		   String layer, String appContext);

    /**
     * Get the registration identifiers for all registrations of the 
     * provider instance at the factory.
     *
     * @param provider The AuthConfigurationProvider whose registration
     *          identifiers are to be returned. This argument may be
     *          null, in which case it indicates that the IDs of
     *          all active registrations within the factory are to be returned.
     *
     * @return An array of String values where each value identifies a 
     * provider registration at the factory. This method never returns null;
     * it returns an empty array when there are no registrations at the 
     * factory for the identified provider.
     */
    public abstract String[] getRegistrationIDs(AuthConfigProvider provider);

    /**
     * Get the the registration context for the identified registration.
     *
     * @param registrationID A String that identifies a provider registration
     *          at the factory
     *
     * @return A RegistrationContext or null. When a Non-null value is
     * returned, it is a copy of the registration context corresponding to the
     * registration. Null is returned when the registration identifier does
     * not correpond to an active registration
      */
    public abstract RegistrationContext 
    getRegistrationContext(String registrationID);

   /**
     * Cause the factory to reprocess its persisent declarative 
     * representation of provider registrations. 
     *
     * <p> A factory should only replace an existing registration when 
     * a change of provider implementation class or initialization 
     * properties has occurred. 
     *
     * @exception AuthException If an error occured during the 
     *          reinitialization.
     *
     * @exception SecurityException If the caller does not have permission
     *		to refresh the factory.
     */
    public abstract void refresh();

    /**
     * Represents the layer identifier, application context identifier,
     * and description components of an AuthConfigProvider registration at the
     * factory.
     */

    public interface RegistrationContext {

       /**
	* Get the layer name from the registration context
	*
	* @return A String identifying the message layer
	*	  for which the AuthConfigProvider was registered.
	*         The returned value may be null.
	*/

        public String getMessageLayer();
	
       /**
	* Get the application context identifier from the registration context
	*
	* @return A String identifying the application context
	*	  for which the AuthConfigProvider was registered.
	*         The returned value may be null.
	*/
        public String getAppContext();

       /**
	* Get the description from the registration context
	*
	* @return The description String from the registration, or null
	*         if no description string was included in the registration.
	*/
	public String getDescription();


       /**
	* Get the persisted status from the registration context.
	*
	* @return A boolean indicating whether the registration is the result 
	*         of a className based registration, or an instance-based
	*         (for example, self-) registration. Only registrations performed
	*         using the five argument <code>registerConfigProvider</code>
	*         method are persistent.
	*/
	public boolean isPersistent();

    }

}
