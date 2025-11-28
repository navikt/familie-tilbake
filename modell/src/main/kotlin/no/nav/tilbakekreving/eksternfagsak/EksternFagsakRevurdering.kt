package no.nav.tilbakekreving.eksternfagsak

import no.nav.kontrakter.frontend.models.RevurderingDto
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

    /**
     * @return null I noen tilfeller hvor vi ikke har integrasjon med fagsystem vil ekstern id være basert på referansen i kravgrunnlaget, vi kan ikke 100% sikre på at referansen er samme id som behandlingen.
     * Når vi ikke vet at det er samme id returnerer vi null.
     * I tilfeller hvor vi har integrasjon med fagsystemet vil denne returnere fagsystemets id på behandlingen
     */
    abstract fun behandlingId(): String?

    class Revurdering(
        id: UUID,
        override val eksternId: String,
        override val revurderingsårsak: Revurderingsårsak,
        override val årsakTilFeilutbetaling: String,
        override val vedtaksdato: LocalDate,
        private val utvidedePerioder: List<UtvidetPeriode>,
    ) : EksternFagsakRevurdering(id) {
        override fun utvidPeriode(periode: Datoperiode): Datoperiode {
            return utvidedePerioder.singleOrNull { it.gjelderFor(periode) }?.utvid() ?: periode
        }

        override fun behandlingId(): String? = eksternId

        override fun tilEntity(eksternFagsakRef: UUID): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                id = this.id,
                type = EksternFagsakBehandlingType.BEHANDLING,
                eksternFagsakRef = eksternFagsakRef,
                eksternId = eksternId,
                revurderingsårsak = revurderingsårsak.tilEntity(),
                årsakTilFeilutbetaling = årsakTilFeilutbetaling,
                utvidedePerioder = utvidedePerioder.map { it.tilEntity(id) },
                vedtaksdato = vedtaksdato,
            )
        }

        override fun tilFrontendDto(): RevurderingDto {
            return RevurderingDto(
                årsak = årsakTilFeilutbetaling,
                vedtaksdato = vedtaksdato,
                resultat = "Ukjent",
            )
        }
    }

    class UtvidetPeriode(
        private val id: UUID,
        private val kravgrunnlagPeriode: Datoperiode,
        private val vedtaksperiode: Datoperiode,
    ) {
        fun gjelderFor(periode: Datoperiode): Boolean = kravgrunnlagPeriode == periode

        fun utvid(): Datoperiode = vedtaksperiode

        fun tilEntity(eksternFagsakBehandlingRef: UUID): UtvidetPeriodeEntity {
            return UtvidetPeriodeEntity(
                id = id,
                eksternFagsakBehandlingRef = eksternFagsakBehandlingRef,
                kravgrunnlagPeriode = DatoperiodeEntity(kravgrunnlagPeriode.fom, kravgrunnlagPeriode.tom),
                vedtaksperiode = DatoperiodeEntity(vedtaksperiode.fom, vedtaksperiode.tom),
            )
        }
    }

    class Ukjent(
        id: UUID,
        override val eksternId: String,
        val revurderingsdatoFraKravgrunnlag: LocalDate?,
    ) : EksternFagsakRevurdering(id) {
        override val revurderingsårsak: Revurderingsårsak = Revurderingsårsak.UKJENT
        override val årsakTilFeilutbetaling: String = "Ukjent - finn i fagsystem"
        override val vedtaksdato: LocalDate = revurderingsdatoFraKravgrunnlag ?: LocalDate.MIN

        override fun behandlingId(): String? = null

        override fun utvidPeriode(periode: Datoperiode): Datoperiode = periode

        override fun tilEntity(eksternFagsakRef: UUID): EksternFagsakBehandlingEntity {
            return EksternFagsakBehandlingEntity(
                id = id,
                eksternFagsakRef = eksternFagsakRef,
                type = EksternFagsakBehandlingType.UKJENT,
                eksternId = eksternId,
                revurderingsårsak = null,
                årsakTilFeilutbetaling = null,
                vedtaksdato = null,
                utvidedePerioder = null,
            )
        }

        override fun tilFrontendDto(): RevurderingDto {
            return RevurderingDto(
                årsak = årsakTilFeilutbetaling,
                vedtaksdato = vedtaksdato,
                resultat = "Ukjent",
            )
        }
    }

    abstract fun tilEntity(eksternFagsakRef: UUID): EksternFagsakBehandlingEntity

    abstract fun tilFrontendDto(): RevurderingDto

    enum class Revurderingsårsak(private val entity: RevurderingsårsakType, val beskrivelse: String) {
        NYE_OPPLYSNINGER(RevurderingsårsakType.NYE_OPPLYSNINGER, "Nye opplysninger"),
        KORRIGERING(RevurderingsårsakType.KORRIGERING, "Korrigering"),
        KLAGE(RevurderingsårsakType.KLAGE, "Klage"),
        UKJENT(RevurderingsårsakType.UKJENT, "Ukjent"),
        ;

        fun tilEntity(): RevurderingsårsakType = entity
    }
}
