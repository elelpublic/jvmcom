package com.infodesire.jvmcom.util;

import java.lang.management.ManagementFactory;

public class JvmUtils {

    public static Integer getProcessId() {

        // https://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf( '@' );

        if( index < 1 ) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return null;
        }

        try {
            return Integer.parseInt( jvmName.substring( 0, index ) );
        }
        catch( NumberFormatException ex ) {
            ex.printStackTrace();
        }

        return null;

    }


}
