package com.infodesire.jvmcom.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Explains how testing with netty's EmbeddedChannel works.
 */
public class UnderstaneNettyTest {

    @Test
    public void decoderReadsInboundAndWritesToInbound() {

        EmbeddedChannel channel = new EmbeddedChannel(
                new StringDecoder(),
                new ToUpperCaseDecoder()
        );
        assertTrue( channel.writeInbound( Unpooled.copiedBuffer( "Hello World!", CharsetUtil.UTF_8 ) ) );
        assertTrue( channel.finish() );
        assertEquals( "HELLO WORLD!", channel.readInbound() );

    }

    @Test
    public void ordinaryHandlerReadsInboundAndWritesOutbound() {

        EmbeddedChannel channel = new EmbeddedChannel(
                new StringDecoder(),
                new ToUpperCaseDecoder(),
                new Reversereply()
        );
        assertFalse( channel.writeInbound( Unpooled.copiedBuffer( "Hello World!", CharsetUtil.UTF_8 ) ) );
        assertTrue( channel.finish() );
        assertEquals( "!DLROW OLLEH", channel.readOutbound() );

    }

    @Test
    public void makeSureTextLinesArriveAsAWhole() throws InterruptedException {

        EmbeddedChannel channel = new EmbeddedChannel(
                new LineBasedFrameDecoder( 1024, true, false ),
                new StringDecoder()
        );
        assertFalse( channel.writeInbound( Unpooled.copiedBuffer( "Hello", CharsetUtil.UTF_8 ) ) );
        assertNull( channel.readInbound() );

        assertTrue( channel.writeInbound( Unpooled.copiedBuffer( " World!\r\n", CharsetUtil.UTF_8 ) ) );
        assertEquals( "Hello World!", channel.readInbound() );

        assertFalse( channel.finishAndReleaseAll() );

    }

    static class ToUpperCaseDecoder extends MessageToMessageDecoder<String> {

        protected void decode( ChannelHandlerContext channelHandlerContext, String input, List<Object> list ) throws Exception {
            list.add( input.toUpperCase() );
        }
    }

    static class Reversereply extends SimpleChannelInboundHandler<String> {

        protected void channelRead0( ChannelHandlerContext ctx, String input ) throws Exception {
            StringBuffer rev = new StringBuffer();
            input.chars().forEach( c -> { rev.insert( 0, (char) c ); } );
            ctx.write( rev.toString() );
        }

        public void channelReadComplete( ChannelHandlerContext ctx ) {
            ctx.flush();
        }

    }

}
