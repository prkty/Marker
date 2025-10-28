package com.example.marker.exception;

public class BookmarkNotFoundException extends RuntimeException {

    public BookmarkNotFoundException(Long id) {
        super("Bookmark not found with id: " + id);
    }
}