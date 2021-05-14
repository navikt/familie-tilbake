package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype


object HendelsestypePerYtelsestype {

    private val HIERARKI = mapOf(Ytelsestype.BARNETRYGD to setOf(Hendelsestype.BA_ANNET,
                                                                 Hendelsestype.ØKONOMIFEIL,
                                                                 Hendelsestype.MEDLEMSKAP),
                                 Ytelsestype.OVERGANGSSTØNAD to setOf(Hendelsestype.EF_ANNET,
                                                                      Hendelsestype.ØKONOMIFEIL,
                                                                      Hendelsestype.MEDLEMSKAP),
                                 Ytelsestype.BARNETILSYN to setOf(Hendelsestype.EF_ANNET,
                                                                  Hendelsestype.ØKONOMIFEIL,
                                                                  Hendelsestype.MEDLEMSKAP),
                                 Ytelsestype.SKOLEPENGER to setOf(Hendelsestype.EF_ANNET,
                                                                  Hendelsestype.ØKONOMIFEIL,
                                                                  Hendelsestype.MEDLEMSKAP),
                                 Ytelsestype.KONTANTSTØTTE to setOf(Hendelsestype.KS_ANNET,
                                                                    Hendelsestype.ØKONOMIFEIL,
                                                                    Hendelsestype.MEDLEMSKAP))

    fun getHendelsestyper(ytelsestype: Ytelsestype): Set<Hendelsestype> {
        return HIERARKI[ytelsestype] ?: error("Ikke-støttet ytelsestype: $ytelsestype")
    }
}