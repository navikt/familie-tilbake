UPDATE tilbakekreving_faktavurdering
SET rettsgebyr_år_fra_saksbehandler = NULL
WHERE rettsgebyr_år_fra_saksbehandler = '0'
   OR btrim(rettsgebyr_år_fra_saksbehandler) = '';
