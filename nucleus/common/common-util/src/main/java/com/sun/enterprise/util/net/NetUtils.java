/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.net;

import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.StringUtils;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetUtils {
    public static final int MAX_PORT = 65535;
    private final static int IS_RUNNING_DEFAULT_TIMEOUT = 3000;
    private final static boolean asDebug =
            Boolean.parseBoolean(System.getenv("AS_DEBUG"));
    private final static Logger logger = CULoggerInfo.getLogger();
    private static Boolean badHostErrorWasReportedAlready = Boolean.FALSE;
    private final static Object SYNC_OBJECT = new Object();
    private static void printd(String string) {
        if (asDebug) {
            System.out.println(string);
        }
    }

    public enum PortAvailability {
        illegalNumber, noPermission, inUse, unknown, OK
    };

    private NetUtils() {
    }

    /**
     * Return true if hostname represents the current machine.
     * A null or empty hostname is considered local, as is the
     * name "localhost".  Otherwise, all the IP addresses
     * corresponding to hostname are compared with all the IP addresses
     * corresponding to "localhost", as well as all the IP addresses
     * for all the network interfaces on this machine.  Note that
     * hostname can also be an IP address in string form.
     *
     * @return true if hostname is the local host
     */
    public static boolean isThisHostLocal(String hostname) {
        // optimize common cases
        if (hostname == null || hostname.length() == 0
                || hostname.equalsIgnoreCase("localhost"))
            return true;

        // now check all the addresses of "localhost"
        InetAddress hostAddrs[] = null;
        try {
            hostAddrs = InetAddress.getAllByName(hostname);
            assert hostAddrs != null;

            // any address that's a loopback address is a local address
            for (InetAddress ia : hostAddrs) {
                if (ia.isLoopbackAddress())
                    return true;
            }

            // are any of our addresses the same as any address of "localhost"?
            // XXX - redundant with the above check?
            for (InetAddress lia : InetAddress.getAllByName("localhost")) {
                for (InetAddress ia : hostAddrs) {
                    if (lia.equals(ia))
                        return true;
                }
            }
        }
        catch (UnknownHostException ex) {
            // ignore it
        }

        // it's not localhost, perhaps it's one of the addresses of this host?
        Enumeration<NetworkInterface> eni = null;
        try {
            eni = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException ex) {
            // ignore it
        }
        if (eni != null && hostAddrs != null) {
            while (eni.hasMoreElements()) {
                NetworkInterface ni = eni.nextElement();
                for (InterfaceAddress intf : ni.getInterfaceAddresses()) {
                    for (InetAddress ia : hostAddrs) {
                        if (intf.getAddress().equals(ia))
                            return true;
                    }
                }
            }
        }
        return false;   // nothing matched, not local

        /*
         * For reference, here's the old code...
         *
        // come on JDK!  Why is this so #$@% difficult!?!
        // if you can think of something better -- go for it!!!
        // use IP addresses because one hostname can have more than one IP address!
        List<String> host_ips = new ArrayList<String>();
        List<String> local_ips = new ArrayList<String>();
        String myCanonicalHostName = System.getProperty(SystemPropertyConstants.HOST_NAME_PROPERTY);

        try {
        if (!StringUtils.ok(myCanonicalHostName))
        myCanonicalHostName = getCanonicalHostName();

        InetAddress[] adds = InetAddress.getAllByName(hostname);

        if (adds == null || adds.length <= 0)
        return false;

        for (InetAddress ia : adds)
        host_ips.add(ia.getHostAddress());

        adds = InetAddress.getAllByName(myCanonicalHostName);
        for (int i = 0; adds != null && i < adds.length; i++) {
        String ip = adds[i].getHostAddress();
        if (!local_ips.contains(ip))
        local_ips.add(ip);
        }

        adds = InetAddress.getAllByName("localhost");
        for (int i = 0; adds != null && i < adds.length; i++) {
        String ip = adds[i].getHostAddress();
        if (!local_ips.contains(ip))
        local_ips.add(ip);
        }

        adds = InetAddress.getAllByName(null);
        for (int i = 0; adds != null && i < adds.length; i++) {
        String ip = adds[i].getHostAddress();
        if (!local_ips.contains(ip))
        local_ips.add(ip);
        }
        }
        catch (UnknownHostException ex) {
        return false;
        }

        // if any hostAddress matches any localAddress -- then the host is local...

        for (String hip : host_ips)
        for (String lip : local_ips)
        if (hip.equals(lip))
        return true;

        return false;
         */
    }

    /**
     * Painfully thorough error-handling.  Some would say over-engineered but I
     * plan on never looking at this code again!
     * @param host1
     * @param host2
     * @return
     */
    static public boolean isEqual(String host1, String host2) {
        List<String> host1_ips = new ArrayList<String>();
        List<String> host2_ips = new ArrayList<String>();

        try {
            if (!StringUtils.ok(host1) && !StringUtils.ok(host2))
                return true; // edge case ==> both are null or empty

            if (!StringUtils.ok(host1) || !StringUtils.ok(host2))
                return false; // just one of them is null or empty

            InetAddress[] adds1 = InetAddress.getAllByName(host1);
            InetAddress[] adds2 = InetAddress.getAllByName(host2);

            boolean adds1Empty = false; // readability.  You'll see why below!
            boolean adds2Empty = false;

            if (adds1.length <= 0)
                adds1Empty = true;

            if (adds2.length <= 0)
                adds2Empty = true;

            // I told you!
            if (adds1Empty && adds2Empty) // both
                return true;

            if (adds1Empty || adds2Empty) // one but not the other
                return false;

            for (InetAddress ia : adds1)
                host1_ips.add(ia.getHostAddress());

            for (InetAddress ia : adds2)
                host2_ips.add(ia.getHostAddress());

            for (String h1ip : host1_ips)
                for (String h2ip : host2_ips)
                    if (h1ip.equals(h2ip))
                        return true;

            return false;
        }
        catch (UnknownHostException ex) {
            return false;
        }
    }

    static public Socket getClientSocket(final String host, final int port, final int msecTimeout) {
        class SocketFetcher implements Runnable {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, port);
                }
                catch (Exception e) {
                    socket = null;
                }
            }

            Socket getSocket() {
                return socket;
            }
            private Socket socket;
        }
        ;

        SocketFetcher fetcher = new SocketFetcher();
        Thread t = new Thread(fetcher);

        t.start();
        try {
            t.join(msecTimeout);
        }
        catch (InterruptedException ex) {
            // It's probably not running at all
        }

        return fetcher.getSocket();
    }

    ///////////////////////////////////////////////////////////////////////////
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /**
     * This method returns the fully qualified name of the host.  If
     * the name can't be resolved (on windows if there isn't a domain specified), just
     * host name is returned
     *
     * @throws UnknownHostException so it can be handled on a case by case basis
     */
    public static String getCanonicalHostName() throws UnknownHostException {
        String hostname = null;
        String defaultHostname = InetAddress.getLocalHost().getHostName();

        // short-circuit out if user has reverse-DNS issues
        if (Boolean.parseBoolean(System.getenv("AS_NO_REVERSE_DNS"))) {
            return defaultHostname;
        }

        // look for full name
        hostname = InetAddress.getLocalHost().getCanonicalHostName();

        // check to see if ip returned or canonical hostname is different than hostname
        // It is possible for dhcp connected computers to have an erroneous name returned
        // that is created by the dhcp server.  If that happens, return just the default hostname
        if (hostname.equals(InetAddress.getLocalHost().getHostAddress())
                || !hostname.startsWith(defaultHostname)) {
            // don't want IP or canonical hostname, this will cause a lot of problems for dhcp users
            // get just plan host name instead
            hostname = defaultHostname;
        }

        return hostname;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static InetAddress[] getHostAddresses() {
        try {
            String hname = getHostName();

            if (hname == null) {
                return null;
            }

            return InetAddress.getAllByName(hname);
        }
        catch (Exception e) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public static String[] getHostIPs() {
        try {
            InetAddress[] adds = getHostAddresses();

            if (adds == null) {
                return null;
            }

            String[] ips = new String[adds.length];

            for (int i = 0; i < adds.length; i++) {
                String ip = trimIP(adds[i].toString());
                ips[i] = ip;
            }

            return ips;
        }
        catch (Exception e) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public static String trimIP(String ip) {
        if (ip == null || ip.length() <= 0) {
            return ip;
        }

        int index = ip.lastIndexOf('/');

        if (index >= 0) {
            return ip.substring(++index);
        }

        return ip;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static byte[] ip2bytes(String ip) {
        try {
            // possibilities:  "1.1.1.1", "frodo/1.1.1.1", "frodo.foo.com/1.1.1.1"

            ip = trimIP(ip);
            StringTokenizer stk = new StringTokenizer(ip, ".");

            byte[] bytes = new byte[stk.countTokens()];

            for (int i = 0; stk.hasMoreTokens(); i++) {
                String num = stk.nextToken();
                int inum = Integer.parseInt(num);
                bytes[i] = (byte) inum;
                //System.out.println("token: " + inum);
            }
            return bytes;
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean isLocalHost(String ip) {
        if (ip == null) {
            return false;
        }

        ip = trimIP(ip);

        return ip.equals(LOCALHOST_IP);
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean isLocal(String ip) {
        if (ip == null) {
            return false;
        }

        ip = trimIP(ip);

        if (isLocalHost(ip)) {
            return true;
        }

        String[] myIPs = getHostIPs();

        if (myIPs == null) {
            return false;
        }

        for (int i = 0; i < myIPs.length; i++) {
            if (ip.equals(myIPs[i])) {
                return true;
            }
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean isRemote(String ip) {
        return !isLocal(ip);
    }

    /**
     * Get the next free port (incrementing by 1)
     * @param hostName The host name on which the port is to be obtained
     * @param port The port number
     * @return The next incremental port number or 0 if a port cannot be found.
     */
    public static int getNextFreePort(String hostName, int port) {
        while (port++ < MAX_PORT) {
            if (isPortFree(hostName, port)) {
                return port;
            }
        }
        return 0;
    }

    /**
     * Returns a random port in the specified range
     * @param hostName The host on which the port is to be obtained.
     * @param startingPort starting port in the range
     * @param endingPort ending port in the range
     * @return the new port or 0 if the range is invalid.
     */
    public static int getFreePort(String hostName, int startingPort, int endingPort) {
        int range = endingPort - startingPort;
        int port = 0;
        if (range > 0) {
            Random r = new Random();
            while (true) {
                port = r.nextInt(range + 1) + startingPort;
                if (isPortFree(hostName, port)) {
                    break;
                }
            }
        }
        return port;
    }

    public static boolean isPortValid(int portNumber) {
        if (portNumber >= 0 && portNumber <= MAX_PORT) {
            return true;
        }
        else {
            return false;
        }
    }

    public static boolean isPortStringValid(String portNumber) {
        try {
            return isPortValid(Integer.parseInt(portNumber));
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean isPortFree(String hostName, int portNumber) {
        if (portNumber <= 0 || portNumber > MAX_PORT) {
            return false;
        }

        if (hostName == null || isThisMe(hostName)) {
            return isPortFreeServer(portNumber);
        }
        else {
            return isPortFreeClient(hostName, portNumber);
        }
    }

    /**
     * There are 4 possibilities when you want to setup a server socket on a port:
     * 1. The port is already in use
     * 2. The user does not have permission to open up shop on that port
     *    An example of (2) is a non-root user on UNIX trying to use port 80
     * 3. The port number is not in the legal range
     * 4. OK -- you can use it!
     *
     * @param portNumber
     * @return one of the 4 possibilities for this port
     */
    public static PortAvailability checkPort(int portNumber) {
        if (!isPortValid(portNumber))
            return PortAvailability.illegalNumber;

        // if we can setup a server socket on that port then it must be free.
        if (isPortFreeServer(portNumber))
            return PortAvailability.OK;

        if (isPortFreeClient(null, portNumber)) {
            // can not setup a server socket and can not connect as a client
            // that means we don't have permission...
            return PortAvailability.noPermission;
        }
        return PortAvailability.inUse;
    }

    public static boolean isPortFree(int portNumber) {
        return isPortFree(null, portNumber);
    }

    private static boolean isPortFreeClient(String hostName, int portNumber) {
        try {
            // WBN - I have no idea why I'm messing with these streams!
            // I lifted the code from installer.  Apparently if you just
            // open a socket on a free port and catch the exception something
            // will go wrong in Windows.
            // Feel free to change it if you know EXACTLY what you're doing

            //If the host name is null, assume localhost
            if (hostName == null) {
                hostName = getHostName();
            }
            Socket socket = new Socket(hostName, portNumber);
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            os.close();
            is.close();
            socket.close();
        }
        catch (Exception e) {
            // Nobody is listening on this port
            return true;
        }

        return false;
    }

    private static boolean isPortFreeServer(int port) {
        // check 3 different ip-port combinations.
        // Amazingly I have seen all 3 possibilities -- so just checking on 0.0.0.0
        // is not good enough.
        // Usually it is the 0.0.0.0 -- but JMS (default:7676)
        // only returns false from the "localhost":port combination.
        // We want to be aggressively disqualifying ports rather than the other
        // way around

        // JIRA 19391 April 2013  Byron Nevins
        // If DNS can not resolve the hostname, then
        // InetAddress.getLocalHost() will throw an UnknownHostException
        // Before this change we caught ALL Exceptions and returned false.
        // So if, say, the system has a bad hostname setup, this method
        // would say the port is in use.  Which probably is not true.
        // Change:  log it as a warning the first time and then log it as a FINE.

        try {
            byte[] allZero = new byte[]{0, 0, 0, 0};
            InetAddress add = InetAddress.getByAddress(allZero);

            if (isPortFreeServer(port, add) == false)
                return false;   // return immediately on "not-free"

            try {
                add = InetAddress.getLocalHost();
            }
            catch (UnknownHostException uhe) {
                // Byron says: We don't want to report this error over and over
                // and over again at WARNING level.
                Level level = Level.WARNING;

                // careful: many threads may call in here!
                synchronized (SYNC_OBJECT) {
                    if (badHostErrorWasReportedAlready)
                        level = Level.FINE;
                    else
                        badHostErrorWasReportedAlready = true;
                }
                logger.log(level, CULoggerInfo.badNetworkConfig, uhe.toString());
            }

            if (isPortFreeServer(port, add) == false)
                return false;   // return immediately on "not-free"

            add = InetAddress.getByName("localhost");

            return isPortFreeServer(port, add);
        }
        catch (Exception e) {
            // If we can't get an IP address then we can't check
            return false;
        }
    }

    private static boolean isPortFreeServer(int port, InetAddress add) {
        try {
            ServerSocket ss = new ServerSocket(port, 100, add);
            ss.close();

            printd(add.toString() + " : " + port + " --> FREE");
            return true;
        }
        catch (Exception e) {
            printd(add.toString() + " : " + port + " --> IN USE");
            return false;
        }
    }

    /**
    Gets a free port at the time of call to this method.
    The logic leverages the built in java.net.ServerSocket implementation
    which binds a server socket to a free port when instantiated with
    a port <code> 0 </code>.
    <P> Note that this method guarantees the availability of the port
    only at the time of call. The method does not bind to this port.
    <p> Checking for free port can fail for several reasons which may
    indicate potential problems with the system. This method acknowledges
    the fact and following is the general contract:
    <li> Best effort is made to find a port which can be bound to. All
    the exceptional conditions in the due course are considered SEVERE.
    <li> If any exceptional condition is experienced, <code> 0 </code>
    is returned, indicating that the method failed for some reasons and
    the callers should take the corrective action. (The method need not
    always throw an exception for this).
    <li> Method is synchronized on this class.
    @return integer depicting the free port number available at this time
    0 otherwise.
     */
    public static int getFreePort() {
        int freePort = 0;
        boolean portFound = false;
        ServerSocket serverSocket = null;

        synchronized (NetUtils.class) {
            try {
                /*following call normally returns the free port,
                to which the ServerSocket is bound. */
                serverSocket = new ServerSocket(0);
                freePort = serverSocket.getLocalPort();
                portFound = true;
            }
            catch (Exception e) {
                //squelch the exception
            }
            finally {
                if (!portFound) {
                    freePort = 0;
                }
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                        if (!serverSocket.isClosed()) {
                            throw new Exception("local exception ...");
                        }
                    }
                }
                catch (Exception e) {
                    //squelch the exception
                    freePort = 0;
                }
            }
            return freePort;
        }
    }

    /**
     *	This method accepts a hostname and port #.  It uses this information
     *	to attempt to connect to the port, send a test query, analyze the
     *	result to determine if the port is secure or unsecure (currently only
     *	http / https is supported).
     *  @param hostname - String name of the host where the connections has to be made
     *  @param port - int name of the port where the connections has to be made
     *  @return admin password
     *  @throws IOException if an error occurs during the connection
     *  @throws ConnectException if an error occurred while attempting to connect a socket to a remote address and port.
     *  @throws SocketTimeoutException if timeout(4s) expires before connecting
     */
    public static boolean isSecurePort(String hostname, int port) throws IOException, ConnectException, SocketTimeoutException {
        // Open the socket w/ a 4 second timeout
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(hostname, port), 4000);

        // Send an https query (w/ trailing http query)
        java.io.OutputStream ostream = socket.getOutputStream();
        ostream.write(TEST_QUERY);

        // Get the result
        java.io.InputStream istream = socket.getInputStream();
        int count = 0;
        while (count < 20) {
            // Wait up to 4 seconds
            try {
                if (istream.available() > 0) {
                    break;
                }
                Thread.sleep(200);
            }
            catch (InterruptedException ex) {
            }
            count++;
        }
        byte[] input = new byte[istream.available()];
        int read = istream.read(input);

        // Close the socket
        socket.close();

        // Determine protocol from result
        // Can't read https response w/ OpenSSL (or equiv), so use as
        // default & try to detect an http response.
        String response = new String(input).toLowerCase(Locale.ENGLISH);
        boolean isSecure = true;
        if (read <= 0 || response.length() == 0) {
            isSecure = false;
        }
        else if (response.startsWith("http/1.")) {
            isSecure = false;
        }
        else if (response.indexOf("<html") != -1) {
            isSecure = false;
        }
        else if (response.indexOf("connection: ") != -1) {
            isSecure = false;
        }
        return isSecure;
    }

    /**
     * There is sometimes a need for subclasses to know if a
     * <code> local domain </code> is running. An example of such a command is
     * change-master-password command. The stop-domain command also needs to
     * know if a domain is running <i> without </i> having to provide user
     * name and password on command line (this is the case when I own a domain
     * that has non-default admin user and password) and want to stop it
     * without providing it.
     * <p>
     * In such cases, we need to know if the domain is running and this method
     * provides a way to do that.
     * @param the timeout in milliseconds
     * @return boolean indicating whether the server is running
     */
    public static boolean isRunning(String host, int port, int timeoutMilliseconds) {
        Socket server = new Socket();

        try {
            if (host == null)
                host = InetAddress.getByName(null).getHostName();

            InetSocketAddress whom = new InetSocketAddress(host, port);
            server.connect(whom, timeoutMilliseconds);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
        finally {
            try {
                server.close();
            }
            catch (IOException ex) {
                // nothing to do
            }
        }
    }

    public static boolean isRunning(String host, int port) {
        return isRunning(host, port, IS_RUNNING_DEFAULT_TIMEOUT);
    }

    /**
     * convenience method for the local machine
     */
    public static final boolean isRunning(int port) {
        return isRunning(null, port);
    }
    ///////////////////////////////////////////////////////////////////////////
    private static final String LOCALHOST_IP = "127.0.0.1";

    ///////////////////////////////////////////////////////////////////////////
    private static boolean isThisMe(String hostname) {
        try {
            InetAddress[] myadds = getHostAddresses();
            InetAddress[] theiradds = InetAddress.getAllByName(hostname);

            for (int i = 0; i < theiradds.length; i++) {
                if (theiradds[i].isLoopbackAddress()) {
                    return true;
                }

                for (int j = 0; j < myadds.length; j++) {
                    if (myadds[j].equals(theiradds[i])) {
                        return true;
                    }
                }
            }
        }
        catch (Exception e) {
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        System.out.println("80: " + isPortFree(80));
        System.out.println("777: " + isPortFree(777));
        System.out.println("8000: " + isPortFree(8000));
        System.out.println("");
        timeit(null, 12345);
        timeit(null, 4848);
        timeit("10.0.4.5", 1234); // unlikely IP address
    }

    private static void timeit(String h, int p) {
        long start = System.currentTimeMillis();
        boolean b = isRunning(h, p);
        long duration = System.currentTimeMillis() - start;
        System.out.printf("isRunning says: %b for %s, %d, %d%n", b, h, p, duration);
    }
    /**
     *	This is the test query used to ping the server in an attempt to
     *	determine if it is secure or not.
     */
    private static byte[] TEST_QUERY = new byte[]{
        // The following SSL query is from nmap (http://www.insecure.org)
        // This HTTPS request should work for most (all?) https servers
        (byte) 0x16, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 'S', (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 'O', (byte) 0x03, (byte) 0x00, (byte) '?',
        (byte) 'G', (byte) 0xd7, (byte) 0xf7, (byte) 0xba, (byte) ',', (byte) 0xee,
        (byte) 0xea, (byte) 0xb2, (byte) '`', (byte) '~', (byte) 0xf3, (byte) 0x00,
        (byte) 0xfd, (byte) 0x82, (byte) '{', (byte) 0xb9, (byte) 0xd5, (byte) 0x96,
        (byte) 0xc8, (byte) 'w', (byte) 0x9b, (byte) 0xe6, (byte) 0xc4, (byte) 0xdb,
        (byte) '<', (byte) '=', (byte) 0xdb, (byte) 'o', (byte) 0xef, (byte) 0x10,
        (byte) 'n', (byte) 0x00, (byte) 0x00, (byte) '(', (byte) 0x00, (byte) 0x16,
        (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 'f',
        (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 'e',
        (byte) 0x00, (byte) 'd', (byte) 0x00, (byte) 'c', (byte) 0x00, (byte) 'b',
        (byte) 0x00, (byte) 'a', (byte) 0x00, (byte) '`', (byte) 0x00, (byte) 0x15,
        (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x14,
        (byte) 0x00, (byte) 0x11, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x00,
        // The following is a HTTP request, some HTTP servers won't
        // respond unless the following is also sent
        (byte) 'G', (byte) 'E', (byte) 'T', (byte) ' ', (byte) '/', (byte) ' ',
        (byte) 'H', (byte) 'T', (byte) 'T', (byte) 'P', (byte) '/', (byte) '1',
        (byte) '.', (byte) '0', (byte) '\n', (byte) '\n'
    };
}
