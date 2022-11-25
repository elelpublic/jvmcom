package com.infodesire.jvmcom;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.services.value.ValueServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;


/**
 * The command line version of server and client
 *
 */
public class Main {

  private static Options options;

  private static final int THREAD_COUNT = Integer.parseInt( System.getProperty( "com.infodesire.jvmcom.threadCount", "10" ) );

  public static void main( String... args ) throws IOException, ParseException, InterruptedException {

    print( "Demo 1.0" );
    print( "Running as " + SystemUtils.getUserName() + "@" + InetAddress.getLocalHost().getHostName() );
    print( "" );

    options = createOptions();

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    List<String> argslist = cmd.getArgList();

    if( argslist.isEmpty() ) {
      showUsage( "No command given." );
      Runtime.getRuntime().halt( 1 );
    }

    String command = argslist.get( 0 );

    String host = "localhost";
    int port = 44000;

    if( cmd.hasOption( "h" ) ) {
      host = cmd.getOptionValue( "h" );
    }

    if( cmd.hasOption( "p" ) ) {
      port = Integer.parseInt( cmd.getOptionValue( "p" ) );
    }

    if( command.equals( "help" ) ) {
      showUsage( "" );
    }
    else if( command.equals( "server" ) ) {
      ServerConfig config = new ServerConfig();
      config.port = port;
      config.threadCount = THREAD_COUNT;
      ValueServer server = new ValueServer( config );
      server.start();
      server.waitForShutDown();
    }
    else if( command.equals( "client" ) ) {
      LineBufferClient client = new LineBufferClient( host, port );
      client.connect( true );
    }

    Runtime.getRuntime().halt( 0 );

  }
  
  
  private static Options createOptions() {

    // create Options object
    Options options = new Options();

    // add l option for lowercase
    //options.addOption( "l", false, "print lower case" );

    options.addOption(
      Option.builder()
        .argName( "host" )
        .option( "h" )
        .hasArg()
        .desc( "host name, default is localhost" )
        .build()
    );

    options.addOption(
      Option.builder()
        .argName( "port" )
        .option( "p" )
        .hasArg()
        .desc( "port, default is 44000" )
        .build()
    );

    return options;

  }


  private static void showUsage( String message ) {

    HelpFormatter formatter = new HelpFormatter();
    print( message );
    formatter.printHelp("hello [options] command", options);
    print( "" );
    print( "commands:" );
    print( "help \t show help" );
    print( "server \t start a server" );
    print( "client \t start a server" );

  }

  private static void print( String line ) {
    System.out.println( line );
  }

}