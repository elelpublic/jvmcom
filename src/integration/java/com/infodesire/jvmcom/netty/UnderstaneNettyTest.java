package com.infodesire.jvmcom.netty;

import com.infodesire.jvmcom.netty.util.BufferUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.util.ArrayList;
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

    @Test
    public void decodeMessagesFromChoppyByteStream() {

        List<Message> messages = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel(
                new MessageDecoder(),
                new MessageStore( messages )
        );

        assertEquals( 0, messages.size() );

        channel.writeInbound( Unpooled.copiedBuffer( "TE", CharsetUtil.UTF_8 ) );
        channel.writeInbound( Unpooled.copiedBuffer( "XT ", CharsetUtil.UTF_8 ) );
        channel.writeInbound( Unpooled.buffer().writeInt( 120 ) );

        assertEquals( 1, messages.size() );
        assertEquals( "TEXT", messages.get( 0 ).type );
        assertEquals( 120, messages.get( 0 ).length );

    }

    static class Message {
        String type;
        int length;
    }

    static class MessageDecoder extends ByteToMessageDecoder {
        protected void decode( ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list ) throws Exception {
            buf.markReaderIndex();
            while( buf.readableBytes() > 0 ) {
                String type = BufferUtils.readLineUntil( buf, ' ', 100 );
                if( type != null ) {
                    Message message = new Message();
                    message.type = type.trim();
                    try {
                        message.length = buf.readInt();
                        list.add( message );
                        buf.markReaderIndex();
                    }
                    catch( IndexOutOfBoundsException ex ) {
                        buf.resetReaderIndex(); // not enough data
                        break;
                    }
                }
                else {
                    buf.resetReaderIndex(); // not enough data
                    break;
                }
            }
        }
    }

    static class MessageStore extends SimpleChannelInboundHandler<Message> {
        private final List<Message> storage;
        public MessageStore( List<Message> storage ) {
            this.storage = storage;
        }
        protected void channelRead0( ChannelHandlerContext channelHandlerContext, Message message ) throws Exception {
            storage.add( message );
        }
    }


    static class ToUpperCaseDecoder extends MessageToMessageDecoder<String> {

        protected void decode( ChannelHandlerContext channelHandlerContext, String input, List<Object> list ) throws Exception {
            list.add( input.toUpperCase() );
        }
    }

    static class Reversereply extends SimpleChannelInboundHandler<String> {

        protected void channelRead0( ChannelHandlerContext ctx, String input ) throws Exception {
            StringBuffer rev = new StringBuffer();
            input.chars().forEach( c -> {
                rev.insert( 0, (char) c );
            } );
            ctx.write( rev.toString() );
        }

        public void channelReadComplete( ChannelHandlerContext ctx ) {
            ctx.flush();
        }

    }

}
