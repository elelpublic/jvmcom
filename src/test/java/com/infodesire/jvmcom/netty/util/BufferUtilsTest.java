package com.infodesire.jvmcom.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BufferUtilsTest {

    @Test
    public void readLineUntil() {

        ByteBuf buf = Unpooled.buffer();
        ByteBuf copy;
        buf.writeCharSequence( "Hello World!", CharsetUtil.UTF_8 );

        assertEquals( 0, buf.readerIndex() );

        copy = buf.copy();
        assertEquals( "Hello ", BufferUtils.readLineUntil( copy, ' ', 100 ) );
        assertNotEquals( 0, copy.readerIndex() );

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( buf.copy(), ' ', 3 ) );
        assertEquals( 0, copy.readerIndex() );

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( buf.copy(), '\n', 100 ) );
        assertEquals( 0, copy.readerIndex() );

    }

    @Test
    public void readLineUntilMultibyteCharacters() {

        String original = new String( "A" + "\u00ea" + "\u00f1" + "\u00fc" + "C" );
        byte[] bytes = original.getBytes( StandardCharsets.UTF_8 );
        assertEquals( 5, original.length() );
        assertEquals( 8, bytes.length );

        ByteBuf buf = Unpooled.buffer();

        buf.writeCharSequence( original, CharsetUtil.UTF_8 );
        ByteBuf copy = buf.copy();

        assertEquals( 8, buf.readableBytes() );

        String result = buf.readCharSequence( 8, CharsetUtil.UTF_8 ).toString();
        assertEquals( original, result );

        assertEquals( 0, buf.readableBytes() );
        assertEquals( 8, buf.readerIndex() );

        assertEquals( original, BufferUtils.readLineUntil( copy, 'C', 100 ) );

    }

    @Test
    public void peekLine() {

        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence( "Hello\nWorld!", CharsetUtil.UTF_8 );

        assertEquals( "Hello", BufferUtils.peakLineUntil( buf, 100, true ) );
        assertEquals( "Hello\nWorld!", BufferUtils.peakLineUntil( buf, 100, false ) );

        assertEquals( "Hel", BufferUtils.peakLineUntil( buf, 3, true ) );
        assertEquals( "Hel", BufferUtils.peakLineUntil( buf, 3, false ) );

    }

}