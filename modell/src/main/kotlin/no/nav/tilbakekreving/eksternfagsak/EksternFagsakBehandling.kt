package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingType
import no.nav.tilbakekreving.entities.UtvidetPeriodeEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.util.UUID

sealed class EksternFagsakBehandling(
    override val internId: UUID,
) : Historikk.HistorikkInnslag<UUID> {
    internal abstract val eksternId: String
    abstract val revurderingsresultat: String
    abstract val revurderingsårsak: String
    abstract val begrunnelseForTilbakekreving: String
    abstract val revurderingsvedtaksdato: LocalDate

    abstract fun utvidPeriode(periode: Datoperiode): Datoperiode

    class Behandling(
        internId: UUID,
        override val eksternId: String,
        override val revurderingsresultat: String,
        override val revurderingsårsak: String,
        override val begrunnelseForTilbakekreving: String,
        override val revurderingsvedtaksdato: LocalDate,
        private val utvidetPerioder: List<UtvidetPeriode>,
    ) : EksternFagsakBehandling(internId) {
        override fun utvidPeriode(periode: Datoperiode): Datoperiode {
            return utvidetPerioder.singleOrNull { it.gjelderFor(periode) }?.utvid() ?: periode
        }

        override fun tilEntity(): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.BEHANDLING,
                internId = internId,
                eksternId = eksternId,
                revurderingsresultat = revurderingsresultat,
                revurderingsårsak = revurderingsårsak,
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                utvidetPerioder = utvidetPerioder.map { it.tilEntity() },
            )
        }
    }

    class UtvidetPeriode(
        private val kravgrunnlagPeriode: Datoperiode,
        private val vedtaksperiode: Datoperiode,
    ) {
        fun gjelderFor(periode: Datoperiode): Boolean = kravgrunnlagPeriode == periode

        fun utvid(): Datoperiode = vedtaksperiode

        fun tilEntity(): UtvidetPeriodeEntity {
            return UtvidetPeriodeEntity(
                kravgrunnlagPeriode = DatoperiodeEntity(kravgrunnlagPeriode.fom, kravgrunnlagPeriode.tom),
                vedtaksperiode = DatoperiodeEntity(vedtaksperiode.fom, vedtaksperiode.tom),
            )
        }
    }

    class Ukjent(
        internId: UUID,
        val revurderingsdatoFraKravgrunnlag: LocalDate?,
    ) : EksternFagsakBehandling(internId) {
        override val eksternId: String = "Ukjent"
        override val revurderingsresultat: String = "Ukjent"
        override val revurderingsårsak: String = "Ukjent - finn i fagsystem"
        override val begrunnelseForTilbakekreving: String = "Ukjent - finn i fagsystem"
        override val revurderingsvedtaksdato: LocalDate = revurderingsdatoFraKravgrunnlag ?: LocalDate.MIN

        override fun utvidPeriode(periode: Datoperiode): Datoperiode = periode

        override fun tilEntity(): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.UKJENT,
                internId = internId,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        }
    }

    abstract fun tilEntity(): EksternFagsakBehandlingEntity
}
