package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.util.UUID

class EksternFagsak(
    val eksternId: String,
    internal val ytelse: Ytelse,
    val behandlinger: EksternFagsakBehandlingHistorikk,
    private val behovObservatør: BehovObservatør,
) : FrontendDto<EksternFagsakDto> {
    override fun tilFrontendDto(): EksternFagsakDto {
        return EksternFagsakDto(
            eksternId = eksternId,
            ytelsestype = ytelse.tilYtelseDTO(),
            fagsystem = ytelse.tilFagsystemDTO(),
        )
    }

    fun lagre(fagsysteminfo: FagsysteminfoHendelse): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        return behandlinger.lagre(
            EksternFagsakBehandling.Behandling(
                internId = UUID.randomUUID(),
                eksternId = fagsysteminfo.behandlingId,
                revurderingsårsak = fagsysteminfo.revurderingsårsak,
                revurderingsresultat = fagsysteminfo.revurderingsresultat,
                begrunnelseForTilbakekreving = fagsysteminfo.begrunnelseForTilbakekreving,
                revurderingsvedtaksdato = fagsysteminfo.revurderingsvedtaksdato,
                utvidetPerioder = fagsysteminfo.utvidPerioder?.map {
                    EksternFagsakBehandling.UtvidetPeriode(
                        kravgrunnlagPeriode = it.kravgrunnlagPeriode,
                        vedtaksperiode = it.vedtakPeriode,
                    )
                } ?: emptyList(),
            ),
        )
    }

    fun lagreTomBehandling(revurderingsdatoFraKravgrunnlag: LocalDate?): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        return behandlinger.lagre(
            EksternFagsakBehandling.Ukjent(
                internId = UUID.randomUUID(),
                revurderingsdatoFraKravgrunnlag = revurderingsdatoFraKravgrunnlag,
            ),
        )
    }

    fun trengerFagsysteminfo() {
        behovObservatør.håndter(
            FagsysteminfoBehov(
                eksternFagsakId = eksternId,
                ytelse = ytelse,
            ),
        )
    }

    fun tilEntity(): EksternFagsakEntity {
        return EksternFagsakEntity(
            eksternId = eksternId,
            ytelseEntity = ytelse.tilEntity(),
            behandlinger = behandlinger.tilEntity(),
        )
    }
}
