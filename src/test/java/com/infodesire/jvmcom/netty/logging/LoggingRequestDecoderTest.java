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


}