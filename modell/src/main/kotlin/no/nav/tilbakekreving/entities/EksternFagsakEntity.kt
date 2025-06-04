package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

data class EksternFagsakEntity(
    val eksternId: String,
    val ytelsestype: String,
    val fagsystem: String,
    val behandlinger: EksternFagsakBehandlingHistorikkEntity,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): EksternFagsak {
        return EksternFagsak(
            eksternId = eksternId,
            ytelsestype = when {
                ytelsestype.contains("BA") -> Ytelsestype.BARNETRYGD
                ytelsestype.contains("EFOG") -> Ytelsestype.OVERGANGSSTØNAD
                ytelsestype.contains("EFBT") -> Ytelsestype.BARNETILSYN
                ytelsestype.contains("EFSP") -> Ytelsestype.SKOLEPENGER
                ytelsestype.contains("KS") -> Ytelsestype.KONTANTSTØTTE
                else -> throw IllegalArgumentException("feil ytelsestype $ytelsestype")
            },
            fagsystem = when {
                fagsystem.contains("Barnetrygd") -> Fagsystem.BA
                fagsystem.contains("Enslig forelder") -> Fagsystem.EF
                fagsystem.contains("Kontantstøtte") -> Fagsystem.KONT
                fagsystem.contains("Infotrygd") -> Fagsystem.IT01
                else -> throw IllegalArgumentException("feil fagsystem $fagsystem")
            },
            behandlinger = behandlinger.fraEntity(),
            behovObservatør = behovObservatør,
        )
    }
}
