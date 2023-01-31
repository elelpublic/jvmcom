package com.infodesire.jvmcom.services.logging;

/**
 * Log leven of the logging service
 */
public enum Level {

  ERROR( 200, org.slf4j.event.Level.ERROR ), //	200	An error occurred in the application.
  WARN( 300, org.slf4j.event.Level.WARN ), //	300	Something unexpected—though not necessarily an error—happened and needs to be watched.
  INFO( 400, org.slf4j.event.Level.INFO ), //	400	A normal, expected, relevant event happened.
  DEBUG( 500, org.slf4j.event.Level.DEBUG ), //	500	Used for debugging purposes
  TRACE( 600, org.slf4j.event.Level.TRACE )	// 600	Used for debugging purposes—includes the most detailed information

  ;

  private final int numeric;
  private org.slf4j.event.Level slf4jLevel;

  Level( int numeric, org.slf4j.event.Level slf4jLevel ) {
    this.numeric = numeric;
    this.slf4jLevel = slf4jLevel;
  }

  /**
   * Test if this severity is at least as severe as the given limit
   *
   * @param limit Limit below which this level is ignored
   * @return This level as at least the given severity
   *
   */
  public boolean isAtLeast( Level limit ) {
    return numeric <= limit.numeric;
  }

  public org.slf4j.event.Level asSlf4jLevel() {
    return slf4jLevel;
  }

}
