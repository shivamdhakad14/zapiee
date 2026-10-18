package com.zapiee.subscriptionservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SubscriptionException extends RuntimeException {
  private final String errorCode;
  private final HttpStatus httpStatus;

  public SubscriptionException(String message, String errorCode) {
    this(message, errorCode, HttpStatus.BAD_REQUEST);
  }

  public SubscriptionException(String message, String errorCode, HttpStatus httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }
}
