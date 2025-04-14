package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

class VilkårsvurderingsperiodeAdapter(private val vurdering: Vilkårsvurderingsperiode) : VilkårsvurdertPeriodeAdapter {
    override fun periode(): Datoperiode {
        return vurdering.periode.toDatoperiode()
    }

    override fun ignoreresPgaLavtBeløp(): Boolean {
        return vurdering.aktsomhet?.tilbakekrevSmåbeløp == false
    }

    override fun renter(): Boolean {
        val aktsomhet = vurdering.aktsomhet ?: return false
        return aktsomhet.aktsomhet == Aktsomhet.FORSETT && aktsomhet.ileggRenter ?: true || aktsomhet.ileggRenter ?: false
    }

    override fun reduksjon(): Reduksjon {
        return when {
            vurdering.aktsomhet != null -> {
                vurdering.aktsomhet.manueltSattBeløp?.let(Reduksjon::ManueltBeløp)
                    ?: finnAndelForAktsomhet(vurdering.aktsomhet)
            }
            vurdering.godTro != null -> {
                when (vurdering.godTro.beløpErIBehold) {
                    true -> Reduksjon.ManueltBeløp(vurdering.godTro.beløpTilbakekreves ?: error("Beløp er i behold, men beløp som skal tilbakekreves er ikke satt."))
                    false -> Reduksjon.IngenTilbakekreving()
                }
            }
            else -> error("Vurdering mangler vurdering av aktsomhet og god tro. Kan ikke beregne reduksjon.")
        }
    }

    private fun finnAndelForAktsomhet(aktsomhet: VilkårsvurderingAktsomhet): Reduksjon =
        if (Aktsomhet.SIMPEL_UAKTSOMHET == aktsomhet.aktsomhet && !aktsomhet.tilbakekrevSmåbeløp) {
            Reduksjon.IngenTilbakekreving()
        } else if (Aktsomhet.FORSETT == aktsomhet.aktsomhet || !aktsomhet.særligeGrunnerTilReduksjon) {
            Reduksjon.FullstendigRefusjon()
        } else {
            Reduksjon.Prosentdel(aktsomhet.andelTilbakekreves ?: error("Særlige grunner til reduksjon er satt, men andel mangler."))
        }

    override fun vurdering(): Vurdering {
        return when {
            vurdering.aktsomhet != null -> vurdering.aktsomhet.aktsomhet
            vurdering.godTro != null -> AnnenVurdering.GOD_TRO
            else -> throw IllegalArgumentException("Vurdering skal peke til GodTro-entiet eller Aktsomhet-entitet")
        }
    }
}
