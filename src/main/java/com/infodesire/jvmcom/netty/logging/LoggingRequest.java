package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.services.logging.Level;

/**
 * Meta data of a log request:
 * <p>
 *
 */
public class LoggingRequest {

    String clientName;

    String category;
    
    Level level;

    String message;

//    public LoggingRequest( String category, Level level, String message ) {
//        this.category = category;
//        this.level = level;
//        this.message = message;
//    }

    /**
     * @return Debug output (not meant for parsing)
     *
     */
    public String toString() {
        return ( clientName + " " + category + " " + level + " " + message ).replaceAll( "(\r\n)*", "" );
    }
    
}
