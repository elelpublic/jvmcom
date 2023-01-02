package com.infodesire.jvmcom.services;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.services.logging.LoggingHandler;
import com.infodesire.jvmcom.services.logging.LoggingHandlerImpl;
import com.infodesire.jvmcom.services.logging.LoggingService;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class DefaultServiceFactory implements ServiceFactory {

    @Override
    public Service create( int port, String name ) {
        if( name.equals( "logging" ) ) {
            return new LoggingService( port, new Supplier<LoggingHandler>() {
                @Override
                public LoggingHandler get() {
                    return new LoggingHandlerImpl();
                }
            } );
        }
        return null;
    }

}
