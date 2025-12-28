package com.lofo.serenia.exception.handler;
import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.rest.dto.out.QuotaErrorDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
/**
 * Handler pour les exceptions de quota dépassé.
 * Retourne un status 429 avec les détails du quota.
 */
@Slf4j
@Provider
public class QuotaExceededHandler implements ExceptionMapper<QuotaExceededException> {
    @Override
    public Response toResponse(QuotaExceededException exception) {
        log.warn("Quota exceeded: {}", exception.getMessage());
        QuotaErrorDTO errorDTO = new QuotaErrorDTO(
                exception.getQuotaType().getCode(),
                exception.getLimit(),
                exception.getCurrent(),
                exception.getRequested(),
                exception.getMessage()
        );
        return Response.status(429)
                .entity(errorDTO)
                .build();
    }
}
