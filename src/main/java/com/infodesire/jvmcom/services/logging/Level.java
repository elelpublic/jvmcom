package com.infodesire.jvmcom.services.logging;

/**
 * Log leven of the logging service
 */
public enum Level {

  OFF, //	0	No logging
  FATAL, //	100	The application is unusable. Action needs to be taken immediately.
  ERROR, //	200	An error occurred in the application.
  WARN, //	300	Something unexpected—though not necessarily an error—happened and needs to be watched.
  INFO, //	400	A normal, expected, relevant event happened.
  DEBUG, //	500	Used for debugging purposes
  TRACE	// 600	Used for debugging purposes—includes the most detailed information

}
