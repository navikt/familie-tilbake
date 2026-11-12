DELETE
FROM tilbakekreving_krav_statusmelding
WHERE id IN (
             'ea8341b0-b259-4fc9-b306-6e7656477cd2',
             '80d7e6e7-0d4b-446a-8288-4c097a988199',
             'e69add1d-01be-4222-89e1-6edca968c91b',
             'e3489599-af89-41f4-9b22-c3a440d84411',
             '7925df7b-23ad-46c7-a994-3911a17cdd81',
             '362295ce-369e-4855-91c8-7652a8b49682',
             '4700764f-dd55-4475-a680-1d967bfa5d5f',
             '24ed0216-602a-4b8f-a351-714f70629325',
             'b9f0f039-770e-46dc-b908-161c081f0bc5',
             '781f2634-2cbe-4692-9c0c-27e9357877d1',
             'ff1813f9-d6a1-439a-b0ee-6c2a9deec02f',
             'f44665ee-c40d-4376-b43b-1a99e2a3dc20',
             '58e69f49-ca27-4088-aed4-8060480a6efb',
             'c12b736d-4ae8-43df-b2aa-1b488a403aa7',
             '649305f3-82d9-44c7-9be6-6e60f7e1bc5a',
             '164f7b2e-e8ad-446e-85a9-794b2b267db1',
             'cf3e7c5a-d3f1-4fae-9c17-b0329481f1cd',
             '66047fa9-97d7-4f2d-ad61-6df759bf18e6',
             '9856c98f-b92f-4b8c-bb6a-9f7a5aa35268',
             'e521d527-bc83-44ca-965f-a870f8e7cb29',
             'f675cf9b-32cb-45d8-9c44-99a2cb89d33c',
             'df1a5df2-8c70-430e-83eb-a8b69b7b0b9c',
             '8f011c4c-381f-45ae-9fe2-7fae46919cdb',
             '4c4fab11-5eb6-46da-8f31-c73da92288cd',
             '1d61cfca-ef6c-4aaa-a291-7b173d7ec30d'
    );

UPDATE tilbakekreving
SET nåværende_tilstand='TIL_BEHANDLING'
WHERE id IN (
             9462,
             7383,
             7528,
             9266,
             7617,
             9409
    )
