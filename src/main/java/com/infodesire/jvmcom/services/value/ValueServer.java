package com.infodesire.jvmcom.services.value;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.SocketManager;
import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.text.TextHandler;
import com.infodesire.jvmcom.clientserver.text.TextServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.infodesire.jvmcom.services.ProtocolParser.Token;
import static com.infodesire.jvmcom.services.ProtocolParser.parseFirstWord;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ValueServer {

  private static final Logger logger = LoggerFactory.getLogger( "Server" );

  private final ServerConfig config;
  private final TextServer server;
  private final ConcurrentHashMap<String, Map<String, String>> maps = new ConcurrentHashMap<>(
    10, // initial capacity
    0.8f, // load factor
    10 // concurrency level (number of concurrent modifications)
    );
  private SocketManager serverSocketManager;


  /**
   * Create server. To find a random free port, pass 0 as port. You can get the port number AFTER
   * calling start using getPort().
   *
   * @param config Configuration parameters of a server
   *
   */
  public ValueServer( ServerConfig config ) {
    this.config = config;
    server = new TextServer( config, new HandlerFactory() );
  }

  public void start() throws IOException {
    server.start();
  }

  class HandlerFactory implements Supplier<TextHandler> {

    @Override
    public TextHandler get() {
      return new MappedValuesHandler();
    }

  }

  public int getPort() {
    return server.getPort();
  }

  public void stop( long timeoutMs ) throws InterruptedException {
    server.stop( timeoutMs );
  }

  public void waitForShutDown() throws InterruptedException {
    server.waitForShutDown();
  }


  class MappedValuesHandler implements TextHandler {

    private InetSocketAddress sender;

    @Override
    public void setSender( InetSocketAddress senderAddress ) {
      this.sender = senderAddress;
    }

    public HandlerReply process( String line ) {

      if( line.equals( "help" ) ) {
        return new HandlerReply( printHelp().toString() );
      }
      else if( line.equals( "bye" ) ) {
        return new HandlerReply( false, "Bye. Closing connection now." );
      }
      else {
        Token command = parseFirstWord( line );
        if( command.word.equals( "put" ) ) {
          Triple<Map<String, String>,Token,String> mapAndKey = getMapAndKeyAndError( command, true );
          String error = mapAndKey.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else {
            Map<String, String> map = mapAndKey.getLeft();
            Token valueName = mapAndKey.getMiddle();
            if( isEmpty( valueName.restOfLine ) ) {
              return new HandlerReply( "Error " + ValueError.VALUE_MUST_NO_BE_NULL );
            }
            else {
              map.put( valueName.word, valueName.restOfLine );
            }
          }
          return new HandlerReply( "OK" );
        }
        else if( command.word.equals( "get" ) ) {
          Triple<Map<String, String>,Token,String> mapAndKey = getMapAndKeyAndError( command, false );
          String error = mapAndKey.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else {
            Map<String, String> map = mapAndKey.getLeft();
            Token valueName = mapAndKey.getMiddle();
            String value = map.get( valueName.word );
            return new HandlerReply( value == null ? "" : value.toString() );
          }
        }
        else if( command.word.equals( "has" ) ) {
          Triple<Map<String, String>,Token,String> mapAndKey = getMapAndKeyAndError( command, false );
          String error = mapAndKey.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else {
            Map<String, String> map = mapAndKey.getLeft();
            Token valueName = mapAndKey.getMiddle();
            boolean result = map.containsKey( valueName.word );
            return new HandlerReply( "" + result );
          }
        }
        else if( command.word.equals( "remove" ) ) {
          Triple<Map<String, String>,Token,String> mapAndKey = getMapAndKeyAndError( command, false );
          String error = mapAndKey.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else {
            Map<String, String> map = mapAndKey.getLeft();
            Token valueName = mapAndKey.getMiddle();
            String value = map.remove( valueName.word );
            return new HandlerReply( value );
          }
        }
        else if( command.word.equals( "size" ) ) {
          Pair<Map<String, String>,String> mapAndError = getMapAndError( command );
          String error = mapAndError.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else if( mapAndError.getLeft() == null ) {
            return new HandlerReply( "Error " + ValueError.NO_SUCH_MAP + " " + command.restOfLine );
          }
          else {
            return new HandlerReply( "" + mapAndError.getLeft().size() );
          }
        }
        else if( command.word.equals( "clear" ) ) {
          Pair<Map<String, String>,String> mapAndError = getMapAndError( command );
          String error = mapAndError.getRight();
          if( error != null ) {
            return new HandlerReply( error );
          }
          else if( mapAndError.getLeft() == null ) {
            return new HandlerReply( "Error " + ValueError.NO_SUCH_MAP + " " + command.restOfLine );
          }
          else {
            mapAndError.getLeft().clear();
            return new HandlerReply( "OK" );
          }
        }
        else if( command.word.equals( "ping" ) ) {
          return new HandlerReply( "OK" );
        }
        else {
          return new HandlerReply( String.format(
            "Error: Unknown command '%s' (try 'help' for a list of commands)", line ) );
        }
      }

    }

    private Pair<Map<String, String>,String> getMapAndError( Token command ) {

      Token mapName = parseFirstWord( command.restOfLine );
      if( isEmpty( mapName.word ) ) {
        return new ImmutablePair<>( null, "Error " + ValueError.MAP_NAME_MISSING );
      }
      else {
        return new ImmutablePair<>( getMapImpl( mapName.word, false ), null );
      }

    }

    private Triple<Map<String, String>,Token,String> getMapAndKeyAndError( Token command, boolean createIfMissing ) {

      Token mapName = parseFirstWord( command.restOfLine );
      if( isEmpty( mapName.word ) ) {
        return new ImmutableTriple<>( null, null, "Error " + ValueError.MAP_NAME_MISSING );
      }
      else {
        Map<String, String> map = getMapImpl( mapName.word, createIfMissing );
        if( map == null ) {
          return new ImmutableTriple<>( null, null, "Error " + ValueError.NO_SUCH_MAP + " " + mapName.word );
        }
        else {
          Token valueName = parseFirstWord( mapName.restOfLine );
          if( isEmpty( valueName.word ) ) {
            return new ImmutableTriple<>( null, null, "Error " + ValueError.VALUE_NAME_MISSING );
          }
          else {
            return new ImmutableTriple<>( map, valueName, null );
          }
        }
      }

    }

    private StringJoiner printHelp() {
      StringJoiner result = new StringJoiner( "\n" );
      result.add( "" );
      result.add( "Available commands:" );
      result.add( "" );
      result.add( "help ................... show information on how to use the server" );
      result.add( "ping ................... will reply with OK when running" );
      result.add( "bye .................... close connection" );
      result.add( "put map key value ...... put value into map named 'map' under the given key" );
      result.add( "get map key ............ get value from map named 'map' under the given key" );
      result.add( "has map key ............ check if map named map contains given key" );
      result.add( "remove map key ......... remove key from map" );
      result.add( "size map ............... size of map" );
      result.add( "clear map .............. remove all entries from map" );
      result.add( "" );
      return result;
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
