package com.cunoc.restaurant.common.exception;

/** Recurso inexistente. Su ErrorCode ya declara HttpStatus.NOT_FOUND. */
public class NotFoundException extends BusinessException
{
    public NotFoundException(ErrorCode errorCode, String message)
    {
        super(errorCode, message);
    }

    public NotFoundException(ErrorCode errorCode)
    {
        super(errorCode);
    }
}
