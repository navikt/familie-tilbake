package no.nav.tilbakekreving.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@RestController
@RequestMapping("/api/dokumenter")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakDokumentController(
    private val vedtakDokumentService: VedtakDokumentService,
) {
    @GetMapping(
        path = ["/vedtak/{vedtakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Hent dokumentreferanser for et iverksatt vedtak")
    fun hentDokumentreferanser(
        @PathVariable vedtakId: String,
    ): Ressurs<List<VedtakDokumentreferanseDto>> =
        Ressurs.success(
            vedtakDokumentService.hentDokumentreferanser(
                vedtakId = vedtakId.tilBigInteger(),
            ),
        )
}

private fun String.tilBigInteger(): BigInteger {
    if (length > 64 || !matches(Regex("[0-9]+"))) {
        throw Feil(
            message = "VedtakId må være en tallstreng med maksimalt 64 tegn",
            logContext = SecureLog.Context.tom(),
            httpStatus = HttpStatus.BAD_REQUEST,
        )
    }
    return BigInteger(this).also {
        if (it > BigInteger.valueOf(Long.MAX_VALUE)) {
            throw Feil(
                message = "VedtakId er utenfor gyldig område",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
}
    }
