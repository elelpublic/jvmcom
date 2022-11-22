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
   * Make threads names easier to read for debugging:
   * Optional name pattern for the server thread. Can contain %d
   */
  public String serverThreadNamePattern;

  /**
   * Make threads names easier to read for debugging:
   * Optional name pattern for workers. Can contain %d
   */
  public String workerThreadNamePattern;

}
