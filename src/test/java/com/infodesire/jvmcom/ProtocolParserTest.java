package com.infodesire.jvmcom;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProtocolParserTest {

  @Test
  public void parseFirstWord() {

    assertEquals( "put", ProtocolParser.parseFirstWord( "put" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( " put" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "put " ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "\tput" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "\tput\t" ).word );

    assertEquals( "put", ProtocolParser.parseFirstWord( "put map" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "put  map" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "put\tmap" ).word );
    assertEquals( "put", ProtocolParser.parseFirstWord( "put \tmap" ).word );

    assertEquals( "map", ProtocolParser.parseFirstWord( "put map" ).restOfLine );
    assertEquals( " map", ProtocolParser.parseFirstWord( "put  map" ).restOfLine );
    assertEquals( "map", ProtocolParser.parseFirstWord( "put\tmap" ).restOfLine );
    assertEquals( "\tmap", ProtocolParser.parseFirstWord( "put \tmap" ).restOfLine );

    assertEquals( "map and other stuff", ProtocolParser.parseFirstWord( "put map and other stuff" ).restOfLine );
    assertEquals( " map and other stuff ", ProtocolParser.parseFirstWord( "put  map and other stuff " ).restOfLine );

  }

}