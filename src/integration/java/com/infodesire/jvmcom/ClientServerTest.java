package com.infodesire.jvmcom;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ClientServerTest {


  @Test
  public void testCommunication() throws IOException {

    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );

    StringBuffer reply;

    Server server = new Server( 0 );

    // make threads easier to read for development
    Thread.currentThread().setName( "MAIN____" );
    server.serverThreadName = "SERVER__";
    server.workerThreadName = "WORKER-%d";

    server.start();
    int port = server.getPort();

    Client client = new Client( "127.0.0.1", port );
    client.connect( false );

    reply = client.send( "put main version 1" );

    assertEquals( "OK", reply.toString() );

  }

}