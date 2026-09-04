package com.razorrecover.support;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
