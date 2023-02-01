package com.infodesire.jvmcom.netty;

/**
 * Base class for configurations of a server
 *
 */
public class ServerConfig {

    public int port;

    public boolean useSSL = false;

    public String bossThreadName;

    public String workerThreadName;

}
