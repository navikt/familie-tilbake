package no.nav.familie.tilbake.api.baks

import jakarta.transaction.Transactional
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.bigQuery.BigQueryAdapterService
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.historikkinnslag.Aktør.Vedtaksløsning
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.norg2.Norg2Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/baks/portefoljejustering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BAKSPorteføljejusteringController(
    private val behandlingRepository: BehandlingRepository,
    private val integrasjonerClient: IntegrasjonerClient,
    private val historikkService: HistorikkService,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val featureService: FeatureService,
    private val norg2Service: Norg2Service,
    private val bigQueryAdapterService: BigQueryAdapterService,
) {
    @Transactional
    @PutMapping("/oppdater-behandlende-enhet")
    fun oppdaterBehandlendeEnhetPåBehandling(
        @RequestBody oppdaterBehandlendeEnhetRequest: OppdaterBehandlendeEnhetRequest,
    ): Ressurs<String> {
        val (behandlingEksternBrukId, nyEnhetId) = oppdaterBehandlendeEnhetRequest

        val behandling = behandlingRepository.findByEksternBrukId(behandlingEksternBrukId)
            ?: throw IllegalStateException("Fant ikke behandling for eksternBrukId=$behandlingEksternBrukId")

        val gammelEnhetNavn = behandling.behandlendeEnhetsNavn

        if (behandling.behandlendeEnhet == nyEnhetId) {
            return Ressurs.success("Behandlende enhet er allerede satt til $nyEnhetId for behandling med eksternBrukId=$behandlingEksternBrukId")
        }

        val nyEnhet = if (featureService.modellFeatures[Toggle.Norg2]) {
            norg2Service.hentNavKontor(nyEnhetId)
        } else {
            integrasjonerClient.hentNavkontor(nyEnhetId)
        }
        val oppdatertBehandling = behandlingRepository.update(
            behandling.copy(
                ansvarligSaksbehandler = Vedtaksløsning.ident,
                ansvarligBeslutter = null,
                behandlendeEnhet = nyEnhetId,
                behandlendeEnhetsNavn = nyEnhet.navn,
            ),
        )
        bigQueryAdapterService.oppdaterBigQuery(oppdatertBehandling)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandling.id,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
            aktør = Vedtaksløsning,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = "Behandlende enhet endret fra $gammelEnhetNavn til ${nyEnhet.navn} i forbindelse med porteføljejustering januar 2026.",
        )

        behandlingTilstandService.opprettSendingAvBehandlingenManuelt(behandlingId = behandling.id)

        return Ressurs.success("Behandlende enhet oppdatert til $nyEnhetId for behandling med eksternBrukId=$behandling")
    }

    data class OppdaterBehandlendeEnhetRequest(
        val behandlingEksternBrukId: UUID,
        val nyEnhet: String,
    )
}
