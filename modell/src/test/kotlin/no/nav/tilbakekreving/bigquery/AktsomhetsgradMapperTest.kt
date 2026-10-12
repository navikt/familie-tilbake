package no.nav.tilbakekreving.bigquery

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v1.dto.Aktsomhetsgrad
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class AktsomhetsgradMapperTest {
    @ParameterizedTest
    @MethodSource("modellvurderinger")
    fun `vurderinger fra ny løsning`(vurdering: Vurdering, forventet: Aktsomhetsgrad) {
        AktsomhetsgradMapper.fraVurdering(vurdering) shouldBe forventet
    }

    @Test
    fun `uvurdert periode i ny løsning`() {
        AktsomhetsgradMapper.fraVurdering(ForårsaketAvBruker.IkkeVurdert()) shouldBe null
    }

    @Test
    fun `ukjent vurderingstype i ny løsning`() {
        val vurdering = object : Vurdering {
            override val navn = "Ukjent vurdering"
        }
        shouldThrow<IllegalStateException> {
            AktsomhetsgradMapper.fraVurdering(vurdering)
        }
    }

    @ParameterizedTest
    @MethodSource("gamleVurderinger")
    fun `vurderinger fra gammel løsning`(resultat: Vilkårsvurderingsresultat, aktsomhet: Aktsomhet?, forventet: Aktsomhetsgrad?) {
        AktsomhetsgradMapper.fraVilkårsvurderingsresultat(resultat, aktsomhet) shouldBe forventet
    }

    @ParameterizedTest
    @EnumSource(
        value = Vilkårsvurderingsresultat::class,
        names = ["FORSTO_BURDE_FORSTÅTT", "FEIL_OPPLYSNINGER_FRA_BRUKER", "MANGELFULLE_OPPLYSNINGER_FRA_BRUKER"],
    )
    fun `vurdering uten påkrevd aktsomhet`(resultat: Vilkårsvurderingsresultat) {
        shouldThrow<IllegalArgumentException> {
            AktsomhetsgradMapper.fraVilkårsvurderingsresultat(resultat, null)
        }
    }

    companion object {
        @JvmStatic
        fun modellvurderinger() = listOf(
            Arguments.of(AnnenVurdering.GOD_TRO, Aktsomhetsgrad.GOD_TRO),
            Arguments.of(NivåAvForståelse.Type.Forstod, Aktsomhetsgrad.FORSTOD),
            Arguments.of(NivåAvForståelse.Type.BurdeForstått, Aktsomhetsgrad.BURDE_FORSTÅTT),
            Arguments.of(NivåAvForståelse.Type.MåForstått, Aktsomhetsgrad.BURDE_FORSTÅTT),
            Arguments.of(Aktsomhet.SIMPEL_UAKTSOMHET, Aktsomhetsgrad.UAKTSOM),
            Arguments.of(Aktsomhet.GROV_UAKTSOMHET, Aktsomhetsgrad.GROVT_UAKTSOM),
            Arguments.of(Aktsomhet.FORSETT, Aktsomhetsgrad.FORSETT),
        )

        @JvmStatic
        fun gamleVurderinger(): List<Arguments> {
            val aktsomhetsmapping = mapOf(
                Aktsomhet.SIMPEL_UAKTSOMHET to Aktsomhetsgrad.UAKTSOM,
                Aktsomhet.GROV_UAKTSOMHET to Aktsomhetsgrad.GROVT_UAKTSOM,
                Aktsomhet.FORSETT to Aktsomhetsgrad.FORSETT,
            )
            return listOf<Aktsomhet?>(null).plus(Aktsomhet.entries).flatMap {
                listOf(
                    Arguments.of(Vilkårsvurderingsresultat.GOD_TRO, it, Aktsomhetsgrad.GOD_TRO),
                    Arguments.of(Vilkårsvurderingsresultat.UDEFINERT, it, null),
                )
            } + Aktsomhet.entries.flatMap { aktsomhet ->
                listOf(
                    Arguments.of(
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhet,
                        if (aktsomhet == Aktsomhet.FORSETT) Aktsomhetsgrad.FORSTOD else Aktsomhetsgrad.BURDE_FORSTÅTT,
                    ),
                    Arguments.of(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, aktsomhet, aktsomhetsmapping.getValue(aktsomhet)),
                    Arguments.of(Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER, aktsomhet, aktsomhetsmapping.getValue(aktsomhet)),
                )
            }
        }
    }
}
