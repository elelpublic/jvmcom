package com.infodesire.jvmcom.servers.value;

import com.infodesire.jvmcom.line.LineBufferClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ValueClient extends LineBufferClient {

  private static Logger logger = LoggerFactory.getLogger( "Client" );

  public ValueClient( String host, int port ) {
    super( host, port );
  }

  public boolean has( String mapName, String valueName ) throws IOException {
    String originalReply = send( "has " + mapName + " " + valueName ).toString();
    if( originalReply.startsWith( "Error" ) ) {
      String reply = originalReply.substring( 5 ).trim();
      int sep = reply.indexOf( ' ' );
      if( sep == -1 ) {
        throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
      }
      else {
        String remainder = reply.substring( sep ).trim();
        reply = reply.substring( 0, sep );
        ValueError error = null;
        try {
          error = ValueError.valueOf( reply );
        }
        catch( Exception ex ) {
          throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
        }
        if( error == ValueError.NO_SUCH_MAP ) {
          logger.debug( originalReply );
          return false;
        }
        else if( error == ValueError.MAP_NAME_MISSING || error == ValueError.VALUE_NAME_MISSING || error == ValueError.VALUE_MUST_NO_BE_NULL ) {
          throw new ValueServerException( error, originalReply );
        }
      }
    }
    return Boolean.parseBoolean( originalReply );
  }

  public String get( String mapName, String valueName ) throws IOException {
    String originalReply = send( "get " + mapName + " " + valueName ).toString();
    if( originalReply.startsWith( "Error" ) ) {
      String reply = originalReply.substring( 5 ).trim();
      int sep = reply.indexOf( ' ' );
      if( sep == -1 ) {
        throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
      }
      else {
        reply = reply.substring( 0, sep );
        ValueError error = null;
        try {
          error = ValueError.valueOf( reply );
        }
        catch( Exception ex ) {
          throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
        }
        if( error == ValueError.NO_SUCH_MAP ) {
          logger.debug( originalReply );
          return null;
        }
        else if( error == ValueError.MAP_NAME_MISSING || error == ValueError.VALUE_NAME_MISSING || error == ValueError.VALUE_MUST_NO_BE_NULL ) {
          throw new ValueServerException( error, originalReply );
        }
      }
    }
    if( originalReply == null ) {
      return originalReply;
    }
    originalReply = originalReply.trim();
    return originalReply.length() == 0 ? null : originalReply;
  }

  public void put( String mapName, String valueName, String value ) throws IOException {
    String originalReply = send( "put " + mapName + " " + valueName + " " + value ).toString();
    if( originalReply.startsWith( "Error" ) ) {
      String reply = originalReply.substring( 5 ).trim();
      int sep = reply.indexOf( ' ' );
      if( sep == -1 ) {
        throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
      }
      else {
        reply = reply.substring( 0, sep );
        ValueError error = null;
        try {
          error = ValueError.valueOf( reply );
        }
        catch( Exception ex ) {
          throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
        }
        throw new ValueServerException( error, originalReply );
      }
    }
  }

  public int size( String mapName ) throws IOException {
    String originalReply = send( "size " + mapName ).toString();
    if( originalReply.startsWith( "Error" ) ) {
      String reply = originalReply.substring( 5 ).trim();
      int sep = reply.indexOf( ' ' );
      if( sep == -1 ) {
        throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
      }
      else {
        reply = reply.substring( 0, sep );
        ValueError error = null;
        try {
          error = ValueError.valueOf( reply );
        }
        catch( Exception ex ) {
          throw new ValueServerException( error, "Unknwon error in reply " + originalReply );
        }
        if( error == ValueError.NO_SUCH_MAP ) {
          logger.debug( originalReply );
          return 0;
        }
        else if( error == ValueError.MAP_NAME_MISSING || error == ValueError.VALUE_NAME_MISSING || error == ValueError.VALUE_MUST_NO_BE_NULL ) {
          throw new ValueServerException( error, originalReply );
        }
      }
    }
    return Integer.parseInt( originalReply );
  }

  public void clear( String mapName ) throws IOException {
    String originalReply = send( "clear " + mapName ).toString();
    if( originalReply.startsWith( "Error" ) ) {
      String reply = originalReply.substring( 5 ).trim();
      int sep = reply.indexOf( ' ' );
      if( sep == -1 ) {
        throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
      }
      else {
        reply = reply.substring( 0, sep );
        ValueError error = null;
        try {
          error = ValueError.valueOf( reply );
        }
        catch( Exception ex ) {
          throw new ValueServerException( ValueError.UNKNOWN_ERROR, "Unknwon error in reply " + originalReply );
        }
        if( error == ValueError.NO_SUCH_MAP ) {
          logger.debug( originalReply );
        }
        else {
          throw new ValueServerException( error, originalReply );
        }
      }
    }
  }

}
