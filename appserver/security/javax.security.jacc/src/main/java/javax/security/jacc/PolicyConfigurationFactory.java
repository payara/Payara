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

package javax.security.jacc;


import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 * Abstract factory and finder class for obtaining
 * the instance of the class that implements the PolicyConfigurationFactory
 * of a provider. The factory will be used to instantiate PolicyConfiguration
 * objects that will be used by the deployment tools of the container
 * to create and manage policy contexts within the Policy Provider.
 * <P>
 * Implementation classes must have a public no argument constructor that
 * may be used to create an operational instance of the factory implementation
 * class.
 *
 * @see java.security.Permission
 * @see javax.security.jacc.PolicyConfiguration
 * @see javax.security.jacc.PolicyContextException
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 * @author Harpreet Singh
 */

public abstract class PolicyConfigurationFactory
{
    private static String FACTORY_NAME = 
	"javax.security.jacc.PolicyConfigurationFactory.provider";
    
    private static PolicyConfigurationFactory pcFactory;

   /**
    * This static method uses a system property to find and instantiate
    * (via a public constructor) a provider specific factory implementation
    * class. The name of the provider specific factory implementation class is
    * obtained from the value of the system property,
    * <P><code><Pre>
    *     javax.security.jacc.PolicyConfigurationFactory.provider.
    * </Pre></code><P>
    *
    * @return the singleton instance of the provider specific
    * PolicyConfigurationFactory implementation class.
    *
    * @throws java.lang.SecurityException
    * when called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.ClassNotFoundException
    * when the class named by the system property could not be found
    * including because the value of the system property has not be set.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the getPolicyConfigurationFactory method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException
    */

    public static PolicyConfigurationFactory getPolicyConfigurationFactory()
        throws java.lang.ClassNotFoundException,
	    javax.security.jacc.PolicyContextException
    {

      SecurityManager sm = System.getSecurityManager();
      if (sm != null) sm.checkPermission(new 
			     java.security.SecurityPermission("setPolicy"));
      if(pcFactory != null)
	  return pcFactory;
	
      String msg;
      final String classname[] = { null };

      try {

          Class clazz = null;
          
          if (sm  != null){
              try{
                clazz = (Class)AccessController.doPrivileged
		    (new PrivilegedExceptionAction(){
                      public Object run() throws java.lang.Exception{
			  
			  classname[0] = System.getProperty(FACTORY_NAME);
			  
			  if(classname[0] == null){
			      String msg = "JACC:Error PolicyConfigurationFactory : property not set : "+ FACTORY_NAME;
			      throw new ClassNotFoundException(msg);
			  }
	
              return Class.forName(classname[0],true,
			      Thread.currentThread().getContextClassLoader());
                      }
                  });
              } catch (PrivilegedActionException ex){
                  Exception e = ex.getException() ;
                  if ( e instanceof java.lang.ClassNotFoundException){
                        throw (java.lang.ClassNotFoundException)e;
                  } else if ( e instanceof java.lang.InstantiationException){
                        throw (java.lang.InstantiationException)e;
                  } else if ( e instanceof java.lang.IllegalAccessException){
                        throw (java.lang.IllegalAccessException)e;
                  }
              }
          } else {
	      classname[0] = System.getProperty(FACTORY_NAME);
	      
	      if (classname[0] == null){
		  msg = "JACC:Error PolicyConfigurationFactory : property not set : "+ FACTORY_NAME;
		  throw new ClassNotFoundException(msg);
	      }

	      clazz = Class.forName(classname[0],true,
				    Thread.currentThread().getContextClassLoader());
          }

          Object factory  = clazz.newInstance();

          pcFactory  = (PolicyConfigurationFactory) factory;

      } catch(java.lang.ClassNotFoundException cnfe){
	  msg = "JACC:Error PolicyConfigurationFactory : cannot find class : "
	       + classname[0];
	  throw new ClassNotFoundException(msg,cnfe);
      } catch(java.lang.IllegalAccessException iae){
	  msg = "JACC:Error PolicyConfigurationFactory : cannot access class : "
	       + classname[0];
	  throw new PolicyContextException(msg,iae);
      } catch(java.lang.InstantiationException ie){
	  msg = "JACC:Error PolicyConfigurationFactory : cannot instantiate : "
	       + classname[0];
	  throw new PolicyContextException(msg,ie);
      } catch(java.lang.ClassCastException cce){
	  msg = "JACC:Error PolicyConfigurationFactory : class not PolicyConfigurationFactory : "+ classname[0];
	  throw new ClassCastException(msg);
      }

      return pcFactory;
      
    }
    
   /**
    * This method is used to obtain an instance of the provider specific
    * class that implements the PolicyConfiguration interface that
    * corresponds to the identified policy context within the provider.
    * The methods of the PolicyConfiguration interface are used to
    * define the policy statements of the identified policy context.
    * <P>
    * If at the time of the call, the identified policy context does not
    * exist in the provider, then the policy context will be created
    * in the provider and the Object that implements the context's
    * PolicyConfiguration Interface will be returned. If the state of the
    * identified context is "deleted" or "inService" it will be transitioned to
    * the "open" state as a result of the call. The states in the lifecycle
    * of a policy context are defined by the PolicyConfiguration interface.
    * <P>
    * For a given value of policy context identifier, this method
    * must always return the same instance of PolicyConfiguration
    * and there must be at most one actual instance of a
    * PolicyConfiguration with a given policy context identifier
    * (during a process context).
    * <P>
    * To preserve the invariant that there be at most one
    * PolicyConfiguration object for a given policy context,
    * it may be necessary for this method to be thread safe.
    * <P>
    * @param contextID A String identifying the policy context whose
    * PolicyConfiguration interface is to be returned. The value passed to
    * this parameter must not be null.
    * <P>
    * @param remove A boolean value that establishes whether or not the
    * policy statements and linkages of an existing policy context are to be 
    * removed before its PolicyConfiguration object is returned. If the value
    * passed to this parameter is true, the policy statements and linkages of  
    * an existing policy context will be removed. If the value is false, 
    * they will not be removed.
    *
    * @return an Object that implements the PolicyConfiguration
    * Interface matched to the Policy provider and corresponding to the
    * identified policy context.
    *
    * @throws java.lang.SecurityException
    * when called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the getPolicyConfiguration method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */

    public abstract PolicyConfiguration 
    getPolicyConfiguration(String contextID, boolean remove)
    	throws javax.security.jacc.PolicyContextException;

   /**
    * This method determines if the identified policy context
    * exists with state "inService" in the Policy provider
    * associated with the factory.
    * <P>
    * @param contextID A string identifying a policy context
    *
    * @return true if the identified policy context exists within the
    * provider and its state is "inService", false otherwise.
    *
    * @throws java.lang.SecurityException
    * when called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the inService method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */

    public abstract boolean inService(String contextID)
		throws javax.security.jacc.PolicyContextException;

}
