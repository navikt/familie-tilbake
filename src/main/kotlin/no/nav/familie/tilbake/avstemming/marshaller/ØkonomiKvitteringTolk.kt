package no.nav.familie.tilbake.avstemming.marshaller

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import java.util.Set

object ØkonomiKvitteringTolk {

    private val KVITTERING_OK_KODE = Set.of("00", "04")
    private const val KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES = "B420010I"
    const val KODE_MELDING_KRAVGRUNNLAG_ER_SPERRET = "B420012I"
    fun erKvitteringOK(kvittering: MmelDto?): Boolean {
        return kvittering != null && KVITTERING_OK_KODE.contains(kvittering.alvorlighetsgrad)
    }

    fun erKvitteringOK(response: TilbakekrevingsvedtakResponse?): Boolean {
        return response != null && erKvitteringOK(response.mmel)
    }

    fun erKravgrunnlagetIkkeFinnes(kvittering: MmelDto?): Boolean {
        return kvittering != null && KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES == kvittering.kodeMelding
    }

    fun erKravgrunnlagetSperret(kvittering: MmelDto?): Boolean {
        return kvittering != null && KODE_MELDING_KRAVGRUNNLAG_ER_SPERRET == kvittering.kodeMelding
    }

    fun harKravgrunnlagNoeUkjentFeil(kvittering: MmelDto?): Boolean {
        return kvittering != null && !nullOrEmpty(kvittering.kodeMelding) && !nullOrEmpty(kvittering.beskrMelding)
    }

    private fun nullOrEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }
}