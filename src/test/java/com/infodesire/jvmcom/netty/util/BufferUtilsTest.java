package com.infodesire.jvmcom.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BufferUtilsTest {

    @Test
    public void singleLine() {

        ByteBuf buf = Unpooled.buffer();
        ByteBuf copy;
        buf.writeCharSequence( "Hello World!", CharsetUtil.UTF_8 );

        assertEquals( 0, buf.readerIndex() );
        assertEquals( 12, buf.readableBytes() );

        copy = buf.copy();
        assertEquals( "Hello ", BufferUtils.readLineUntil( copy, ' ', 100 ) );
        assertEquals( 6, copy.readerIndex() );
        assertEquals( 6, copy.readableBytes() );

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( buf.copy(), ' ', 3 ) );
        assertEquals( 0, copy.readerIndex() );
        assertEquals( 12, copy.readableBytes() );

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( buf.copy(), '\n', 100 ) );
        assertEquals( 0, copy.readerIndex() );
        assertEquals( 12, copy.readableBytes() );

    }

    @Test
    public void multiLine() {

        ByteBuf buf = Unpooled.buffer();
        ByteBuf copy;
        buf.writeCharSequence( "Hello World\nAnd Outer World!", CharsetUtil.UTF_8 );

        assertEquals( 0, buf.readerIndex() );
        assertEquals( 28, buf.readableBytes() );

        copy = buf.copy();
        assertEquals( "Hello World\n", BufferUtils.readLineUntil( copy, '\n', 100 ) );
        assertEquals( 12, copy.readerIndex() );
        assertEquals( 16, copy.readableBytes() );

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( buf.copy(), '!', 3 ) );
        assertEquals( 0, copy.readerIndex() );
        assertEquals( 28, copy.readableBytes() );

    }

    @Test
    public void readWithMultibyteCharacters() {

        // create a string with multibyte characters

        String original = new String( "A" + "\u00ea" + "\u00f1" + "\u00fc" + "C\nNewLine!" );
        byte[] bytes = original.getBytes( StandardCharsets.UTF_8 );
        assertEquals( 14, original.length() );
        assertEquals( 17, bytes.length );

        ByteBuf buf = Unpooled.buffer();

        buf.writeCharSequence( original, CharsetUtil.UTF_8 );
        ByteBuf copy = buf.copy();


        // assert some baseline

        assertEquals( 17, copy.readableBytes() );

        String result = copy.readCharSequence( 17, CharsetUtil.UTF_8 ).toString();
        assertEquals( original, result );

        assertEquals( 0, copy.readableBytes() );
        assertEquals( 17, copy.readerIndex() );


        // read the full string across line breaks

        copy = buf.copy();
        assertEquals( original, BufferUtils.readLineUntil( copy, '!', 100 ) );
        assertEquals( 17, copy.readerIndex() );
        assertEquals( 0, copy.readableBytes() );


        // read until first line break

        copy = buf.copy();
        assertEquals( original.substring( 0, 6 ), BufferUtils.readLineUntil( copy, '\n', 100 ) );
        assertEquals( 9, copy.readerIndex() );
        assertEquals( 8, copy.readableBytes() );


        // read and find no end character

        copy = buf.copy();
        assertNull( BufferUtils.readLineUntil( copy, 'x', 100 ) );
        assertEquals( 0, copy.readerIndex() );
        assertEquals( 17, copy.readableBytes() );

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