package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelseperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entities.ForeldelsesvurderingEntity
import no.nav.tilbakekreving.entities.ForeldelsesvurderingType
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class Foreldelsesteg(
    private val id: UUID,
    private var vurdertePerioder: List<Foreldelseperiode>,
    private var underkjent: Boolean,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FORELDELSE

    override fun erFullstendig(klokke: Klokke): Boolean = vurdertePerioder.all { it.vurdering != Vurdering.IkkeVurdert }

    private fun erAutomatiskVurdert(): Boolean = vurdertePerioder.any { it.vurdering is Vurdering.AutomatiskIkkeForeldet }

    override fun erUnderkjent(): Boolean {
        return underkjent
    }

    override fun underkjennSteget() {
        this.underkjent = true
    }

    override fun nullstill(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering) {
        vurdertePerioder = tomVurdering(eksternFagsakRevurdering, kravgrunnlag)
    }

    override fun automatiskVurder(
        kravgrunnlag: KravgrunnlagHendelse,
        klokke: Klokke,
        behandlingslogg: Behandlingslogg,
        behandlingId: UUID,
    ) {
        val førsteUtbetalingFom = kravgrunnlag.perioder().minOf { it.periode().fom }
        val vurderingsdato = klokke.dagensDato()
        val kanVæreForeldet = vurderingsdato > førsteUtbetalingFom.plusMonths(30)
        if (kanVæreForeldet && !erAutomatiskVurdert()) return

        vurdertePerioder
            .filter { it.vurdering.kanOverstyresAutomatisk() }
            .forEach {
                if (kanVæreForeldet) {
                    it.vurderForeldelse(Vurdering.IkkeVurdert)
                } else {
                    it.vurderForeldelse(
                        Vurdering.AutomatiskIkkeForeldet.opprett(
                            førsteUtbetalingFom = førsteUtbetalingFom,
                            vurderingsdato = vurderingsdato,
                        ),
                    )
                }
            }
        behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = behandlingId,
                opprettetTid = klokke.nå(),
                behandlingsloggstype = if (erAutomatiskVurdert()) {
                    Behandlingsloggstype.AUTOMATISK_FORELDELSE_VURDERT
                } else {
                    Behandlingsloggstype.AUTOMATISK_FORELDELSE_FJERNET
                },
                rolle = Rolle.VEDTAKSLØSNING,
                behandlerIdent = "VL",
                ekstraInfo = emptyMap(),
            ),
        )
    }

    internal fun vurderForeldelse(
        periode: Datoperiode,
        vurdering: Vurdering,
    ) {
        val periodeId = finnIdFor(periode) // I fremtiden ønsker vi å sende inn id, ikke periode
        vurderForeldelse(periodeId, vurdering)
        underkjent = false
    }

    internal fun vurderForeldelse(
        periodeId: UUID,
        vurdering: Vurdering,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurdertePerioder.single { it.id == periodeId }.vurderForeldelse(vurdering)
    }

    fun erPeriodeForeldet(periode: Datoperiode): Boolean {
        return vurdertePerioder.single { it.periode.inneholder(periode) }.vurdering.erForeldet()
    }

    fun erSammenslåttPeriodeForeldet(periode: Datoperiode): Boolean {
        val overlappendePerioder = vurdertePerioder.filter {
            it.periode.fom <= periode.tom && it.periode.tom >= periode.fom
        }
        return overlappendePerioder.all { erPeriodeForeldet(it.periode) }
    }

    fun hjemlerForTilbakekreving() = vurdertePerioder.flatMap { it.vurdering.hjemlerForTilbakekreving() }.distinct()

    private fun finnIdFor(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurdertePerioder.single { it.periode == periode }.id
    }

    fun perioder(): List<Datoperiode> = vurdertePerioder
        .filter { it.vurdering is Vurdering.Foreldet }
        .map(Foreldelseperiode::periode)

    fun tilFrontendDto(
        kravgrunnlag: KravgrunnlagHendelse,
    ): VurdertForeldelseDto {
        return VurdertForeldelseDto(
            foreldetPerioder = vurdertePerioder.map {
                VurdertForeldelsesperiodeDto(
                    periode = it.periode,
                    feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(it.periode),
                    begrunnelse = it.vurdering.begrunnelse,
                    foreldelsesvurderingstype =
                        when (it.vurdering) {
                            is Vurdering.IkkeForeldet -> Foreldelsesvurderingstype.IKKE_FORELDET
                            is Vurdering.AutomatiskIkkeForeldet -> Foreldelsesvurderingstype.IKKE_FORELDET
                            is Vurdering.Foreldet -> Foreldelsesvurderingstype.FORELDET
                            is Vurdering.IkkeVurdert -> Foreldelsesvurderingstype.IKKE_VURDERT
                            is Vurdering.Tilleggsfrist -> Foreldelsesvurderingstype.TILLEGGSFRIST
                        },
                    foreldelsesfrist = it.vurdering.frist,
                    oppdagelsesdato = (it.vurdering as? Vurdering.Tilleggsfrist)?.oppdaget,
                )
            },
        )
    }

    fun tilEntity(behandlingRef: UUID): ForeldelsesstegEntity {
        return ForeldelsesstegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurdertePerioder = vurdertePerioder.map { it.tilEntity(id) },
            trengerNyVurdering = underkjent,
        )
    }

    class Foreldelseperiode internal constructor(
        val id: UUID,
        val periode: Datoperiode,
        private var _vurdering: Vurdering,
    ) {
        val vurdering get() = _vurdering

        fun vurderForeldelse(vurdering: Vurdering) {
            this._vurdering = vurdering
        }

        fun tilEntity(foreldelsesvurderingRef: UUID): ForeldelseperiodeEntity {
            return ForeldelseperiodeEntity(
                id = id,
                foreldelsesvurderingRef = foreldelsesvurderingRef,
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                foreldelsesvurdering = _vurdering.tilEntity(),
            )
        }

        companion object {
            fun opprett(periode: Datoperiode) =
                Foreldelseperiode(
                    id = UUID.randomUUID(),
                    periode = periode,
                    _vurdering = Vurdering.IkkeVurdert,
                )
        }
    }

    sealed interface Vurdering {
        val begrunnelse: String? get() = null
        val frist: LocalDate? get() = null

        fun tilEntity(): ForeldelsesvurderingEntity

        fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving>

        fun kanOverstyresAutomatisk(): Boolean

        fun erForeldet(): Boolean

        class IkkeForeldet(override val begrunnelse: String) : Vurdering {
            override fun kanOverstyresAutomatisk() = false

            override fun erForeldet() = false

            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.IKKE_FORELDET,
                    begrunnelse = begrunnelse,
                    frist = null,
                    oppdaget = null,
                )
            }

            override fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = emptyList()
        }

        object IkkeVurdert : Vurdering {
            override fun kanOverstyresAutomatisk() = true

            override fun erForeldet() = false

            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.IKKE_VURDERT,
                    begrunnelse = null,
                    frist = null,
                    oppdaget = null,
                )
            }

            override fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = emptyList()
        }

        class AutomatiskIkkeForeldet(
            override val begrunnelse: String,
        ) : Vurdering {
            override fun kanOverstyresAutomatisk() = true

            override fun erForeldet() = false

            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.AUTOMATISK_IKKE_FORELDET,
                    begrunnelse = begrunnelse,
                    frist = null,
                    oppdaget = null,
                )
            }

            override fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = emptyList()

            companion object {
                private val SAKSBEHANDLER_FORMAT = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.of("nb"))

                fun opprett(
                    førsteUtbetalingFom: LocalDate,
                    vurderingsdato: LocalDate,
                ): AutomatiskIkkeForeldet {
                    return AutomatiskIkkeForeldet(
                        begrunnelse = buildString {
                            appendLine("Ingen perioder er foreldet fordi det er mindre enn tre år siden første feilutbetaling fant sted. Dette følger av foreldelsesloven §§ 2 og 3.")
                            appendLine()
                            appendLine("Perioden er automatisk vurdert fordi det er mer enn 6 måneder til foreldelse inntreffer.")
                            appendLine()
                            appendLine("Ved den automatiske vurderingen av foreldelse er det tatt utgangspunkt i ${førsteUtbetalingFom.format(SAKSBEHANDLER_FORMAT)}, som er den første dagen i feilutbetalingsperioden.")
                            appendLine()
                            appendLine("Merk at foreldelse skal vurderes fra utbetalingstidspunktet, og at første dag i feilutbetalingsperioden har blitt valgt på grunn av automatiseringshensyn.")
                            appendLine()
                            append("Automatisk vurdering av foreldelse ble gjort ${vurderingsdato.format(SAKSBEHANDLER_FORMAT)}, som er den datoen saken ble sendt til beslutter")
                        },
                    )
                }
            }
        }

        class Tilleggsfrist(override val frist: LocalDate, val oppdaget: LocalDate) : Vurdering {
            override fun kanOverstyresAutomatisk() = false

            override fun erForeldet() = false

            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.TILLEGGSFRIST,
                    frist = frist,
                    oppdaget = oppdaget,
                    begrunnelse = null,
                )
            }

            override fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = listOf(
                HjemmelForTilbakekreving.FORELDELSESLOVEN_2,
                HjemmelForTilbakekreving.FORELDELSESLOVEN_3,
                HjemmelForTilbakekreving.FORELDELSESLOVEN_10,
            )
        }

        class Foreldet(override val begrunnelse: String) : Vurdering {
            override fun kanOverstyresAutomatisk() = false

            override fun erForeldet() = true

            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.FORELDET,
                    begrunnelse = begrunnelse,
                    frist = null,
                    oppdaget = null,
                )
            }

            override fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = listOf(
                HjemmelForTilbakekreving.FORELDELSESLOVEN_2,
                HjemmelForTilbakekreving.FORELDELSESLOVEN_3,
            )
        }
    }

    companion object {
        fun opprett(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlag: KravgrunnlagHendelse,
        ): Foreldelsesteg {
            return Foreldelsesteg(
                id = UUID.randomUUID(),
                vurdertePerioder = tomVurdering(eksternFagsakRevurdering, kravgrunnlag),
                underkjent = false,
            )
        }

        private fun tomVurdering(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlag: KravgrunnlagHendelse,
        ): List<Foreldelseperiode> {
            return kravgrunnlag.datoperioder(eksternFagsakRevurdering).map(Foreldelseperiode::opprett)
        }
    }
}
