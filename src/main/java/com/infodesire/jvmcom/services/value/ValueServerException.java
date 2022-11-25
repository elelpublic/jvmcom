package com.infodesire.jvmcom.services.value;

public class ValueServerException extends RuntimeException {

  private final ValueError error;

  public ValueServerException( ValueError error, String message ) {
    super( message );
    this.error = error;
  }

  public ValueError getError() {
    return error;
  }

  public String toString() {
    return error.toString() + (getMessage() == null ? "" : getMessage());
  }

}
