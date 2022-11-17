package com.infodesire.jvmcom;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class LifecycleTest {


  @Test
  public void testStartupShutdown() throws IOException, InterruptedException {

    Server server = new Server( 0, 3 );
    server.start();

    int port = server.getPort();

    Client client = new Client( "localhost", port );
    client.connect( false );
    assertTrue( client.ping() );

    server.stop( 100 );
    server.start();

    assertEquals( port, server.getPort() );

    client.close();

  }

}
