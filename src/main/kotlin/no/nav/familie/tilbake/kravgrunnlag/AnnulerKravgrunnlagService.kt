package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.log.SecureLog
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerRequest
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.kravgrunnlag.annuller.v1.AnnullerKravgrunnlagDto
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class AnnulerKravgrunnlagService(
    private val oppdragClient: OppdragClient,
    private val oppdragRestClient: OppdragRestClient,
    private val featureService: FeatureService,
) {
    fun annulerKravgrunnlagRequest(
        eksternKravgrunnlagId: BigInteger,
        vedtakId: BigInteger,
        logContext: SecureLog.Context,
    ) {
        if (featureService.modellFeatures[Toggle.OppdragRestClient]) {
            oppdragRestClient.annullerKravgrunnlag(vedtakId)
        } else {
            val annullerKravgrunnlagDto = AnnullerKravgrunnlagDto()
            annullerKravgrunnlagDto.kodeAksjon = KodeAksjon.ANNULERE_GRUNNLAG.kode // fast verdi
            annullerKravgrunnlagDto.vedtakId = vedtakId
            annullerKravgrunnlagDto.saksbehId = "K231B433" // fast verdi

            val annulerRequest = KravgrunnlagAnnulerRequest()
            annulerRequest.annullerkravgrunnlag = annullerKravgrunnlagDto

            oppdragClient.annulerKravgrunnlag(eksternKravgrunnlagId, annulerRequest, logContext)
        }
    }
}
