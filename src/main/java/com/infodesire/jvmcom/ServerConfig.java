package com.infodesire.jvmcom;

/**
 * Configuration parameters of a server
 */
public class ServerConfig {

  /**
   * Port to listen on. 0 means random free port.
   */
  public int port = 0;

  /**
   * Max number of parallel worker threds to handle requests
   */
  public int threadCount = 10;

  /**
   * Optional name pattern for threads. Can contain %d
   */
  public String threadNamePattern;

}
