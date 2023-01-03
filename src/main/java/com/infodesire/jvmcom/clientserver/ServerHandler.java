package com.infodesire.jvmcom.clientserver;

import java.net.InetSocketAddress;

/**
 * Handler for requests to the server
 */
public interface ServerHandler {

    /**
     * Set sender of requests
     *
     * @param senderAddress Sender of requests
     *
     */
    void setSender( InetSocketAddress senderAddress );

}
