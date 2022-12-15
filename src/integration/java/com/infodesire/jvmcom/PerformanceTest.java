package com.infodesire.jvmcom;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.services.value.ValueServer;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

public class PerformanceTest {


  @Test
  public void testCommunication() throws IOException {

    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );

    CharSequence reply;

    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    config.serverThreadNamePattern = "SERVER__";
    config.workerThreadNamePattern = "WORKER-%d";
    ValueServer server = new ValueServer( config );

    // make threads easier to read for development
    Thread.currentThread().setName( "MAIN____" );

    server.start();
    int port = server.getPort();

    try( LineBufferClient client = new LineBufferClient( "127.0.0.1", port ) ) {

      client.connect( false );
      reply = client.send( "put main version 1" );
      assertEquals( "OK", reply.toString() );

    }

  }


  int DATA_SIZE = 10000;

  @Test
  public void testBigData() throws IOException {

    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "error" );
    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    ValueServer server = new ValueServer( config );
    server.start();

    int port = server.getPort();

    LineBufferClient client = new LineBufferClient( "127.0.0.1", port );
    client.connect( false );

    for( int i = 0; i < DATA_SIZE; i++ ) {
      client.send( "put main v-" + i + " " + i );
    }

    CharSequence reply = client.send( "size main" );
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


  @Test
  public void testStringBuilderVsStringBufferVsString() {

    int COUNT = 10000;

    long t0 = System.currentTimeMillis();
    StringBuilder sbi = new StringBuilder();
    for( int i = 0; i < COUNT; i++ ) {
      sbi.append( i );
    }
    System.out.println( "StringBuilder " + (System.currentTimeMillis() - t0) + " ms" );

    t0 = System.currentTimeMillis();
    StringBuffer sbu = new StringBuffer();
    for( int i = 0; i < COUNT; i++ ) {
      sbu.append( i );
    }
    System.out.println( "StringBuffer " + (System.currentTimeMillis() - t0) + " ms" );

    t0 = System.currentTimeMillis();
    String s = "";
    for( int i = 0; i < COUNT; i++ ) {
      s += i;
    }
    System.out.println( "String " + (System.currentTimeMillis() - t0) + " ms" );

    int SUBLENGTH = 100;
    System.out.println( "With constructors every " + SUBLENGTH + " loops:" );

    t0 = System.currentTimeMillis();
    StringBuilder sbi2 = new StringBuilder();
    for( int i = 0; i < COUNT; i++ ) {
      if( i % SUBLENGTH == 0 ) {
        sbi2 = new StringBuilder();
      }
      sbi2.append( i );
    }
    System.out.println( "StringBuilder " + (System.currentTimeMillis() - t0) + " ms" );

    t0 = System.currentTimeMillis();
    StringBuffer sbu2 = new StringBuffer();
    for( int i = 0; i < COUNT; i++ ) {
      if( i % SUBLENGTH == 0 ) {
        sbu2 = new StringBuffer();
      }
      sbu2.append( i );
    }
    System.out.println( "StringBuffer " + (System.currentTimeMillis() - t0) + " ms" );

    t0 = System.currentTimeMillis();
    String s2 = "";
    for( int i = 0; i < COUNT; i++ ) {
      if( i % SUBLENGTH == 0 ) {
        s2 = "";
      }
      s2 += i;
    }
    System.out.println( "String " + (System.currentTimeMillis() - t0) + " ms" );

  }


}