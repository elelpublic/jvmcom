package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.services.logging.Level;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggingRequestDecoderTest {

    @Test
    public void normalDecoding() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBUG 5\nHello", CharsetUtil.UTF_8 );

        channel.writeInbound( buf );

        LoggingRequest loggingRequest = channel.readInbound();

        assertEquals( "main", loggingRequest.category );
        assertEquals( Level.DEBUG, loggingRequest.level );
        assertEquals( "Hello", loggingRequest.message );

    }

    @Test
    public void multipeMessages() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBUG 5\nHellosub INFO 3\nYo!", CharsetUtil.UTF_8 );

        channel.writeInbound( buf );

        LoggingRequest loggingRequest = channel.readInbound();

        assertEquals( "main", loggingRequest.category );
        assertEquals( Level.DEBUG, loggingRequest.level );
        assertEquals( "Hello", loggingRequest.message );

        loggingRequest = channel.readInbound();

        assertEquals( "sub", loggingRequest.category );
        assertEquals( Level.INFO, loggingRequest.level );
        assertEquals( "Yo!", loggingRequest.message );

    }


    @Test
    public void multByteCharactersInMessage() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBUG 6\nH\u00eallosub INFO 4\nY\u014C!", CharsetUtil.UTF_8 );

        channel.writeInbound( buf );

        LoggingRequest loggingRequest = channel.readInbound();

        assertEquals( "main", loggingRequest.category );
        assertEquals( Level.DEBUG, loggingRequest.level );
        assertEquals( "H\u00eallo", loggingRequest.message );

        loggingRequest = channel.readInbound();

        assertEquals( "sub", loggingRequest.category );
        assertEquals( Level.INFO, loggingRequest.level );
        assertEquals( "Y\u014C!", loggingRequest.message );

    }

    @Test( timeout = 1000 )
    public void noInfiniteLoopWhenSizeParameterTooSmall() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBUG 4\nHello", CharsetUtil.UTF_8 );

        channel.writeInbound( buf );

        LoggingRequest loggingRequest = channel.readInbound();

        assertEquals( "main", loggingRequest.category );
        assertEquals( Level.DEBUG, loggingRequest.level );
        assertEquals( "Hell", loggingRequest.message );

    }

    @Test( timeout = 1000 )
    public void errorHandling() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBG 5\nHello", CharsetUtil.UTF_8 );
        channel.writeInbound( buf );

        assertNull( channel.readInbound() );

        channel = new EmbeddedChannel( new LoggingRequestDecoder() );
        buf = Unpooled.buffer();
        buf.writeCharSequence( "main 5\nHello", CharsetUtil.UTF_8 );
        channel.writeInbound( buf );
        assertNull( channel.readInbound() );

        channel = new EmbeddedChannel( new LoggingRequestDecoder() );
        buf = Unpooled.buffer();
        buf.writeCharSequence( "main DEBUG 5\n\n", CharsetUtil.UTF_8 );
        channel.writeInbound( buf );
        assertNull( channel.readInbound() );

    }

    @Test
    public void workOnChoppyInput() {

        EmbeddedChannel channel = new EmbeddedChannel( new LoggingRequestDecoder() );

        // cut in meta data

        ByteBuf buf1 = Unpooled.buffer();
        buf1.writeCharSequence( "main DEB", CharsetUtil.UTF_8 );
        channel.writeInbound( buf1 );

        Thread.yield();

        ByteBuf buf2 = Unpooled.buffer();
        buf2.writeCharSequence( "UG 5\nHello", CharsetUtil.UTF_8 );
        channel.writeInbound( buf2 );

        LoggingRequest loggingRequest = channel.readInbound();

        assertEquals( "main", loggingRequest.category );
        assertEquals( Level.DEBUG, loggingRequest.level );
        assertEquals( "Hello", loggingRequest.message );


        // cut in message

        buf1 = Unpooled.buffer();
        buf1.writeCharSequence( "sub WARN 6\nWa", CharsetUtil.UTF_8 );
        channel.writeInbound( buf1 );

        Thread.yield();

        buf2 = Unpooled.buffer();
        buf2.writeCharSequence( "ssup", CharsetUtil.UTF_8 );
        channel.writeInbound( buf2 );

        loggingRequest = channel.readInbound();

        assertEquals( "sub", loggingRequest.category );
        assertEquals( Level.WARN, loggingRequest.level );
        assertEquals( "Wassup", loggingRequest.message );

    }

}