package com.zapiee.menu_service.exception;

import com.zapiee.menu_service.enums.ImageSource;

import java.util.List;

public class ImageSearchUnavailableException extends RuntimeException {
  private final List<ImageSource> failedSources;

  public ImageSearchUnavailableException(List<ImageSource> failedSources) {
    super("All image providers failed: " + failedSources);
    this.failedSources = failedSources;
  }

  public List<ImageSource> getFailedSources() {
    return failedSources;
  }
}