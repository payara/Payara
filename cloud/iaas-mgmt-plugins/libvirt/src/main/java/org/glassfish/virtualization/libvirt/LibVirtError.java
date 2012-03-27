/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.virtualization.libvirt;

import org.glassfish.virtualization.libvirt.jna.LibVirtLibrary;
import org.glassfish.virtualization.libvirt.jna.VirError;
import org.glassfish.virtualization.spi.VirtException;

/**
 * LibVirt error Java Abstraction.
 * @author Jerome Dochez
 */
public class LibVirtError {

    public static enum ErrorDomain {
        VIR_FROM_NONE, VIR_FROM_XEN, /* Error at Xen hypervisor layer */
        VIR_FROM_XEND, /* Error at connection with xend daemon */
        VIR_FROM_XENSTORE, /* Error at connection with xen store */
        VIR_FROM_SEXPR, /* Error in the S-Expression code */
        VIR_FROM_XML, /* Error in the XML code */
        VIR_FROM_DOM, /* Error when operating on a domain */
        VIR_FROM_RPC, /* Error in the XML-RPC code */
        VIR_FROM_PROXY, /* Error in the proxy code */
        VIR_FROM_CONF, /* Error in the configuration file handling */
        VIR_FROM_QEMU, /* Error at the QEMU daemon */
        VIR_FROM_NET, /* Error when operating on a network */
        VIR_FROM_TEST, /* Error from test driver */
        VIR_FROM_REMOTE, /* Error from remote driver */
        VIR_FROM_OPENVZ, /* Error from OpenVZ driver */
        VIR_FROM_XENXM, /* Error at Xen XM layer */
        VIR_FROM_STATS_LINUX, /* Error in the Linux Stats code */
        VIR_FROM_LXC, /* Error from Linux Container driver */
        VIR_FROM_STORAGE, /* Error from storage driver */
        VIR_FROM_NETWORK, /* Error from network config */
        VIR_FROM_DOMAIN, /* Error from domain config */
        VIR_FROM_UML, /* Error at the UML driver */
        VIR_FROM_NODEDEV, /* Error from node device monitor */
        VIR_FROM_XEN_INOTIFY, /* Error from xen inotify layer */
        VIR_FROM_SECURITY, /* Error from security framework */
        VIR_FROM_VBOX, /* Error from VirtualBox driver */
        VIR_FROM_INTERFACE, /* Error when operating on an interface */
        VIR_FROM_ONE, /* Error from OpenNebula driver */
        VIR_FROM_ESX, /* Error from ESX driver */
        VIR_FROM_PHYP, /* Error from IBM power hypervisor */
        VIR_FROM_SECRET
        /* Error from secret storage */
    }

    public static enum ErrorLevel {
        VIR_ERR_NONE,
        /**
         * A simple warning
         */
        VIR_ERR_WARNING,
        /**
         * An error
         */
        VIR_ERR_ERROR
    }

