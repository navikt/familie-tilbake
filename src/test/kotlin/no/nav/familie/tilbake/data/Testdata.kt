package no.nav.familie.tilbake.data

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Fil
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.avstemming.domain.Avstemmingsfil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Vedtaksbrevbehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Vedtaksbrevgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Friteksttype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object Testdata {
    val avstemmingsfil = Avstemmingsfil(fil = Fil("File.txt", ByteArray(100) { 1 }))

    private val bruker = Bruker(ident = "32132132111")

    val fagsak =
        Fagsak(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            eksternFagsakId = "testverdi",
            bruker = bruker,
        )

    private val date = LocalDate.now()

    private val fagsystemsbehandling =
        Fagsystemsbehandling(
            eksternId = UUID.randomUUID().toString(),
            tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            revurderingsvedtaksdato = date.minusDays(1),
            resultat = "OPPHØR",
            årsak = "testverdi",
        )

    val varsel =
        Varsel(
            varseltekst = "testverdi",
            varselbeløp = 123,
            perioder = setOf(Varselsperiode(fom = date.minusMonths(2), tom = date)),
        )

    val verge =
        Verge(
            ident = "32132132111",
            type = Vergetype.VERGE_FOR_BARN,
            orgNr = "testverdi",
            navn = "testverdi",
            kilde = "testverdi",
            begrunnelse = "testverdi",
        )

    private val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now())

    val behandlingsresultat = Behandlingsresultat(behandlingsvedtak = behandlingsvedtak)

    fun lagBehandling(
        fagsakId: UUID = fagsak.id,
        ansvarligSaksbehandler: String = "saksbehandler",
        behandlingStatus: Behandlingsstatus = Behandlingsstatus.UTREDES,
    ) =
        Behandling(
            fagsakId = fagsakId,
            status = behandlingStatus,
            type = Behandlingstype.TILBAKEKREVING,
            opprettetDato = LocalDate.now(),
            avsluttetDato = null,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            ansvarligBeslutter = "beslutter",
            behandlendeEnhet = "testverdi",
            behandlendeEnhetsNavn = "testverdi",
            manueltOpprettet = false,
            fagsystemsbehandling = setOf(fagsystemsbehandling),
            resultater = setOf(behandlingsresultat),
            varsler = setOf(varsel),
            verger = setOf(verge),
            eksternBrukId = UUID.randomUUID(),
            begrunnelseForTilbakekreving = null,
        )

    fun lagRevurdering(originalBehandlingId: UUID) =
        Behandling(
            fagsakId = fagsak.id,
            årsaker =
                setOf(
                    Behandlingsårsak(
                        originalBehandlingId = originalBehandlingId,
                        type = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                    ),
                ),
            type = Behandlingstype.REVURDERING_TILBAKEKREVING,
            opprettetDato = LocalDate.now(),
            ansvarligSaksbehandler = "saksbehandler",
            behandlendeEnhet = "testverdi",
            behandlendeEnhetsNavn = "testverdi",
            manueltOpprettet = false,
            fagsystemsbehandling = setOf(fagsystemsbehandling.copy(id = UUID.randomUUID())),
            resultater = emptySet(),
            varsler = emptySet(),
            verger = setOf(verge.copy(id = UUID.randomUUID())),
            eksternBrukId = UUID.randomUUID(),
            begrunnelseForTilbakekreving = null,
        )

    fun lagBehandlingsstegstilstand(behandlingId: UUID) =
        Behandlingsstegstilstand(
            behandlingId = behandlingId,
            behandlingssteg = Behandlingssteg.FAKTA,
            behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
        )

    fun lagTotrinnsvurdering(behandlingId: UUID) =
        Totrinnsvurdering(
            behandlingId = behandlingId,
            behandlingssteg = Behandlingssteg.FAKTA,
            godkjent = true,
            begrunnelse = "testverdi",
        )

    private val foreldelsesperiode =
        Foreldelsesperiode(
            periode = Månedsperiode(LocalDate.now(), LocalDate.now().plusDays(1)),
            foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
            begrunnelse = "testverdi",
            foreldelsesfrist = LocalDate.now(),
            oppdagelsesdato = LocalDate.now(),
        )

    fun lagVurdertForeldelse(behandlingId: UUID) =
        VurdertForeldelse(
            behandlingId = behandlingId,
            foreldelsesperioder = setOf(foreldelsesperiode),
        )

    val feilKravgrunnlagsbeløp433 =
        Kravgrunnlagsbeløp433(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            klassetype = Klassetype.FEIL,
            opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
            nyttBeløp = BigDecimal("10000"),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal.ZERO,
            resultatkode = "testverdi",
            årsakskode = "testverdi",
            skyldkode = "testverdi",
            skatteprosent = BigDecimal("35.1100"),
        )

    val ytelKravgrunnlagsbeløp433 =
        Kravgrunnlagsbeløp433(
            klassekode = Klassekode.BATR,
            klassetype = Klassetype.YTEL,
            opprinneligUtbetalingsbeløp = BigDecimal("10000"),
            nyttBeløp = BigDecimal.ZERO,
            tilbakekrevesBeløp = BigDecimal("10000"),
            uinnkrevdBeløp = BigDecimal.ZERO,
            resultatkode = "testverdi",
            årsakskode = "testverdi",
            skyldkode = "testverdi",
            skatteprosent = BigDecimal("35.1100"),
        )

    val kravgrunnlagsperiode432 =
        Kravgrunnlagsperiode432(
            periode =
                Månedsperiode(
                    YearMonth.now().minusMonths(1),
                    YearMonth.now(),
                ),
            beløp =
                setOf(
                    feilKravgrunnlagsbeløp433,
                    ytelKravgrunnlagsbeløp433,
                ),
            månedligSkattebeløp = BigDecimal("123.11"),
        )

    fun lagKravgrunnlagsperiode(
        fom: LocalDate,
        tom: LocalDate,
    ): Kravgrunnlagsperiode432 =
        Kravgrunnlagsperiode432(
            periode =
                Månedsperiode(
                    fom,
                    tom,
                ),
            beløp =
                setOf(
                    feilKravgrunnlagsbeløp433,
                    ytelKravgrunnlagsbeløp433,
                ),
            månedligSkattebeløp = BigDecimal("123.11"),
        )

    fun lagKravgrunnlag(
        behandlingId: UUID,
        perioder: Set<Kravgrunnlagsperiode432> = setOf(kravgrunnlagsperiode432),
    ) =
        Kravgrunnlag431(
            behandlingId = behandlingId,
            vedtakId = BigInteger.ZERO,
            kravstatuskode = Kravstatuskode.NYTT,
            fagområdekode = Fagområdekode.EFOG,
            fagsystemId = "testverdi",
            fagsystemVedtaksdato = LocalDate.now(),
            omgjortVedtakId = BigInteger.ZERO,
            gjelderVedtakId = "testverdi",
            gjelderType = GjelderType.PERSON,
            utbetalesTilId = "testverdi",
            utbetIdType = GjelderType.PERSON,
            hjemmelkode = "testverdi",
            beregnesRenter = true,
            ansvarligEnhet = "testverdi",
            bostedsenhet = "testverdi",
            behandlingsenhet = "testverdi",
            kontrollfelt = "testverdi",
            saksbehandlerId = "testverdi",
            referanse = "testverdi",
            eksternKravgrunnlagId = BigInteger.ZERO,
            perioder = perioder,
            aktiv = true,
            sperret = false,
        )

    private val vilkårsvurderingSærligGrunn =
        VilkårsvurderingSærligGrunn(
            særligGrunn = SærligGrunn.GRAD_AV_UAKTSOMHET,
            begrunnelse = "testverdi",
        )

    private val vilkårsvurderingGodTro =
        VilkårsvurderingGodTro(
            beløpErIBehold = true,
            beløpTilbakekreves = BigDecimal("32165"),
            begrunnelse = "testverdi",
        )

    private val vilkårsvurderingAktsomhet =
        VilkårsvurderingAktsomhet(
            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
            ileggRenter = true,
            andelTilbakekreves = BigDecimal("123.11"),
            manueltSattBeløp = null,
            begrunnelse = "testverdi",
            særligeGrunnerTilReduksjon = true,
            særligeGrunnerBegrunnelse = "testverdi",
            vilkårsvurderingSærligeGrunner = setOf(vilkårsvurderingSærligGrunn),
        )

    val vilkårsperiode =
        Vilkårsvurderingsperiode(
            periode = Månedsperiode(LocalDate.now(), LocalDate.now().plusDays(1)),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            begrunnelse = "testverdi",
            aktsomhet = vilkårsvurderingAktsomhet,
            godTro = vilkårsvurderingGodTro,
        )

    fun lagVilkårsvurdering(behandlingId: UUID) =
        Vilkårsvurdering(
            behandlingId = behandlingId,
            perioder = setOf(vilkårsperiode),
        )

    private val faktaFeilutbetalingsperiode =
        FaktaFeilutbetalingsperiode(
            periode = Månedsperiode(LocalDate.now(), LocalDate.now().plusDays(1)),
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )

    fun lagFaktaFeilutbetaling(behandlingId: UUID) =
        FaktaFeilutbetaling(
            begrunnelse = "testverdi",
            aktiv = true,
            behandlingId = behandlingId,
            perioder =
                setOf(
                    FaktaFeilutbetalingsperiode(
                        periode = Månedsperiode("2020-04" to "2022-08"),
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                    faktaFeilutbetalingsperiode,
                ),
        )

    val økonomiXmlMottatt =
        ØkonomiXmlMottatt(
            melding = "testverdi",
            kravstatuskode = Kravstatuskode.NYTT,
            eksternFagsakId = "testverdi",
            ytelsestype = Ytelsestype.BARNETRYGD,
            referanse = "testverdi",
            eksternKravgrunnlagId = BigInteger.ZERO,
            vedtakId = BigInteger.ZERO,
            kontrollfelt = "2023-07-12-22.53.47.806186",
        )

    val økonomiXmlMottattArkiv =
        ØkonomiXmlMottattArkiv(
            gammel_okonomi_xml_mottatt_id = null,
            melding = "testverdi",
            eksternFagsakId = "testverdi",
            ytelsestype = Ytelsestype.BARNETRYGD,
        )

    fun lagVedtaksbrevsoppsummering(behandlingId: UUID) =
        Vedtaksbrevsoppsummering(
            behandlingId = behandlingId,
            oppsummeringFritekst = "testverdi",
            skalSammenslåPerioder = SkalSammenslåPerioder.IKKE_AKTUELT,
        )

    fun lagVedtaksbrevsperiode(behandlingId: UUID) =
        Vedtaksbrevsperiode(
            behandlingId = behandlingId,
            periode = Månedsperiode(LocalDate.now(), LocalDate.now()),
            fritekst = "testverdi",
            fritekststype = Friteksttype.FAKTA,
        )

    fun lagØkonomiXmlSendt(behandlingId: UUID) =
        ØkonomiXmlSendt(
            behandlingId = behandlingId,
            melding = "testverdi",
            kvittering = "testverdi",
        )

    fun lagBrevsporing(behandlingId: UUID) =
        Brevsporing(
            behandlingId = behandlingId,
            journalpostId = "testverdi",
            dokumentId = "testverdi",
            brevtype = Brevtype.VARSEL,
        )

    fun lagVedtaksbrevbehandling(behandling: Behandling) =
        Vedtaksbrevbehandling(
            id = behandling.fagsakId,
            type = Behandlingstype.TILBAKEKREVING,
            ansvarligSaksbehandler = "saksbehandler",
            ansvarligBeslutter = "beslutter",
            behandlendeEnhet = "testverdi",
            behandlendeEnhetsNavn = "testverdi",
            fagsystemsbehandling = setOf(fagsystemsbehandling),
            resultater = setOf(behandlingsresultat),
            varsler = setOf(varsel),
            verger = setOf(verge),
            vedtaksbrevOppsummering = lagVedtaksbrevsoppsummering(behandling.id),
            saksbehandlingstype = behandling.saksbehandlingstype,
        )

    fun lagVedtaksbrevgrunnlag(behandling: Behandling) =
        Vedtaksbrevgrunnlag(
            id = behandling.id,
            bruker = bruker,
            eksternFagsakId = "testverdi",
            fagsystem = Fagsystem.BA,
            ytelsestype = Ytelsestype.BARNETRYGD,
            behandlinger = setOf(lagVedtaksbrevbehandling(behandling)),
        )

    fun lagFeilBeløp(feilutbetaling: BigDecimal): Kravgrunnlagsbeløp433 =
        Kravgrunnlagsbeløp433(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            klassetype = Klassetype.FEIL,
            nyttBeløp = feilutbetaling,
            opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal.ZERO,
            skatteprosent = BigDecimal.ZERO,
        )

    fun lagYtelBeløp(
        utbetalt: BigDecimal,
        skatteprosent: BigDecimal,
    ): Kravgrunnlagsbeløp433 =
        Kravgrunnlagsbeløp433(
            klassekode = Klassekode.BATR,
            klassetype = Klassetype.YTEL,
            tilbakekrevesBeløp = BigDecimal("10000"),
            opprinneligUtbetalingsbeløp = utbetalt,
            nyttBeløp = BigDecimal.ZERO,
            skatteprosent = skatteprosent,
        )

    fun lagYtelBeløp(
        utbetalt: BigDecimal,
        nyttBeløp: BigDecimal,
        skatteprosent: BigDecimal,
    ): Kravgrunnlagsbeløp433 {
        return Kravgrunnlagsbeløp433(
            klassekode = Klassekode.BATR,
            klassetype = Klassetype.YTEL,
            tilbakekrevesBeløp = BigDecimal("10000"),
            opprinneligUtbetalingsbeløp = utbetalt,
            nyttBeløp = nyttBeløp,
            skatteprosent = skatteprosent,
            skyldkode =
                UUID
                    .randomUUID()
                    .toString(),
        ) // brukte skyldkode for å få ulike Kravgrunnlagsbeløp433
    }
}
