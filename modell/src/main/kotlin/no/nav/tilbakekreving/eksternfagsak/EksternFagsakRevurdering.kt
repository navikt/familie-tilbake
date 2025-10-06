package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingType
import no.nav.tilbakekreving.entities.RevurderingsårsakType
import no.nav.tilbakekreving.entities.UtvidetPeriodeEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.util.UUID

sealed class EksternFagsakRevurdering(
    override val id: UUID,
) : Historikk.HistorikkInnslag<UUID> {
    internal abstract val eksternId: String
    abstract val revurderingsårsak: Revurderingsårsak
    abstract val årsakTilFeilutbetaling: String
    abstract val vedtaksdato: LocalDate

    abstract fun utvidPeriode(periode: Datoperiode): Datoperiode

    class Revurdering(
        internId: UUID,
        override val eksternId: String,
        override val revurderingsårsak: Revurderingsårsak,
        override val årsakTilFeilutbetaling: String,
        override val vedtaksdato: LocalDate,
        private val utvidedePerioder: List<UtvidetPeriode>,
    ) : EksternFagsakRevurdering(internId) {
        override fun utvidPeriode(periode: Datoperiode): Datoperiode {
            return utvidedePerioder.singleOrNull { it.gjelderFor(periode) }?.utvid() ?: periode
        }

        override fun tilEntity(): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.BEHANDLING,
                internId = id,
                eksternId = eksternId,
                revurderingsårsak = revurderingsårsak.tilEntity(),
                årsakTilFeilutbetaling = årsakTilFeilutbetaling,
                utvidedePerioder = utvidedePerioder.map { it.tilEntity() },
                vedtaksdato = vedtaksdato,
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
    ) : EksternFagsakRevurdering(internId) {
        override val eksternId: String = "Ukjent"
        override val revurderingsårsak: Revurderingsårsak = Revurderingsårsak.UKJENT
        override val årsakTilFeilutbetaling: String = "Ukjent - finn i fagsystem"
        override val vedtaksdato: LocalDate = revurderingsdatoFraKravgrunnlag ?: LocalDate.MIN

        override fun utvidPeriode(periode: Datoperiode): Datoperiode = periode

        override fun tilEntity(): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.UKJENT,
                internId = id,
                eksternId = null,
                revurderingsårsak = null,
                årsakTilFeilutbetaling = null,
                vedtaksdato = null,
                utvidedePerioder = null,
            )
        }
    }

    abstract fun tilEntity(): EksternFagsakBehandlingEntity

    enum class Revurderingsårsak(private val entity: RevurderingsårsakType, val beskrivelse: String) {
        NYE_OPPLYSNINGER(RevurderingsårsakType.NYE_OPPLYSNINGER, "Nye opplysninger"),
        KORRIGERING(RevurderingsårsakType.KORRIGERING, "Korrigering"),
        KLAGE(RevurderingsårsakType.KLAGE, "Klage"),
        UKJENT(RevurderingsårsakType.UKJENT, "Ukjent"),
        ;

        fun tilEntity(): RevurderingsårsakType = entity
    }
}
