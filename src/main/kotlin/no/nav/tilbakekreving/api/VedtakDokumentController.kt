package no.nav.tilbakekreving.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakDokumentController(
    private val vedtakDokumentService: VedtakDokumentService,
) {
    @GetMapping(
        path = ["/dokumenter/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Hent dokumentreferanser for et iverksatt vedtak")
    fun hentDokumentreferanser(
        @ModelAttribute @Valid request: HentVedtakDokumenterRequest,
    ): Ressurs<List<VedtakDokumentreferanseDto>> =
        Ressurs.success(
            vedtakDokumentService.hentDokumentreferanser(
                vedtakId = request.vedtakId.tilBigInteger(),
            ),
        )
}

data class HentVedtakDokumenterRequest(
    @field:Size(max = 64)
    @field:Pattern(regexp = "\\d+")
    val vedtakId: String,
)

private fun String.tilBigInteger(): BigInteger =
    BigInteger(this).also {
        if (it > BigInteger.valueOf(Long.MAX_VALUE)) {
            throw Feil(
                message = "VedtakId er utenfor gyldig område",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
