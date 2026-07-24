package no.nav.familie.tilbake.bigQuery

import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryTilleggsfristDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import org.springframework.stereotype.Service

@Service
class BigQueryAdapterService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val fagsakService: FagsakService,
    private val bigQueryService: BigQueryService,
) {
    fun oppdaterBigQuery(behandling: Behandling) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id).lastOrNull()
        val ytelsestype = fagsakService.finnFagsystemForBehandlingId(behandling.id).navn
        bigQueryService.oppdaterBehandling(
            BigQueryBehandlingDataDto(
                behandlingId = behandling.id.toString(),
                opprettetDato = behandling.opprettetTidspunkt,
                periode = kravgrunnlag?.samletPeriode(),
                behandlingstype = behandling.type.name,
                ytelse = ytelsestype,
                beløp = kravgrunnlag?.sumFeilutbetaling()?.toLong(),
                enhetNavn = behandling.behandlendeEnhetsNavn,
                enhetKode = behandling.behandlendeEnhet,
                status = behandling.status.name,
                resultat = behandling.sisteResultat?.type?.name,
            ),
        )
    }

    fun loggTilleggsfrist(
        behandlingId: String,
        dto: BehandlingsstegForeldelseDto,
    ) {
        val tilleggsfristPerioder = dto.foreldetPerioder
            .filter { it.foreldelsesvurderingstype == Foreldelsesvurderingstype.TILLEGGSFRIST }
        if (tilleggsfristPerioder.isEmpty()) return

        bigQueryService.loggTilleggsfrist(
            BigQueryTilleggsfristDto(
                behandlingId = behandlingId,
                antallPerioderMedTilleggsfrist = tilleggsfristPerioder.size,
                perioder = tilleggsfristPerioder.map { it.periode },
            ),
        )
    }
}
