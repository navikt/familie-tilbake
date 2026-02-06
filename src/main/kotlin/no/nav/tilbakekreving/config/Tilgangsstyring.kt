package no.nav.tilbakekreving.config

import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO

data class Tilgangsstyring(
    val grupper: Map<FagsystemDTO, Map<Behandlerrolle, List<String>>>,
    val forvalterGruppe: String,
)
