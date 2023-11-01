kubectl config use-context dev-gcp
TILBAKE_LOKAL_SECRETS=$(kubectl -n teamfamilie get secret azuread-familie-tilbake-lokal -o json | jq '.data | map_values(@base64d)');

AZURE_APP_CLIENT_ID=$(echo "$TILBAKE_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_ID')
AZURE_APP_CLIENT_SECRET=$(echo "$TILBAKE_LOKAL_SECRETS" | jq -r '.AZURE_APP_CLIENT_SECRET')

if [ -z "$AZURE_APP_CLIENT_ID" ]
then
      return 1
else
      printf "%s;%s" "AZURE_APP_CLIENT_ID=$AZURE_APP_CLIENT_ID" "AZURE_APP_CLIENT_SECRET=$AZURE_APP_CLIENT_SECRET"
fi