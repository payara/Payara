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

package javax.security.auth.message;
  
// for @see
import javax.security.auth.message.config.ClientAuthContext;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.*;


/**
 * This class defines a message authentication policy.
 *
 * <p> A ClientAuthContext uses this class to communicate
 * (at module initialization time) request and response message protection 
 * policies to its ClientAuthModule objects.
 * A ServerAuthContext uses this class to communicate
 * request and response message protection 
 * policies to its ServerAuthModule objects.  
 * 
 * @version %I%, %G%
 * @see ClientAuthContext
 * @see ServerAuthContext
 * @see ClientAuthModule
 * @see ServerAuthModule
 */

public class MessagePolicy {

    private TargetPolicy[] targetPolicies;
    private boolean mandatory;

    /**
     * Create a MessagePolicy instance with an array of target policies.
     *
     * @param targetPolicies an array of target policies.
     *
     * @param mandatory A boolean value indicating whether the MessagePolicy is
     *          mandatory or optional. 
     *
     * @exception IllegalArgumentException if the specified targetPolicies
     *		is null.
     */
    public MessagePolicy(TargetPolicy[] targetPolicies, boolean mandatory) {
	if (targetPolicies == null) {
	    throw new IllegalArgumentException("invalid null targetPolicies");
	}
	this.targetPolicies = targetPolicies.clone();
        this.mandatory = mandatory;
    }
  
    /** 
     * Get the MessagePolicy modifier.
     *
     * @return A boolean indicating whether the MessagePolicy is 
     *         optional(false) or required(true). 
     */
    public boolean isMandatory() {
        return mandatory;        
    }
        
    /**
     * Get the target policies that comprise the authentication policy.
     *
     * <p> 
     *
     * @return An array of target authentication policies, where each element
     * describes an authentication policy and the parts of the message to which
     * the authentication policy applies. This method returns null to indicate
     * that no security operations should be performed on the messages to which
     * the policy applies. This method never returns a zero-length array.
     *
     * <p> When this method returns an array of target policies, the 
     * order of elements in the array represents the order that the
     * corresponding message transformations or validations described 
     * by the target policies are to be performed by the authentication
     * module.
     */
    public TargetPolicy[] getTargetPolicies() {
	return targetPolicies.clone();
    }

    /**
     * This class defines the message protection policies for specific Targets.
     *
     * <p> This class is used to associate a message protection policy with
     * targets within a message. Message targets are represented using
     * an implementation of the <i>Target</i> interface matched to the
     * message types in MessageInfo. The message protection policy is 
     * identified by an implementation of the <i>ProtectionPolicy</i> 
     * interface.
     *
     * @see ClientAuthModule
     * @see ServerAuthModule
     */
    public static class TargetPolicy {

	private Target[] targets;
	private ProtectionPolicy protectionPolicy;
  
	/**
	 * Create a TargetPolicy instance with an array of Targets
	 * and with a ProtectionPolicy.
	 *
	 * @param targets An array of Targets. This argument may be null.
	 *
	 * @param protectionPolicy The object that describes
	 * the message authentication policy that applies to the
	 * targets.
	 *
	 * @exception IllegalArgumentException if the specified targets
	 *		or protectionPolicy is null.
	 */
	public TargetPolicy(Target[] targets, 
			    ProtectionPolicy protectionPolicy) {
	    if (protectionPolicy == null) {
		throw new IllegalArgumentException
			("invalid null protectionPolicy");
	    }
	    if (targets == null || targets.length == 0) {
		this.targets = null;
	    } else {
		this.targets = targets.clone();
	    }
	    this.protectionPolicy = protectionPolicy;
	}
  
	/** 
	 * Get the array of layer-specific target descriptors that identify
	 * the one or more message parts to which the specified message
	 * protection policy applies.
	 *
	 * @return An array of <i>Target</i> that identify targets 
	 * within a message. This method returns null when the specified policy
	 * applies to the whole message (excluding any metadata added to the 
	 * message to satisfy the policy). This method never returns a
	 * zero-length array.
	 */
	public Target[] getTargets() {
	    return targets;
	}

	/**
	 * Get the ProtectionPolicy that applies to the targets. 
	 *
	 * @return A ProtectionPolicy object that identifies the
	 * message authentication requirements that apply to the
	 * targets.
	 */
	public ProtectionPolicy getProtectionPolicy() {
	    return protectionPolicy;
	}
    }

    /**
     * This interface is used to represent and perform
     * message targeting. Targets are used by message authentication
     * modules to operate on the corresponding content within messages.
     *
     * <p> The internal state of a Target indicates whether it applies
     * to the request or response message of a MessageInfo and to which
     * components it applies within the identified message. 
     */
    public static interface Target {
      
	/** 
	 * Get the Object identified by the Target from the MessageInfo.
	 *
	 * @param messageInfo The MessageInfo containing the request or 
	 *      response message from which the target is to be obtained. 
	 *
	 * @return An Object representing the target,
	 *	or null when the target could not be found in the MessageInfo.
	 */
	public Object get(MessageInfo messageInfo);

	/** 
	 * Remove the Object identified by the Target from the MessageInfo.
	 *
	 * @param messageInfo The MessageInfo containing the request or
	 *	response message from which the target is to be removed.
	 */
	public void remove(MessageInfo messageInfo);

	/** 
	 * Put the Object into the MessageInfo at the location identified
	 * by the target.
	 *
	 * @param messageInfo The MessageInfo containing the request or
	 *	response message into which the object is to be put.
	 */
	public void put(MessageInfo messageInfo, Object data);
    }

    /**
     * This interface is used to represent message authentication policy.
     *
     * <p> The internal state of a ProtectionPolicy object defines the
     * message authentication requirements to be applied to the associated
     * Target.
     */
    public static interface ProtectionPolicy {
      
	/**
         * The identifer for a ProtectionPolicy that indicates that the
	 * sending entity is to be authenticated.
	 */
	public static final String AUTHENTICATE_SENDER = 

	    "#authenticateSender";

	/**
	 * The identifer for a ProtectionPolicy that indicates that the
         * origin of data within the message is to be authenticated 
	 * (that is, the message is to be protected such that 
         * its recipients can establish who defined the message
	 * content).
	 */
	public static final String AUTHENTICATE_CONTENT = 

	    "#authenticateContent";

	/**
	 * The identifer for a ProtectionPolicy that indicates that the
	 * message recipient is to be authenticated.
	 */
	public static final String AUTHENTICATE_RECIPIENT =
 
	    "#authenticateRecipient";

	/** 
	 * Get the ProtectionPolicy identifier. An identifier may represent
         * a conceptual protection policy (as is the case with the static 
         * identifiers defined within this interface) or it may identify
         * a procedural policy expression or plan that may be more difficult
         * to categorize in terms of a conceptual identifier. 
	 *
	 * @return A String containing a policy identifier. This interface
	 *         defines some policy identifier constants.
	 *         Configuration systems may define and employ other
	 *         policy identifiers values.
	 */
	public String getID();
    }

}
