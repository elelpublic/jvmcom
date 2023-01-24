package com.infodesire.jvmcom.netty.file;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Receive files from file server
 *
 */
public class FileClientHandler extends ChannelInboundHandlerAdapter {

    private final File downloadDir;
    Logger logger = FileClient.logger;

    private File file;
    private FileOutputStream fileOutputStream;
    private int remainingBytes;

    static enum ReadState { INIT, AWAIT_REPLY, FILE_DATA};

    private ReadState readState = ReadState.INIT;
    
    public FileClientHandler( File downloadDir ) {
        this.downloadDir = downloadDir;
    }

    @Override
    public void channelRead( ChannelHandlerContext ctx, Object msg ) {

        logger.info( "Message from server. Current state is " + readState.name() );

        ByteBuf buf = (ByteBuf) msg;

        if( logger.isEnabledForLevel( Level.DEBUG ) ) {
            buf.markReaderIndex();
            try {
                int MAX_PREVIEW = 100;
                String message = "" + buf.readCharSequence( Math.max( MAX_PREVIEW, buf.readableBytes() ), CharsetUtil.UTF_8 );
                int eol = message.indexOf( "\n" );
                if( eol != -1 ) {
                    message = message.substring( 0, eol );
                }
                logger.debug( "Message received: " + message );
            }
            finally {
                buf.resetReaderIndex();
            }
        }

        boolean close = false;

        try {

            if( readState == ReadState.INIT ) {

                // discard the welcome message
                CharSequence welcome = buf.readCharSequence( buf.readableBytes(), CharsetUtil.UTF_8 );
                logger.info( "Welcome message from server: " + welcome );

                readState = ReadState.AWAIT_REPLY;

            }
            else if( readState == ReadState.AWAIT_REPLY ) {

                if( buf.readableBytes() >= 3 ) {
                    String replyCode = buf.readCharSequence( 3, CharsetUtil.UTF_8 ).toString().toLowerCase();
                    logger.info( "Reply Code: '" + replyCode + "'" );
                    if( "ok:".equals( replyCode ) ) {

                        // the reply for a file request is "ok: FILESIZE FILENAME\n"

                        // read file size

                        Integer fileSize = null;
                        StringBuffer fileSizeString = new StringBuffer();
                        boolean spacesSkipped = false;
                        while( fileSize == null ) {
                            if( buf.readableBytes() > 0 ) {
                                String c = buf.readCharSequence( 1, CharsetUtil.UTF_8 ).toString();
                                if( c.equals( " " ) ) {
                                    if( spacesSkipped ) {
                                        fileSize = Integer.parseInt( fileSizeString.toString().trim() );
                                    }
                                }
                                else {
                                    fileSizeString.append( c );
                                    spacesSkipped = true;
                                }
                            }
                            else {
                                logger.error( "No file size was sent" );
                                return;
                            }
                        }

                        logger.debug( "File size found: " + fileSize );

                        remainingBytes = fileSize;

                        // now read file name
                        file = null;
                        StringBuffer fileName = new StringBuffer();
                        while( file == null ) {
                            if( buf.readableBytes() > 0 ) {
                                String c = buf.readCharSequence( 1, CharsetUtil.UTF_8 ).toString();
                                if( !c.equals( "\n" ) ) {
                                    fileName.append( c );
                                }
                                else {
                                    if( fileName.length() > 0 ) {
                                        file = new File( downloadDir, fileName.toString() );
                                    }
                                }
                            }
                            else {
                                logger.error( "No file name was sent" );
                                return;
                            }
                        }

                        logger.debug( "File name found: " + fileName );

                        logger.debug( "Remaining bytes in buf: " + buf.readableBytes() );

                        readState = ReadState.FILE_DATA;

                    }
                    else if( "err".equals( replyCode ) ) {
                        replyCode += buf.readCharSequence( buf.readableBytes(), CharsetUtil.UTF_8 );
                        logger.error( "Server sent an error message: " + replyCode );
                    }
                    else {
                        replyCode += buf.readCharSequence( buf.readableBytes(), CharsetUtil.UTF_8 );
                        logger.error( "Unknown reply code: " + replyCode );
                    }
                }

            }
            else if( readState == ReadState.FILE_DATA ) {

                logger.debug( "Reading file data" );

                try {

                    if( fileOutputStream == null ) {
                        logger.info( "Opening file " + file );
                        fileOutputStream = new FileOutputStream( file );
                    }

                    int numBytes = buf.readableBytes();
                    if( numBytes > remainingBytes ) {
                        numBytes = remainingBytes;
                    }

                    buf.getBytes( 0, fileOutputStream, numBytes );
                    remainingBytes -= numBytes;

                    if( remainingBytes == 0 ) {
                        fileOutputStream.close();
                        fileOutputStream = null;
                        logger.info( "Closing file after writing " + file );
                        readState = ReadState.AWAIT_REPLY;
                    }

                }
                catch( IOException ex ) {
                    System.out.println( ex );
                    close = true;
                }

            }

            if( close ) {
                logger.info( "Closing connection" );
                ctx.close();
            }

        }
        finally {
            buf.release();
        }

    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) {
        logger.error( "Error in client", cause );
        ctx.close();
    }

}
