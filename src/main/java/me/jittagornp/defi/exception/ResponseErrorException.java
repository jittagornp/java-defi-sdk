/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi.exception;

import org.web3j.protocol.core.Response;

/**
 * @author jittagornp
 */
public class ResponseErrorException extends RuntimeException {

    private Response.Error error;

    public ResponseErrorException(final Response.Error error) {
        super(error.getMessage());
        this.error = error;
    }
}
