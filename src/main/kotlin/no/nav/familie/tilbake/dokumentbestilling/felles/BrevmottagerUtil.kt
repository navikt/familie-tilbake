package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.behandling.domain.Verge as DomainVerge
import no.nav.tilbakekreving.kontrakter.verge.Verge as VergeDto

object BrevmottagerUtil {
    fun getVergenavn(
        verge: DomainVerge?,
        adresseinfo: Adresseinfo,
    ): String =
        if (Vergetype.ADVOKAT == verge?.type) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }

    fun getVergenavn(
        verge: VergeDto?,
        adresseinfo: Adresseinfo,
    ): String =
        if (Vergetype.ADVOKAT == verge?.vergetype) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }

    fun utledBrevmottager(
        behandling: Behandling,
        fagsak: Fagsak,
    ): Brevmottager =
        if (behandling.harVerge) {
            Brevmottager.VERGE
        } else if (fagsak.institusjon != null) {
            Brevmottager.INSTITUSJON
        } else {
            Brevmottager.BRUKER
        }
}
