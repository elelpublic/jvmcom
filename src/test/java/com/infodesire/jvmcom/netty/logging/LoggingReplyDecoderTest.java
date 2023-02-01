package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.services.logging.Level;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggingReplyDecoderTest {

    @Test
    public void testDecode() {

        // the good cases
        testDecode( true, new LoggingReply( "main", Level.DEBUG ), "OK main DEBUG\n" );
        testDecode( true, new LoggingReply( "com.infodesire.jvmcom", Level.DEBUG ), "OK com.infodesire.jvmcom DEBUG\n" );
        testDecode( true, new LoggingReply( "main", Level.ERROR ), "OK main ERROR\n" );
        testDecode( true, new LoggingReply( "main", Level.ERROR ), "OK  main ERROR\n" );
        testDecode( true, new LoggingReply( "main", Level.ERROR ), "OK  main  ERROR\n" );
        testDecode( true, new LoggingReply( "main", Level.ERROR ), "OK main ERROR \n" );
        testDecode( true, new LoggingReply( "main", Level.ERROR ), "OK main ERROR\r\n" );

        // the bad cases
        testDecode( false, new LoggingReply( "main", Level.DEBUG ), "ok main DEBUG\n" );
        testDecode( false, new LoggingReply( "main", Level.DEBUG ), "OK main FORCE\n" );
        testDecode( false, new LoggingReply( "main", Level.DEBUG ), "OK\n" );
        testDecode( false, new LoggingReply( "main", Level.DEBUG ), "OK main\n" );

    }

    private void testDecode( boolean equal, LoggingReply reply, String channelData ) {
        EmbeddedChannel channel = new EmbeddedChannel( new LoggingReplyDecoder() );
        channel.writeInbound( Unpooled.copiedBuffer( channelData, CharsetUtil.UTF_8 ) );
        channel.flush();
        if( equal ) {
            assertEquals( reply, channel.readInbound() );
        }
        else {
            LoggingReply replyFound = channel.readInbound();
            if( replyFound == null ) {
                assertNull( replyFound );
            }
            else {
                assertNotEquals( reply, replyFound );
            }
        }
    }


}