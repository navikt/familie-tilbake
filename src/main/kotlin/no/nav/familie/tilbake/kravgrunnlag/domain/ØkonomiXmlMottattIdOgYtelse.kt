package no.nav.familie.tilbake.kravgrunnlag.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.util.UUID

class ØkonomiXmlMottattIdOgYtelse(val id: UUID, val eksternKravgrunnlagId: Long, val ytelsestype: Ytelsestype)
