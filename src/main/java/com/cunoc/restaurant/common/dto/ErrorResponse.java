package com.cunoc.restaurant.common.dto;

import com.cunoc.restaurant.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de error unico de todo el sistema. Con
 * spring.jackson.default-property-inclusion=non_null los campos que no aplican no
 * salen en el JSON: un 409 no lleva fields, un 400 no lleva trace_id.
 */
public record ErrorResponse(
        String              errorCode,
        String              message,
        String              suggestedAction,
        String              traceId,
        LocalDateTime       timestamp,
        String              path,
        List<FieldErrorDTO> fields)
{
    public static ErrorResponse business(ErrorCode code, String message, String path)
    {
        return new ErrorResponse(code.name(), message, code.getSuggestedAction(),
                                 null, LocalDateTime.now(), path, null);
    }

    public static ErrorResponse validation(List<FieldErrorDTO> fields, String path)
    {
        return new ErrorResponse(ErrorCode.VALIDATION_ERROR.name(), "La solicitud tiene campos invalidos.",
                                 null, null, LocalDateTime.now(), path, fields);
    }

    public static ErrorResponse internal(String traceId, String path)
    {
        return new ErrorResponse(ErrorCode.INTERNAL_ERROR.name(), "Ocurrio un error inesperado.",
                                 null, traceId, LocalDateTime.now(), path, null);
    }
}
