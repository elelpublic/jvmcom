package com.infodesire.jvmcom.util;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.lang.reflect.Field;

public class LogUtils {

    /**
     * Set log level for slf4j's simple logger implementation.
     * <p>
     * This uses private api and may break at any moment.
     *
     * @param logger    Logger to set level of
     * @param levelName Name of log level
     */
    public static void setSimpleLoggerLevel( Logger logger, String levelName ) {

        try {

            // this property will only be used when the logger ist initializes
            System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", levelName );

            // set the level for an already initialized simple logging system
            Level level = getSimpleLevelForName( levelName );

            // set level in protected field using reflection
            Field field = logger.getClass().getDeclaredField( "currentLogLevel" );
            field.setAccessible( true );
            field.set( logger, level.toInt() );

        }
        catch( Throwable ex ) {
            System.err.println( "Cannot set log level in simple logger: " );
            ex.printStackTrace( System.err );
        }

    }

    /**
     * @param levelName Name of level
     * @return Level for simple logger implementation
     */
    private static Level getSimpleLevelForName( String levelName ) {

        levelName = levelName.trim().toLowerCase();

        if( levelName.equals( "error" ) ) {
            return Level.ERROR;
        }
        else if( levelName.equals( "warn" ) ) {
            return Level.WARN;
        }
        else if( levelName.equals( "info" ) ) {
            return Level.INFO;
        }
        else if( levelName.equals( "debug" ) ) {
            return Level.DEBUG;
        }
        else if( levelName.equals( "trace" ) ) {
            return Level.TRACE;
        }

        return Level.WARN; // a default

    }


}
