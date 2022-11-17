package com.infodesire.jvmcom;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public class PerformanceTest {


  @Test
  public void testCommunication() throws IOException {

    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );

    StringBuffer reply;

    Server server = new Server( 0, 3 );

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


  int DATA_SIZE = 100000;

  @Test
  public void testBigData() throws IOException {

    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "error" );
    Server server = new Server( 0, 3 );
    server.start();

    int port = server.getPort();

    Client client = new Client( "127.0.0.1", port );
    client.connect( false );

    for( int i = 0; i < DATA_SIZE; i++ ) {
      client.send( "put main v-" + i + " " + i );
    }

    StringBuffer reply = client.send( "size main" );
    assertEquals( DATA_SIZE, Integer.parseInt( reply.toString() ) );

  }

  @Test
  public void testBigDataLocal() throws IOException {

    ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    for( int i = 0; i < DATA_SIZE; i++ ) {
      map.put( "v-" + i, "" + i );
    }

    assertEquals( DATA_SIZE, map.size() );

  }


}