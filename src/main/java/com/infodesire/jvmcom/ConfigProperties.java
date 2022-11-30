package com.infodesire.jvmcom;

public class ConfigProperties {


  public static final int THREAD_COUNT = Integer.parseInt( System.getProperty( "com.infodesire.jvmcom.threadCount", "10" ) );

  public static long LEAVE_TIMEOUT_MS = Long.parseLong( System.getProperty( "com.infodesire.jvmcom.mesh.leaveTimeoutMs", "1000" ) );


}
