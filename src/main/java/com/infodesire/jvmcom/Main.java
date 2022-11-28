package com.infodesire.jvmcom;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.mesh.CliNode;
import com.infodesire.jvmcom.mesh.MeshConfig;
import com.infodesire.jvmcom.mesh.NodeAddress;
import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.services.value.ValueServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;


/**
 * The command line version of server and client
 *
 */
public class Main {

  private static Logger logger = LoggerFactory.getLogger( "jvmcom" );

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

    if( command == null ) {
      showUsage( "No command given" );
    }
    else if( command.equals( "help" ) ) {
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
    else if( command.equals( "node" ) ) {
      if( !cmd.hasOption( "c" ) ) {
        showUsage( "No node configuration file. Please use -c FILE" );
        Runtime.getRuntime().halt( 1 );
      }
      if( !cmd.hasOption( "n" ) ) {
        showUsage( "Node id missing. Please use -n ID" );
        Runtime.getRuntime().halt( 1 );
      }
      String configFile = cmd.getOptionValue( "c" );
      String nodeId = cmd.getOptionValue( "n" );
      MeshConfig config = MeshConfig.loadFromFile( new File( configFile ) );
      NodeAddress myAddress = config.getMembers().get( nodeId );
      if( myAddress == null ) {
        showUsage( "No entry found for node id " + nodeId + " in node configuration " + configFile );
        Runtime.getRuntime().halt( 1 );
      }
      SocketPool socketPool = new SocketPool();
      new CliNode( config, myAddress, socketPool ).waitForShutDown();
    }
    else {
      showUsage( "Unknown command: " + command );
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

    options.addOption(
      Option.builder()
        .argName( "config" )
        .option( "c" )
        .hasArg()
        .desc( "mesh configuration file" )
        .build()
    );

    options.addOption(
      Option.builder()
        .argName( "node" )
        .option( "n" )
        .hasArg()
        .desc( "node id" )
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
    print( "client \t start a client" );
    print( "node -i ID\t start mesh node with the given ID" );

  }

  private static void print( String line ) {
    System.out.println( line );
  }

}