package com.infodesire.jvmcom.services;

import com.infodesire.jvmcom.mesh.ServiceConfig;

import java.io.IOException;

public interface Service {

  String getName();

  /**
   * @return Real port after starting service (not configured port)
   */
  int getPort();

  void start() throws IOException;

  void stop( long timeoutMs ) throws InterruptedException;

}
