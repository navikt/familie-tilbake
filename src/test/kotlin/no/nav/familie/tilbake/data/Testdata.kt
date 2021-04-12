package no.nav.familie.tilbake.data

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
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
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Aksjonspunkt
import no.nav.familie.tilbake.domain.tbd.Aksjonspunktsdefinisjon
import no.nav.familie.tilbake.domain.tbd.Aksjonspunktsstatus
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.Behandlingsstegstype
import no.nav.familie.tilbake.domain.tbd.Fritekstavsnittstype
import no.nav.familie.tilbake.domain.tbd.Meldingstype
import no.nav.familie.tilbake.domain.tbd.MottakersVarselrespons
import no.nav.familie.tilbake.domain.tbd.Navoppfulgt
import no.nav.familie.tilbake.domain.tbd.Revurderingsårsak
import no.nav.familie.tilbake.domain.tbd.SærligGrunn
import no.nav.familie.tilbake.domain.tbd.Totrinnsresultatsgrunnlag
import no.nav.familie.tilbake.domain.tbd.Totrinnsvurdering
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurdering
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingGodTro
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.domain.tbd.ÅrsakTotrinnsvurdering
import no.nav.familie.tilbake.domain.tbd.Årsakstype
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlSendt
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
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
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BARNETRYGD,
                        fagsystem = Fagsystem.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    private val date = LocalDate.now()

    private val fagsystemsbehandling =
            Fagsystemsbehandling(eksternId = UUID.randomUUID().toString(),
                                 tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                                 revurderingsvedtaksdato = date.minusDays(1),
                                 resultat = "OPPHØR",
                                 årsak = "testverdi")

    private val varsel = Varsel(varseltekst = "testverdi",
                                varselbeløp = 123,
                                perioder = setOf(Varselsperiode(fom = date.minusMonths(2), tom = date)))

    val verge = Verge(ident = "testverdi",
                      gyldigFom = LocalDate.now(),
                      gyldigTom = LocalDate.now(),
                      type = Vergetype.VERGE_FOR_BARN,
                      orgNr = "testverdi",
                      navn = "testverdi",
                      kilde = "testverdi",
                      begrunnelse = "testverdi")

    private val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                                      ansvarligSaksbehandler = "testverdi")

    private val behandlingsresultat = Behandlingsresultat(behandlingsvedtak = behandlingsvedtak)

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = false,
                                fagsystemsbehandling = setOf(fagsystemsbehandling),
                                resultater = setOf(behandlingsresultat),
                                varsler = setOf(varsel),
                                verger = setOf(verge),
                                eksternBrukId = UUID.randomUUID())

    val behandlingsårsak = Behandlingsårsak(type = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                                            originalBehandlingId = null)

    val aksjonspunkt = Aksjonspunkt(totrinnsbehandling = true,
                                    behandlingsstegstype = Behandlingsstegstype.FAKTA_OM_VERGE,
                                    aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon.FATTE_VEDTAK,
                                    behandlingId = behandling.id,
                                    tidsfrist = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                                    status = Aksjonspunktsstatus.OPPRETTET)

    val revurderingsårsak = Revurderingsårsak(aksjonspunktId = aksjonspunkt.id,
                                              årsakstype = Årsakstype.FEIL_LOV)

    val behandlingsstegstilstand = Behandlingsstegstilstand(behandlingId = behandling.id,
                                                            behandlingssteg = Behandlingssteg.FAKTA,
                                                            behandlingsstegsstatus = Behandlingsstegstatus.KLAR)


    val totrinnsvurdering = Totrinnsvurdering(behandlingId = behandling.id,
                                              aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon.FATTE_VEDTAK,
                                              godkjent = true,
                                              begrunnelse = "testverdi")

    val årsakTotrinnsvurdering = ÅrsakTotrinnsvurdering(årsakstype = Årsakstype.ANNET,
                                                        totrinnsvurderingId = totrinnsvurdering.id)

    val mottakersVarselrespons = MottakersVarselrespons(behandlingId = behandling.id,
                                                        akseptertFaktagrunnlag = true,
                                                        kilde = "testverdi")

    val foreldelsesperiode = Foreldelsesperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(1)),
                                                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                                                begrunnelse = "testverdi",
                                                foreldelsesfrist = LocalDate.now(),
                                                oppdagelsesdato = LocalDate.now())

    val vurdertForeldelse = VurdertForeldelse(behandlingId = behandling.id,
                                              foreldelsesperioder = setOf(foreldelsesperiode))

    val feilKravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(klassekode = Klassekode.KL_KODE_FEIL_BA,
                                                          klassetype = Klassetype.FEIL,
                                                          opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
                                                          nyttBeløp = BigDecimal("10000"),
                                                          tilbakekrevesBeløp = BigDecimal.ZERO,
                                                          uinnkrevdBeløp = BigDecimal.ZERO,
                                                          resultatkode = "testverdi",
                                                          årsakskode = "testverdi",
                                                          skyldkode = "testverdi",
                                                          skatteprosent = BigDecimal("35.1100"))

    val ytelKravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(klassekode = Klassekode.BATR,
                                                          klassetype = Klassetype.YTEL,
                                                          opprinneligUtbetalingsbeløp = BigDecimal("10000"),
                                                          nyttBeløp = BigDecimal.ZERO,
                                                          tilbakekrevesBeløp = BigDecimal("10000"),
                                                          uinnkrevdBeløp = BigDecimal.ZERO,
                                                          resultatkode = "testverdi",
                                                          årsakskode = "testverdi",
                                                          skyldkode = "testverdi",
                                                          skatteprosent = BigDecimal("35.1100"))

    val kravgrunnlagsperiode432 = Kravgrunnlagsperiode432(periode = Periode(YearMonth.now().minusMonths(1),
                                                                            YearMonth.now()),
                                                          beløp = setOf(feilKravgrunnlagsbeløp433,
                                                                        ytelKravgrunnlagsbeløp433),
                                                          månedligSkattebeløp = BigDecimal("123.11"))

    val kravgrunnlag431 = Kravgrunnlag431(behandlingId = behandling.id,
                                          vedtakId = BigInteger.ZERO,
                                          kravstatuskode = Kravstatuskode.NYTT,
                                          fagområdekode = Fagområdekode.BA,
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
                                          perioder = setOf(kravgrunnlagsperiode432),
                                          aktiv = true,
                                          sperret = false)

    private val vilkårsvurderingSærligGrunn = VilkårsvurderingSærligGrunn(særligGrunn = SærligGrunn.GRAD_AV_UAKTSOMHET,
                                                                          begrunnelse = "testverdi")

    private val vilkårsvurderingGodTro = VilkårsvurderingGodTro(beløpErIBehold = true,
                                                                beløpTilbakekreves = BigDecimal("32165"),
                                                                begrunnelse = "testverdi")

    private val vilkårsvurderingAktsomhet =
            VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                      ileggRenter = true,
                                      andelTilbakekreves = BigDecimal("123.11"),
                                      manueltSattBeløp = null,
                                      begrunnelse = "testverdi",
                                      særligeGrunnerTilReduksjon = true,
                                      særligeGrunnerBegrunnelse = "testverdi",
                                      vilkårsvurderingSærligeGrunner = setOf(vilkårsvurderingSærligGrunn))

    private val vilkårsperiode =
            Vilkårsvurderingsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(1)),
                                     navoppfulgt = Navoppfulgt.HAR_IKKE_FULGT_OPP,
                                     vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                     begrunnelse = "testverdi",
                                     aktsomhet = vilkårsvurderingAktsomhet,
                                     godTro = vilkårsvurderingGodTro)

    val vilkår = Vilkårsvurdering(behandlingId = behandling.id,
                                  perioder = setOf(vilkårsperiode))

    private val faktaFeilutbetalingsperiode =
            FaktaFeilutbetalingsperiode(periode = Periode(LocalDate.now(), LocalDate.now().plusDays(1)),
                                        hendelsestype = Hendelsestype.BA_ANNET,
                                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST)

    val faktaFeilutbetaling = FaktaFeilutbetaling(begrunnelse = "testverdi",
                                                  aktiv = true,
                                                  behandlingId = behandling.id,
                                                  perioder = setOf(faktaFeilutbetalingsperiode))

    val økonomiXmlMottatt = ØkonomiXmlMottatt(melding = "testverdi",
                                              kravstatuskode = Kravstatuskode.NYTT,
                                              eksternFagsakId = "testverdi",
                                              ytelsestype = Ytelsestype.BARNETRYGD,
                                              referanse = "testverdi",
                                              eksternKravgrunnlagId = BigInteger.ZERO,
                                              vedtakId = BigInteger.ZERO,
                                              kontrollfelt = "testverdi")

    val økonomiXmlMottattArkiv = ØkonomiXmlMottattArkiv(melding = "testverdi",
                                                        eksternFagsakId = "testverdi",
                                                        ytelsestype = Ytelsestype.BARNETRYGD)

    val totrinnsresultatsgrunnlag = Totrinnsresultatsgrunnlag(behandlingId = behandling.id,
                                                              faktaFeilutbetalingId = faktaFeilutbetaling.id,
                                                              vurdertForeldelseId = vurdertForeldelse.id,
                                                              vilkårsvurderingId = vilkår.id)

    val vedtaksbrevsoppsummering = Vedtaksbrevsoppsummering(behandlingId = behandling.id,
                                                            oppsummeringFritekst = "testverdi",
                                                            fritekst = "testverdi")

    val vedtaksbrevsperiode = Vedtaksbrevsperiode(behandlingId = behandling.id,
                                                  periode = Periode(LocalDate.now(), LocalDate.now()),
                                                  fritekst = "testverdi",
                                                  fritekststype = Fritekstavsnittstype.FAKTA)

    val økonomiXmlSendt = ØkonomiXmlSendt(behandlingId = behandling.id,
                                          melding = "testverdi",
                                          kvittering = "testverdi",
                                          meldingstype = Meldingstype.VEDTAK)

    val brevsporing = Brevsporing(behandlingId = behandling.id,
                                  journalpostId = "testverdi",
                                  dokumentId = "testverdi",
                                  brevtype = Brevtype.VARSEL)

}
