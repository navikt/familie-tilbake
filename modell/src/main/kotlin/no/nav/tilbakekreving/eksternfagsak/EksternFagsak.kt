package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.util.UUID

class EksternFagsak(
    private val id: UUID,
    val eksternId: String,
    internal val ytelse: Ytelse,
    val behandlinger: EksternFagsakBehandlingHistorikk,
) {
    fun tilFrontendDto(): EksternFagsakDto {
        return EksternFagsakDto(
            eksternId = eksternId,
            ytelsestype = ytelse.tilYtelseDTO(),
            fagsystem = ytelse.tilFagsystemDTO(),
        )
    }

    fun lagre(fagsysteminfo: FagsysteminfoHendelse): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Revurdering(
                id = UUID.randomUUID(),
                eksternId = fagsysteminfo.revurdering.behandlingId,
                revurderingsårsak = fagsysteminfo.revurdering.årsak,
                årsakTilFeilutbetaling = fagsysteminfo.revurdering.årsakTilFeilutbetaling ?: "Ukjent",
                vedtaksdato = fagsysteminfo.revurdering.vedtaksdato,
                utvidedePerioder = fagsysteminfo.utvidPerioder?.map {
                    EksternFagsakRevurdering.UtvidetPeriode(
                        id = UUID.randomUUID(),
                        kravgrunnlagPeriode = it.kravgrunnlagPeriode,
                        vedtaksperiode = it.vedtaksperiode,
                    )
                } ?: emptyList(),
                url = fagsysteminfo.revurdering.url,
            ),
        )
    }

    fun lagreTomBehandling(
        revurderingsdatoFraKravgrunnlag: LocalDate?,
        kravgrunnlagReferanse: String,
    ): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Ukjent(
                id = UUID.randomUUID(),
                eksternId = kravgrunnlagReferanse,
                revurderingsdatoFraKravgrunnlag = revurderingsdatoFraKravgrunnlag,
            ),
        )
    }

    fun fagsysteminfoBehov(
        eksternBehandlingId: String,
        vedtakGjelderId: String,
    ): FagsysteminfoBehov {
        return FagsysteminfoBehov(
            eksternFagsakId = eksternId,
            eksternBehandlingId = eksternBehandlingId,
            vedtakGjelderId = vedtakGjelderId,
            ytelse = ytelse,
        )
    }

    fun tilEntity(tilbakekrevingId: String): EksternFagsakEntity {
        return EksternFagsakEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            eksternId = eksternId,
            ytelseEntity = ytelse.tilEntity(),
            behandlinger = behandlinger.tilEntity(id),
        )
    }

    fun forhåndsvarselHjemlerForTilbakekreving() = buildList {
        addAll(ytelse.hjemlerForTilbakekreving())
        addAll(HjemmelForTilbakekreving.standardForhåndsvarselHjemler(beregnerRenter = true))
    }

    fun hentYtelse() = ytelse

    fun saksbehandlingUrl(): String? {
        if (!behandlinger.harBehandling()) return null
        return behandlinger.nåværende().entry.url
    }
}
