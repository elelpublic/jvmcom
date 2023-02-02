package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.services.logging.Level;
import com.infodesire.jvmcom.util.JvmUtils;
import com.infodesire.jvmcom.util.SocketUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ResourceLeakDetector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoggingServerTest {

    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );
        System.setProperty( "org.slf4j.simpleLogger.log.io.netty", "warn" );
        ResourceLeakDetector.setLevel( ResourceLeakDetector.Level.PARANOID );
    }
    private static final Logger logger = LoggerFactory.getLogger( "LoggingServerTest" );

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void simpleLogMessage() throws Exception {

        logger.info( "Process id: " + JvmUtils.getProcessId() );

        String host = "localhost";
        int port = SocketUtils.getFreePort();

        LoggingServer server = null;
        LoggingClient client = null;

        try {

            LoggingServerConfig serverConfig = new LoggingServerConfig();
            serverConfig.port = port;
            LoggingClientConfig clientConfig = new LoggingClientConfig();
            clientConfig.clientName = "node1";
            clientConfig.host = host;
            clientConfig.port = port;

            List<LoggingRequest> messages = new ArrayList<>();
            serverConfig.loggingRequestHandler = new MessageCollector( messages );

            server = new LoggingServer( serverConfig );
            Thread.yield();

            client = new LoggingClient( clientConfig );
            Thread.yield();

            assertEquals( 0, messages.size() );

            client.log( "main", Level.DEBUG, "Hello world" );

            Thread.sleep( 100 );

            assertEquals( 1, messages.size() );
            LoggingRequest message = messages.get( 0 );
            assertEquals( "node1", message.clientName );
            assertEquals( "main", message.category );
            assertEquals( Level.DEBUG, message.level );
            assertEquals( "Hello world", message.message );

        }
        finally {
            if( client != null ) {
                client.close();
            }
            if( server != null ) {
                server.close();
            }
        }

    }


    static class MessageCollector extends SimpleChannelInboundHandler<LoggingRequest> {

        private final List<LoggingRequest> messages;

        public MessageCollector( List<LoggingRequest> messages ) {
            this.messages = messages;
        }

        @Override
        protected void channelRead0( ChannelHandlerContext channelHandlerContext, LoggingRequest loggingRequest ) throws Exception {
            messages.add( loggingRequest );
        }
    }

}

