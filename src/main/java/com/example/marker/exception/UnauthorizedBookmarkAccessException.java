package com.example.marker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // 403 Forbidden
public class UnauthorizedBookmarkAccessException extends RuntimeException {
    public UnauthorizedBookmarkAccessException(Long bookmarkId, Long userId) {
        super("User with ID " + userId + " is not authorized to access bookmark with ID " + bookmarkId);
    }
}