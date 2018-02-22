package fish.payara.kernel.services.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.v3.services.impl.GlassfishNetworkListener;
import com.sun.enterprise.v3.services.impl.GrizzlyService;

import org.glassfish.grizzly.config.dom.NetworkListener;

public class MicroNetworkListener extends GlassfishNetworkListener {

    private static final Logger LOGGER = Logger.getLogger(MicroNetworkListener.class.getName());

    private static Map<Integer, ServerSocket> reservedSocketMap = new HashMap<>();

    public MicroNetworkListener(final GrizzlyService grizzlyService, final NetworkListener networkListener,
            final Logger logger) {
        super(grizzlyService, networkListener, logger);
    }

    @Override
    public void start() throws IOException {
        if (reservedSocketMap.containsKey(port)) {
            ServerSocket reservedSocket = reservedSocketMap.get(port);
            LOGGER.log(Level.INFO, "Found reserved socket on port: {0,number,#}.", port);
            if (reservedSocket.isBound()) {
                reservedSocket.close();
                reservedSocketMap.remove(port);
            }
        }
        super.start();
    }

    public static void addReservedSocket(int boundPort, ServerSocket socket) {
        LOGGER.log(Level.INFO, "Reserving port: {0,number,#}", boundPort);
        reservedSocketMap.put(boundPort, socket);
    }

    public static void clearReservedSockets() throws IOException {
        for (ServerSocket socket : reservedSocketMap.values()) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
        reservedSocketMap.clear();
    }

}