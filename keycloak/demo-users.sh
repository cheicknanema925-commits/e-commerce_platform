#!/usr/bin/env bash
# Crée les comptes de démonstration du realm « boutique » (développement uniquement).
# Prérequis : docker compose --profile keycloak up -d --wait
# Usage, à la racine du projet : ./keycloak/demo-users.sh
set -euo pipefail
cd "$(dirname "$0")/.."

set -a
. ./.env
set +a
: "${KEYCLOAK_ADMIN_PASSWORD:?à définir dans .env}"
: "${KEYCLOAK_DEMO_PASSWORD:?à définir dans .env}"

kcadm() {
	docker compose exec -T keycloak /opt/keycloak/bin/kcadm.sh "$@"
}

kcadm config credentials --server http://localhost:8080 --realm master --user admin --password "$KEYCLOAK_ADMIN_PASSWORD"

# create_user <identifiant> <email> <prénom> <nom> <rôle>
create_user() {
	if kcadm get users -r boutique -q "username=$1" --fields id | grep -q '"id"'; then
		echo "• $1 existe déjà"
	else
		kcadm create users -r boutique -s "username=$1" -s "email=$2" -s emailVerified=true -s enabled=true \
			-s "firstName=$3" -s "lastName=$4"
		echo "• $1 créé"
	fi
	kcadm set-password -r boutique --username "$1" --new-password "$KEYCLOAK_DEMO_PASSWORD"
	kcadm add-roles -r boutique --uusername "$1" --rolename "$5"
}

create_user fatou fatou@example.com Fatou Sow CUSTOMER
create_user mariama mariama@example.com Mariama Ba ADMIN
echo "Comptes de démonstration prêts : fatou (CUSTOMER), mariama (ADMIN). Mot de passe : KEYCLOAK_DEMO_PASSWORD dans .env"
