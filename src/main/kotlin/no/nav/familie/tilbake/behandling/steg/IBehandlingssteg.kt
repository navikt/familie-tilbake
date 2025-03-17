package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

interface IBehandlingssteg {
    fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Unit =
        throw Feil(
            message = "Implementasjon mangler, er i default method implementasjon for $behandlingId",
            logContext = logContext,
        )

    fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ): Unit =
        throw Feil(
            message = "Implementasjon mangler, er i default method implementasjon for $behandlingId",
            logContext = logContext,
        )

    fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Unit =
        throw Feil(
            message = "Implementasjon mangler, er i default method implementasjon for $behandlingId",
            logContext = logContext,
        )

    fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Unit =
        throw Feil(
            message = "Implementasjon mangler, er i default method implementasjon for $behandlingId",
            logContext = logContext,
        )

    fun getBehandlingssteg(): Behandlingssteg
}
