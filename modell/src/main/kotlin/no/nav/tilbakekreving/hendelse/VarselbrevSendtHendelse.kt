package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.brev.Brevtype
import no.nav.tilbakekreving.historikk.Historikk
import java.util.UUID

class VarselbrevSendtHendelse(
    override val internId: UUID,
    private val brevType: Brevtype,
    private val brevTekst: String? = null,
    val varsletBeløp: Long? = null,
) : Historikk.HistorikkInnslag<UUID> {
    companion object {
        fun opprettVarselBrev(
            varsletBeløp: Long,
        ): VarselbrevSendtHendelse {
            return VarselbrevSendtHendelse(
                internId = UUID.randomUUID(),
                brevType = Brevtype.VARSEL,
                varsletBeløp = varsletBeløp,
            )
        }

        fun opprettVedtakBrev(internId: UUID): VarselbrevSendtHendelse {
            return VarselbrevSendtHendelse(internId, Brevtype.VEDTAK)
        }
    }
}
