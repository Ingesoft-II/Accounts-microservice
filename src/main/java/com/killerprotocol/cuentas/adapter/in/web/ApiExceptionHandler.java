package com.killerprotocol.cuentas.adapter.in.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.killerprotocol.cuentas.domain.exception.CuentaBaneadaException;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.exception.CuentaSuspendidaException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException.Motivo;
import com.killerprotocol.cuentas.domain.exception.JugadorNoIdentificadoException;
import com.killerprotocol.cuentas.domain.exception.LimiteDesafiosExcedidoException;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.exception.RefrescoReutilizadoException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE_PROBLEMAS = "https://killerprotocol.dev/problems/";

    @ExceptionHandler(CuentaBaneadaException.class)
    public ProblemDetail cuentaBaneada(CuentaBaneadaException ex, HttpServletRequest request) {
        return problema(HttpStatus.FORBIDDEN, "cuenta-baneada", "Cuenta baneada",
                ex.getMessage(), request);
    }

    @ExceptionHandler(CuentaSuspendidaException.class)
    public ProblemDetail cuentaSuspendida(CuentaSuspendidaException ex, HttpServletRequest request) {
        ProblemDetail pd = problema(HttpStatus.FORBIDDEN, "cuenta-suspendida",
                "Cuenta suspendida", ex.getMessage(), request);
        pd.setProperty("until", ex.until().toString());
        return pd;
    }

    @ExceptionHandler(DesafioInutilizableException.class)
    public ProblemDetail desafioInutilizable(DesafioInutilizableException ex,
                                             HttpServletRequest request) {
        if (ex.motivo() == Motivo.AGOTADO) {
            return problema(HttpStatus.TOO_MANY_REQUESTS, "desafio-agotado",
                    "Demasiados intentos", ex.getMessage(), request);
        }
        return problema(HttpStatus.UNAUTHORIZED, "desafio-inutilizable",
                "Desafío no disponible", ex.getMessage(), request);
    }

    @ExceptionHandler(LimiteDesafiosExcedidoException.class)
    public ProblemDetail limiteDesafios(LimiteDesafiosExcedidoException ex,
                                        HttpServletRequest request) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "limite-desafios",
                "Límite de desafíos excedido", ex.getMessage(), request);
    }

    @ExceptionHandler(RefrescoInvalidoException.class)
    public ProblemDetail refrescoInvalido(RefrescoInvalidoException ex,
                                          HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, "refresco-invalido",
                "Refresh token inválido", ex.getMessage(), request);
    }

    @ExceptionHandler(RefrescoReutilizadoException.class)
    public ProblemDetail refrescoReutilizado(RefrescoReutilizadoException ex,
                                             HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, "refresco-reutilizado",
                "Refresh token reutilizado", ex.getMessage(), request);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ProblemDetail cuentaNoEncontrada(CuentaNoEncontradaException ex,
                                            HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, "cuenta-no-encontrada",
                "Cuenta no encontrada", ex.getMessage(), request);
    }

    @ExceptionHandler(JugadorNoIdentificadoException.class)
    public ProblemDetail jugadorNoIdentificado(JugadorNoIdentificadoException ex,
                                               HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, "jugador-no-identificado",
                "Jugador no identificado", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacion",
                "Datos de entrada inválidos", "Revisa los campos marcados", request);
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> e = new LinkedHashMap<>();
                    e.put("field", fe.getField());
                    e.put("message", fe.getDefaultMessage());
                    return e;
                })
                .toList();
        pd.setProperty("errors", errores);
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex,
                                           HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "validacion",
                "Datos de entrada inválidos", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail metodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                           HttpServletRequest request) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "metodo-no-soportado",
                "Método no soportado", ex.getMessage(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail rutaNoEncontrada(NoHandlerFoundException ex,
                                          HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado",
                "Ruta no encontrada", ex.getMessage(), request);
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String title,
                                          String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE_PROBLEMAS + codigo));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        String traceId = request.getHeader("X-Request-Id");
        if (traceId != null && !traceId.isBlank()) {
            pd.setProperty("traceId", traceId);
        }
        return pd;
    }
}
