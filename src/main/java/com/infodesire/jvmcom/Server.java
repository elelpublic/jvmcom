package com.infodesire.jvmcom;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.infodesire.jvmcom.ProtocolParser.Token;
import static com.infodesire.jvmcom.ProtocolParser.parseFirstWord;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Server {


  private int port;
  private boolean shutdown = false;
  private int threadCount = 10;
  private ConcurrentHashMap<String, Map<String, String>> maps = new ConcurrentHashMap<>(
    10, // initial capacity
    0.8f, // load factor
    10 // concurrency level (number of concurrent modifications)
    );
  private Thread serverThread;


  /**
   * Make threads names easier to read for debugging
   */
  protected String serverThreadName, workerThreadName;

  private static Logger logger = LoggerFactory.getLogger( "Server" );


  public Server( int port ) {
    this.port = port;
  }


  public void start() throws IOException {

    ServerSocket serverSocket = new ServerSocket( port );
    if( port == 0 ) {
      port = serverSocket.getLocalPort();
    }

    stop();

    serverThread = new Thread() {
      public void run() {
        if( serverThreadName != null ) {
          Thread.currentThread().setName( serverThreadName );
        }
        ExecutorService executorService = Executors.newFixedThreadPool( threadCount );
        while( !shutdown ) {
          logger.info( "Waiting for connections on port " + port );
          Socket socket = null;
          try {
            socket = serverSocket.accept();
          }
          catch( IOException ex ) {
            ex.printStackTrace();
          }
          executorService.submit( new Worker( socket ) );
        }
        executorService.shutdownNow();
      }
    };

    shutdown = false;
    serverThread.start();

  }


  public void waitUntilFinish() throws InterruptedException {
    serverThread.join();
  }

  private void stop() {
    shutdown = true;
    if( serverThread != null ) {
      try {
        serverThread.join(100);
      }
      catch( InterruptedException ex ) {}
      finally {
        try {
          if( serverThread.isAlive() ) {
            serverThread.interrupt();
          }
        }
        catch( Exception ex ) {
          ex.printStackTrace();
        }
        serverThread = null;
      }
    }
  }

  public int getPort() {
    return port;
  }

  private static AtomicLong workerCounter = new AtomicLong();

  class Worker implements Runnable {

    private final Socket socket;
    private PrintWriter writer;

    public Worker( Socket socket ) {
      this.socket = socket;
    }

    @Override
    public void run() {
      if( workerThreadName != null ) {
        Thread.currentThread().setName( String.format( workerThreadName, workerCounter.incrementAndGet() ) );
      }
      logger.info( "Accepted new connection." );
      InputStream in = null;
      OutputStream out = null;
      try {
        in = socket.getInputStream();
        out = socket.getOutputStream();
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        writer = new PrintWriter( new OutputStreamWriter( out ) );
        send( "Welcome!%n" );
        while( true ) {
          String line = reader.readLine();
          logger.info( "Client request: '" + line + "'" );
          if( line.equals( "help" ) ) {
            printHelp( writer );
          }
          else if( line.equals( "bye" ) ) {
            send( "Bye. Closing connection now.%n" );
            socket.close();
            return;
          }
          else {
            Token command = parseFirstWord( line );
            if( command.word.equals( "put" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, true );
              if( mapAndKey != null ) {
                Map map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                if( isEmpty( valueName.restOfLine ) ) {
                  send( "Error: value must not be null" );
                }
                else {
                  map.put( valueName.word, valueName.restOfLine );
                }
              }
              send( "OK%n" );
            }
            else if( command.word.equals( "get" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                String value = map.get( valueName.word );
                send( "%s%n", value );
              }
            }
            else if( command.word.equals( "has" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                boolean result = map.containsKey( valueName.word );
                send( "%s%n", result );
              }
            }
            else if( command.word.equals( "remove" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                String value = map.remove( valueName.word );
                send( "%s%n", value );
              }
            }
            else if( command.word.equals( "size" ) ) {
              Map<String, String> map = getMap( command );
              if( map == null ) {
                send( "Error: No map found named %s%n", command.restOfLine );
              }
              else {
                send( "%d%n", map.size() );
              }
            }
            else if( command.word.equals( "clear" ) ) {
              Map<String, String> map = getMap( command );
              if( map == null ) {
                send( "Error: No map found named %s%n", command.restOfLine );
              }
              else {
                map.clear();
                send( "OK%n" );
              }
            }
            else {
              send( "Error: Unknown command '%s' (try 'help' for a list of commands) %n", line );
            }
          }
        }
      }
      catch( IOException ex ) {
        ex.printStackTrace();
      }
      finally {
        if( in != null ) {
          try {
            in.close();
          }
          catch( IOException ex ) {}
        }
        if( out != null ) {
          try {
            out.close();
          }
          catch( IOException ex ) {}
        }
      }
      logger.info( "Connection closed." );
    }

    private Map<String, String> getMap( Token command ) {

      Token mapName = parseFirstWord( command.restOfLine );
      if( isEmpty( mapName.word ) ) {
        send( "Error: no map name given" );
      }
      else {
        return getMapImpl( mapName.word, false );
      }

      return null;

    }

    private Pair<Map<String, String>,Token> getMapAndKey( Token command, boolean createIfMissing ) {

      Token mapName = parseFirstWord( command.restOfLine );
      if( isEmpty( mapName.word ) ) {
        send( "Error: no map name given" );
      }
      else {
        Map map = getMapImpl( mapName.word, createIfMissing );
        if( map == null ) {
          send( "Error: No map found named %s%n", mapName.word );
        }
        else {
          Token valueName = parseFirstWord( mapName.restOfLine );
          if( isEmpty( valueName.word ) ) {
            send( "Error: no value name given" );
          }
          else {
            return new ImmutablePair<>( map, valueName );
          }
        }
      }

      return null;

    }

    private void send( String line, Object... parames ) {
      String reply = String.format( line, parames );
      logger.debug( "Sending: " + reply.trim() );
      writer.print( reply );
      writer.flush();
    }

    private void printHelp( PrintWriter writer ) {
      writer.println( "" );
      writer.println( "Available commands:" );
      writer.println( "" );
      writer.println( "help ................... show information on how to use the server" );
      writer.println( "bye .................... close connection" );
      writer.println( "put map key value ...... put value into map named 'map' under the given key" );
      writer.println( "get map key ............ get value from map named 'map' under the given key" );
      writer.println( "has map key ............ check if map named map contains given key" );
      writer.println( "remove map key ......... remove key from map" );
      writer.println( "size map ............... size of map" );
      writer.println( "clear map .............. remove all entries from map" );
      writer.println( "" );
      writer.flush();
    }

  }

  private Map getMapImpl( String nameOfMap, boolean createIfMissing ) {
    if( createIfMissing ) {
      return maps.computeIfAbsent( nameOfMap, (key) -> { return new ConcurrentHashMap<>(); } );
    }
    else {
      return maps.get( nameOfMap );
    }
  }


}
