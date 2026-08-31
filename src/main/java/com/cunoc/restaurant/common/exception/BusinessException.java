package com.cunoc.restaurant.common.exception;

import lombok.Getter;

/** Regla de negocio incumplida. El estado HTTP y la accion sugerida los lleva el ErrorCode. */
@Getter
public class BusinessException extends RuntimeException
{
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode)
    {
        this(errorCode, errorCode.name());
    }
}
