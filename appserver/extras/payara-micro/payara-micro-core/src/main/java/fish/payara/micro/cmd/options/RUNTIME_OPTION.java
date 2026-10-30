/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.micro.cmd.options;

/**
 * ENUM used for command line switches for Payara Micro
 * @author Steve Millidge (Payara Services Limited)
 */
public enum RUNTIME_OPTION {
    nocluster(false),
    deploydir(true, new DirectoryValidator(true, true, false)),
    deploy(true, new DeploymentFileValidator()),
    port(true, new PortValidator()),
    sslport(true, new PortValidator()),
    name(true),
    instancegroup(true),
    group(true),
    mcaddress(true, new MulticastValidator()),
    mcport(true, new PortValidator()),
    clustername(true),
    hostaware(false),
    nohostaware(false),
    startport(true, new PortValidator()),
    addlibs(true, new SeparatedFilesValidator(true, true, false, true, true)),
    addjars(true, new SeparatedFilesValidator(true, true, false, true, true)),
    rootdir(true, new DirectoryValidator(true, true, true)),
    deploymentdir(true, new DirectoryValidator(true, true, false)),
    secretsdir(true,new DirectoryValidator(true, true, false)),
    showservletmappings(false),
    domainconfig(true, new FileValidator(true, true, true)),
    minhttpthreads(true, new IntegerValidator(1, Integer.MAX_VALUE)),
    maxhttpthreads(true, new IntegerValidator(2, Integer.MAX_VALUE)),
    hzconfigfile(true, new FileValidator(true, true, false)),
    autobindhttp(false),
    autobindssl(false),
    autobindrange(true, new IntegerValidator(1, 100000)),
    lite(false),
    enablehealthcheck(true),
    logo(false),
    deployfromgav(true, new DeploymentGAVValidator()),
    additionalrepository(true),
    outputuberjar(true, new FileValidator(false, false, false)),
    outputlauncher(false),
    copytouberjar(true, new DirectoryValidator(true,true, false)),
    systemproperties(true, new FileValidator(true, true, false)),
    disablephonehome(false),
    version(false),
    logtofile(true, new FileValidator(false, false, false)),
    logproperties(true, new FileValidator(true, true, false)),
    enabledynamiclogging(false),
    accesslog(true, new DirectoryValidator(true, true, true)),
    accesslogformat(true),
    accessloginterval(true),
    accesslogsuffix(true),
    accesslogprefix(true),
    enablerequesttracing(true, new RequestTracingValidator(), true),
    requesttracingthresholdunit(true),
    requesttracingthresholdvalue(true),
    enablerequesttracingadaptivesampling(false),
    requesttracingadaptivesamplingtargetcount(true),
    requesttracingadaptivesamplingtimevalue(true),
    requesttracingadaptivesamplingtimeunit(true),
    prebootcommandfile(true, new FileValidator(true, true, false)),
    postbootcommandfile(true, new FileValidator(true, true, false)),
    postdeploycommandfile(true, new FileValidator(true,true,false)),
    nested(false),
    unpackdir(true, new DirectoryValidator(true, true, true)),
    clustermode(true, new PrefixStringListValidator("tcpip","domain","multicast","dns")),
    interfaces(true),
    sslcert(true),
    help(false),
    enablesni(false),
    hzpublicaddress(true),
    shutdowngrace(true, new IntegerValidator(1, Integer.MAX_VALUE)),
    hzinitialjoinwait(true, new IntegerValidator(0,100000)),
    contextroot(true),
    globalcontextroot(true),
    warmup(false),
    hotdeploy(false),
    nohazelcast(false);

    RUNTIME_OPTION(boolean hasValue) {
        this(hasValue, new Validator());
    }

    RUNTIME_OPTION(boolean hasValue, boolean optionalValue) {
        this(hasValue, new Validator(), optionalValue);
    }

    RUNTIME_OPTION(boolean hasValue, Validator validator) {
        this(hasValue, validator, false);
    }

    RUNTIME_OPTION(boolean hasValue, Validator validator, boolean optionalValue) {
        this.value = hasValue;
        this.validator = validator;
        this.optional = optionalValue;
    }

    public boolean validate(String optionValue) throws ValidationException {
        return validator.validate(optionValue);
    }

    boolean hasFollowingValue() {
        return value;
    }

    boolean followingValueIsOptional() {
        return optional;
    }

    private final Validator validator;
    
    // Indicates the runtime option requires a value
    private final boolean value;

    // Indicates the runtime option is optional
    private final boolean optional;

}
