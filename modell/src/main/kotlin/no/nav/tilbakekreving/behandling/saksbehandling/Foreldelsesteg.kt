package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
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
import java.util.UUID

class Foreldelsesteg(
    private val id: UUID,
    private var vurdertePerioder: List<Foreldelseperiode>,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FORELDELSE

    override fun erFullstendig(): Boolean = vurdertePerioder.all { it.vurdering != Vurdering.IkkeVurdert }

    override fun nullstill(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering) {
        vurdertePerioder = tomVurdering(eksternFagsakRevurdering, kravgrunnlag)
    }

    internal fun vurderForeldelse(
        periode: Datoperiode,
        vurdering: Vurdering,
    ) {
        val periodeId = finnIdFor(periode) // I fremtiden ønsker vi å sende inn id, ikke periode
        vurderForeldelse(periodeId, vurdering)
    }

    internal fun vurderForeldelse(
        periodeId: UUID,
        vurdering: Vurdering,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurdertePerioder.single { it.id == periodeId }.vurderForeldelse(vurdering)
    }

    fun erPeriodeForeldet(periode: Datoperiode): Boolean {
        return vurdertePerioder.single { it.periode.inneholder(periode) }.vurdering !is Vurdering.IkkeForeldet
    }

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

        class IkkeForeldet(override val begrunnelse: String) : Vurdering {
            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.IKKE_FORELDET,
                    begrunnelse = begrunnelse,
                    frist = null,
                    oppdaget = null,
                )
            }
        }

        object IkkeVurdert : Vurdering {
            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.IKKE_VURDERT,
                    begrunnelse = null,
                    frist = null,
                    oppdaget = null,
                )
            }
        }

        class Tilleggsfrist(override val frist: LocalDate, val oppdaget: LocalDate) : Vurdering {
            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.TILLEGGSFRIST,
                    frist = frist,
                    oppdaget = oppdaget,
                    begrunnelse = null,
                )
            }
        }

        class Foreldet(override val begrunnelse: String) : Vurdering {
            override fun tilEntity(): ForeldelsesvurderingEntity {
                return ForeldelsesvurderingEntity(
                    type = ForeldelsesvurderingType.FORELDET,
                    begrunnelse = begrunnelse,
                    frist = null,
                    oppdaget = null,
                )
            }
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
            )
        }

        private fun tomVurdering(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlag: KravgrunnlagHendelse,
        ): List<Foreldelseperiode> {
            return kravgrunnlag.datoperioder().map { kravgrunnlagPeriode ->
                Foreldelseperiode.opprett(
                    periode = eksternFagsakRevurdering.utvidPeriode(kravgrunnlagPeriode),
                )
            }
        }
    }
}
