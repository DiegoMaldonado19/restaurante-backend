package com.cunoc.restaurant.common.exception;

import com.cunoc.restaurant.common.dto.ErrorResponse;
import com.cunoc.restaurant.common.dto.FieldErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    // 1. Cuerpo de la peticion invalido: @Valid sobre @RequestBody.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request)
    {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDTO(toSnakeCase(error.getField()),
                                                String.valueOf(error.getRejectedValue()),
                                                error.getDefaultMessage()))
                .toList();

        return validationResponse(fields, request);
    }

    // 2. Parametros del metodo invalidos: @Validated sobre @RequestParam / @PathVariable.
    //    Sin este manejador, un size=5000 sale como 500 en vez de como 400.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex,
                                                                   HttpServletRequest request)
    {
        var fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDTO(
                                toSnakeCase(result.getMethodParameter().getParameterName()),
                                String.valueOf(result.getArgument()),
                                error.getDefaultMessage())))
                .toList();

        return validationResponse(fields, request);
    }

    // 3. Reglas de negocio y recursos inexistentes: el ErrorCode decide el estado.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request)
    {
        // INFO y sin traza: la regla funciono, no fallo nada. Llenar el log de trazas de
        // INSUFFICIENT_STOCK hace que la traza que si importa no se encuentre.
        log.info("businessRule={} path={}", ex.getErrorCode(), request.getRequestURI());

        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ErrorResponse.business(ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
    }

    // 4. Cualquier otra cosa: 500 con trace_id y la traza completa en el log.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request)
    {
        var traceId = UUID.randomUUID().toString();

        // El ultimo argumento es la excepcion, no un parametro de formato: SLF4J imprime
        // la traza completa, con archivo y numero de linea por marco.
        log.error("traceId={} method={} path={} message={}",
                  traceId, request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.internal(traceId, request.getRequestURI()));
    }

    private ResponseEntity<ErrorResponse> validationResponse(List<FieldErrorDTO> fields,
                                                             HttpServletRequest request)
    {
        log.debug("validationError fields={} path={}", fields, request.getRequestURI());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.validation(fields, request.getRequestURI()));
    }

    // Bean Validation reporta el campo en lowerCamelCase; el frontend lo conoce en
    // snake_case porque asi viaja en el JSON.
    private static String toSnakeCase(String field)
    {
        return field == null ? null : field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