    public static enum ErrorNumber {
        VIR_ERR_OK, VIR_ERR_INTERNAL_ERROR, /* internal error */
        VIR_ERR_NO_MEMORY, /* memory allocation failure */
        VIR_ERR_NO_SUPPORT, /* no support for this function */
        VIR_ERR_UNKNOWN_HOST, /* could not resolve hostname */
        VIR_ERR_NO_CONNECT, /* can't connect to hypervisor */
        VIR_ERR_INVALID_CONN, /* invalid connection object */
        VIR_ERR_INVALID_DOMAIN, /* invalid domain object */
        VIR_ERR_INVALID_ARG, /* invalid function argument */
        VIR_ERR_OPERATION_FAILED, /* a command to hypervisor failed */
        VIR_ERR_GET_FAILED, /* a HTTP GET command to failed */
        VIR_ERR_POST_FAILED, /* a HTTP POST command to failed */
        VIR_ERR_HTTP_ERROR, /* unexpected HTTP error code */
        VIR_ERR_SEXPR_SERIAL, /* failure to serialize an S-Expr */
        VIR_ERR_NO_XEN, /* could not open Xen hypervisor control */
        VIR_ERR_XEN_CALL, /* failure doing an hypervisor call */
        VIR_ERR_OS_TYPE, /* unknown OS type */
        VIR_ERR_NO_KERNEL, /* missing kernel information */
        VIR_ERR_NO_ROOT, /* missing root device information */
        VIR_ERR_NO_SOURCE, /* missing source device information */
        VIR_ERR_NO_TARGET, /* missing target device information */
        VIR_ERR_NO_NAME, /* missing domain name information */
        VIR_ERR_NO_OS, /* missing domain OS information */
        VIR_ERR_NO_DEVICE, /* missing domain devices information */
        VIR_ERR_NO_XENSTORE, /* could not open Xen Store control */
        VIR_ERR_DRIVER_FULL, /* too many drivers registered */
        VIR_ERR_CALL_FAILED, /* not supported by the drivers (DEPRECATED) */
        VIR_ERR_XML_ERROR, /* an XML description is not well formed or broken */
        VIR_ERR_DOM_EXIST, /* the domain already exist */
        VIR_ERR_OPERATION_DENIED, /*
                                   * operation forbidden on read-only
                                   * connections
                                   */
        VIR_ERR_OPEN_FAILED, /* failed to open a conf file */
        VIR_ERR_READ_FAILED, /* failed to read a conf file */
        VIR_ERR_PARSE_FAILED, /* failed to parse a conf file */
        VIR_ERR_CONF_SYNTAX, /* failed to parse the syntax of a conf file */
        VIR_ERR_WRITE_FAILED, /* failed to write a conf file */
        VIR_ERR_XML_DETAIL, /* detail of an XML error */
        VIR_ERR_INVALID_NETWORK, /* invalid network object */
        VIR_ERR_NETWORK_EXIST, /* the network already exist */
        VIR_ERR_SYSTEM_ERROR, /* general system call failure */
        VIR_ERR_RPC, /* some sort of RPC error */
        VIR_ERR_GNUTLS_ERROR, /* error from a GNUTLS call */
        VIR_WAR_NO_NETWORK, /* failed to start network */
        VIR_ERR_NO_DOMAIN, /* domain not found or unexpectedly disappeared */
        VIR_ERR_NO_NETWORK, /* network not found */
        VIR_ERR_INVALID_MAC, /* invalid MAC address */
        VIR_ERR_AUTH_FAILED, /* authentication failed */
        VIR_ERR_INVALID_STORAGE_POOL, /* invalid storage pool object */
        VIR_ERR_INVALID_STORAGE_VOL, /* invalid storage vol object */
        VIR_WAR_NO_STORAGE, /* failed to start storage */
        VIR_ERR_NO_STORAGE_POOL, /* storage pool not found */
        VIR_ERR_NO_STORAGE_VOL, /* storage pool not found */
        VIR_WAR_NO_NODE, /* failed to start node driver */
        VIR_ERR_INVALID_NODE_DEVICE, /* invalid node device object */
        VIR_ERR_NO_NODE_DEVICE, /* node device not found */
        VIR_ERR_NO_SECURITY_MODEL, /* security model not found */
        VIR_ERR_OPERATION_INVALID, /* operation is not applicable at this time */
        VIR_WAR_NO_INTERFACE, /* failed to start interface driver */
        VIR_ERR_NO_INTERFACE, /* interface driver not running */
        VIR_ERR_INVALID_INTERFACE, /* invalid interface object */
        VIR_ERR_MULTIPLE_INTERFACES, /* more than one matching interface found */
        VIR_WAR_NO_SECRET, /* failed to start secret storage */
        VIR_ERR_INVALID_SECRET, /* invalid secret */
        VIR_ERR_NO_SECRET
        /* secret not found */

    }

    ErrorNumber code;
    ErrorDomain domain;
    String message;
    ErrorLevel level;
    String str1;
    String str2;
    String str3;
    int int1;
    int int2;

    public LibVirtError(VirError vError) {
        code = ErrorNumber.values()[vError.code];
        domain = ErrorDomain.values()[vError.domain];
        level = ErrorLevel.values()[vError.level];
        message = vError.message;
        str1 = vError.str1;
        str2 = vError.str2;
        str3 = vError.str3;
        int1 = vError.int1;
        int2 = vError.int2;
    }

    public ErrorLevel getLevel() {
        return level;
    }

    /**
     * Look for the latest error from libvirt not tied to a connection
     *
     * @param libvirt
     *            the active connection
     * @throws VirtException
     */
    public static void processError(LibVirtLibrary libvirt) throws VirtException {
        VirError vError = new VirError();
        int errorCode = libvirt.virCopyLastError(vError);
        if (errorCode > 0) {
            LibVirtError error = new LibVirtError(vError);
            libvirt.virResetLastError();
            /*
             * FIXME: Don't throw exceptions for VIR_ERR_WARNING
             * level errors
             */
            if (error.getLevel() == ErrorLevel.VIR_ERR_ERROR) {
                throw new LibVirtException(error);
            }
        }
    }
}
