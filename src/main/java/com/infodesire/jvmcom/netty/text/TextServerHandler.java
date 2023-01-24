package com.infodesire.jvmcom.netty.text;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.util.Date;

/**
 * Base for text based servers
 *
 */
@Sharable
public class TextServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception {
        ctx.write( "Welcome. Send 'bye' to quit.\r\n" );
        ctx.flush();
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, String request ) throws Exception {

        String response;
        boolean close = false;
        if( request.isEmpty() ) {
            response = "EMPTY_REQUEST\r\n";
        }
        else if( "bye".equals( request.toLowerCase() ) ) {
            response = "BYE\r\n";
            close = true;
        }
        else {
            response = "REPLY '" + request + "'?\r\n";
        }

        ChannelFuture future = ctx.write( response );

        if( close ) {
            // close after reply to bye was sent
            future.addListener( ChannelFutureListener.CLOSE );
        }

    }

    @Override
    public void channelReadComplete( ChannelHandlerContext ctx ) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) {
        cause.printStackTrace();
        ctx.close();
    }

}

