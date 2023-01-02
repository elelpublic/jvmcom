package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import org.slf4j.Logger;

public class LoggingUtils {


    public static Level getLevel( Logger logger ) {

        Level level = Level.OFF;

        if( logger.isErrorEnabled() ) {
            level = Level.ERROR;
        }
        if( logger.isWarnEnabled() ) {
            level = Level.WARN;
        }
        if( logger.isInfoEnabled() ) {
            level = Level.INFO;
        }
        if( logger.isDebugEnabled() ) {
            level = Level.DEBUG;
        }
        if( logger.isTraceEnabled() ) {
            level = Level.TRACE;
        }

        return level;

    }


}
