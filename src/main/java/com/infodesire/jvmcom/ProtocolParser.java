package com.infodesire.jvmcom;

/**
 * Parse command send between servers and clients
 */
public class ProtocolParser {

  public static Token parseFirstWord( String line ) {
    StringBuffer word = new StringBuffer( 10 );
    String restOfLine = null;
    boolean inWord = false;
    for( int i = 0; i < line.length(); i++ ) {
      char c = line.charAt( i );
      if( !Character.isWhitespace( c ) ) {
        word.append( c );
        inWord = true;
      }
      else {
        if( inWord ) {
          restOfLine = line.substring( i + 1 );
          break;
        }
      }
    }
    return new Token( word.toString(), restOfLine );
  }

  static class Token {

    final String word;
    final String restOfLine;

    public Token( String word, String restOfLine ) {
      this.word = word;
      this.restOfLine = restOfLine;
    }

  }


}
