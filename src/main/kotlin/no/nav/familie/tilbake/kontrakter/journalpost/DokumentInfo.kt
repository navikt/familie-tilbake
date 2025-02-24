package no.nav.familie.tilbake.kontrakter.journalpost

import no.nav.familie.tilbake.kontrakter.Brevkoder
import no.nav.familie.tilbake.kontrakter.Tema

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentstatus: Dokumentstatus? = null,
    val dokumentvarianter: List<Dokumentvariant>? = null,
    val logiskeVedlegg: List<LogiskVedlegg>? = null,
) {
    fun erBarnetrygdOrdinærSøknad() = Brevkoder.BARNETRYGD_ORDINÆR_SØKNAD == this.brevkode

    fun erBarnetrygdUtvidetSøknad() = Brevkoder.BARNETRYGD_UTVIDET_SØKNAD == this.brevkode

    fun erBarnetrygdSøknad() = Brevkoder.BARNETRYGD_BREVKODER.any { brevkode -> brevkode == this.brevkode }

    fun erKontantstøtteSøknad() = Brevkoder.KONTANTSTØTTE_SØKNAD == this.brevkode

    fun erSøknadForTema(tema: Tema): Boolean =
        when (tema) {
            Tema.BAR -> erBarnetrygdSøknad()
            Tema.KON -> erKontantstøtteSøknad()
            else -> throw Error("Støtter ikke tema $tema")
        }

    fun harOriginalVariant(): Boolean = this.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL } ?: false
}
