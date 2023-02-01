package com.infodesire.jvmcom.netty;

/**
 * Base configuration class for all clients
 */
public class ClientConfig {

    public String host;

    public int port;

    public boolean useSSL = false;

    public String workerThreadName;

}
