package com.infodesire.jvmcom.servers;

import com.infodesire.jvmcom.ServerWorker;
import com.infodesire.jvmcom.SocketManager;
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
import java.util.function.Supplier;

import static com.infodesire.jvmcom.servers.ProtocolParser.Token;
import static com.infodesire.jvmcom.servers.ProtocolParser.parseFirstWord;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class OldValuesServer {

  private int port;
  private int threadCount;
  private ConcurrentHashMap<String, Map<String, String>> maps = new ConcurrentHashMap<>(
    10, // initial capacity
    0.8f, // load factor
    10 // concurrency level (number of concurrent modifications)
    );
  private SocketManager serverSocketManager;


  /**
   * Make threads names easier to read for debugging
   */
  private String serverThreadName, workerThreadName;

  private static Logger logger = LoggerFactory.getLogger( "Server" );


  /**
   * Create server. To find a random free port, pass 0 as port. You can get the port number AFTER
   * calling start using getPort().
   *
   * @param port Listening port - 0 for random free port
   *
   */
  public OldValuesServer( int port, int threadCount ) {
    this.port = port;
    this.threadCount = threadCount;
  }


  public void start() throws IOException {

    logger.info( "Trying to start server on port " + port );

    ServerSocket serverSocket = new ServerSocket( port );
    if( port == 0 ) {
      port = serverSocket.getLocalPort();
      logger.info( "Free port found " + port );
    }

    serverSocket.close();

    serverSocketManager = new SocketManager( port, threadCount,
      new WorkerFactory( workerThreadName ), serverThreadName );

  }

  public int getPort() {
    return port;
  }

  public void stop( long timeoutMs ) throws InterruptedException {
    serverSocketManager.stop( timeoutMs );
  }

  public void setServerThreadName( String serverThreadName ) {
    this.serverThreadName = serverThreadName;
  }

  public void setWorkerThreadName( String workerThreadName ) {
    this.workerThreadName = workerThreadName;
  }

  public void waitForShutDown() throws InterruptedException {
    serverSocketManager.waitForShutDown();
  }


  class WorkerFactory implements Supplier<ServerWorker> {

    private String threadName;

    WorkerFactory( String threadName ) {
      this.threadName = threadName;
    }

    @Override
    public ServerWorker get() {
      return new Worker( threadName );
    }

  }

  class Worker implements ServerWorker {

    private final String threadName;
    private PrintWriter writer;
    private boolean stopRequest = false;

    Worker( String threadName ) {
      this.threadName = threadName;
    }

    @Override
    public void accept( Socket socket ) {

      if( threadName != null ) {
        Thread.currentThread().setName( threadName );
      }
      logger.info( "Accepted new connection." );
      InputStream in = null;
      OutputStream out = null;
      try {
        in = socket.getInputStream();
        out = socket.getOutputStream();
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        writer = new PrintWriter( new OutputStreamWriter( out ) );
        sendReply( "Welcome!%n" );
        while( !stopRequest ) {
          String line = null;
          try {
            line = reader.readLine();
            logger.info( "Client request: '" + line + "'" );
          }
          catch( IOException ex ) {}
          if( line == null ) {
            stopRequest = true;
            break; // null means end of stream
          }
          if( line.equals( "help" ) ) {
            printHelp( writer );
          }
          else if( line.equals( "bye" ) ) {
            sendReply( "Bye. Closing connection now.%n" );
            socket.close();
            return;
          }
          else {
            Token command = parseFirstWord( line );
            if( command.word.equals( "put" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, true );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                if( isEmpty( valueName.restOfLine ) ) {
                  sendReply( "Error: value must not be null" );
                }
                else {
                  map.put( valueName.word, valueName.restOfLine );
                }
              }
              sendReply( "OK%n" );
            }
            else if( command.word.equals( "get" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                String value = map.get( valueName.word );
                sendReply( "%s%n", value );
              }
            }
            else if( command.word.equals( "has" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                boolean result = map.containsKey( valueName.word );
                sendReply( "%s%n", result );
              }
            }
            else if( command.word.equals( "remove" ) ) {
              Pair<Map<String, String>,Token> mapAndKey = getMapAndKey( command, false );
              if( mapAndKey != null ) {
                Map<String, String> map = mapAndKey.getLeft();
                Token valueName = mapAndKey.getRight();
                String value = map.remove( valueName.word );
                sendReply( "%s%n", value );
              }
            }
            else if( command.word.equals( "size" ) ) {
              Map<String, String> map = getMap( command );
              if( map == null ) {
                sendReply( "Error: No map found named %s%n", command.restOfLine );
              }
              else {
                sendReply( "%d%n", map.size() );
              }
            }
            else if( command.word.equals( "clear" ) ) {
              Map<String, String> map = getMap( command );
              if( map == null ) {
                sendReply( "Error: No map found named %s%n", command.restOfLine );
              }
              else {
                map.clear();
                sendReply( "OK%n" );
              }
            }
            else if( command.word.equals( "ping" ) ) {
              sendReply( "OK%n" );
            }
            else {
              sendReply( "Error: Unknown command '%s' (try 'help' for a list of commands) %n", line );
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
        sendReply( "Error: no map name given" );
      }
      else {
        return getMapImpl( mapName.word, false );
      }

      return null;

    }

    private Pair<Map<String, String>,Token> getMapAndKey( Token command, boolean createIfMissing ) {

      Token mapName = parseFirstWord( command.restOfLine );
      if( isEmpty( mapName.word ) ) {
        sendReply( "Error: no map name given" );
      }
      else {
        Map<String, String> map = getMapImpl( mapName.word, createIfMissing );
        if( map == null ) {
          sendReply( "Error: No map found named %s%n", mapName.word );
        }
        else {
          Token valueName = parseFirstWord( mapName.restOfLine );
          if( isEmpty( valueName.word ) ) {
            sendReply( "Error: no value name given" );
          }
          else {
            return new ImmutablePair<>( map, valueName );
          }
        }
      }

      return null;

    }

    private void sendReply( String line, Object... parames ) {
      String reply = String.format( line, parames );
      logger.debug( "Sending reply: " + reply.trim() );
      writer.print( reply );
      writer.flush();
    }

    private void printHelp( PrintWriter writer ) {
      writer.println( "" );
      writer.println( "Available commands:" );
      writer.println( "" );
      writer.println( "help ................... show information on how to use the server" );
      writer.println( "ping ................... will reply with OK when running" );
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

    @Override
    public void requestStop() {
      stopRequest = true;
    }

  }

  private Map<String, String> getMapImpl( String nameOfMap, boolean createIfMissing ) {
    if( createIfMissing ) {
      return maps.computeIfAbsent( nameOfMap, (key) -> new ConcurrentHashMap<>() );
    }
    else {
      return maps.get( nameOfMap );
    }
  }


}
