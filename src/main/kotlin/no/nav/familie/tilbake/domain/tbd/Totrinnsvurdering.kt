package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnsvurdering(@Id
                             val id: UUID = UUID.randomUUID(),
                             val behandlingId: UUID,
                             val aksjonspunktsdefinisjon: Aksjonspunktsdefinisjon,
                             val godkjent: Boolean,
                             val begrunnelse: String?,
                             val aktiv: Boolean = true,
                             val versjon: Int = 0,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())