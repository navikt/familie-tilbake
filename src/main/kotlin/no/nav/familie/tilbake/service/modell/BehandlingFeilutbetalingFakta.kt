package no.nav.familie.tilbake.service.modell

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal
import java.time.LocalDate

class BehandlingFeilutbetalingFakta(val tidligereVarseltBeløp: Long? = null,
                                    val aktuellFeilUtbetaltBeløp: BigDecimal,
                                    val datoForRevurderingsvedtak: LocalDate? = null,
                                    val totalPeriode: Periode,
                                    val perioder: List<Periode>,
                                    val behandlingsresultat: Behandlingsresultatstype? = null,
                                    val behandlingÅrsaker: List<Behandlingsårsakstype>? = null,
                                    val tilbakekrevingValg: String? = null, // TODO legge inn riktig objekt med verdier
                                    val begrunnelse: String? = null)
