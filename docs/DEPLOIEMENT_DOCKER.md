# Guide de deploiement Docker sur serveur distant

Ce guide explique comment deployer l'application de reconciliation sur un serveur Linux distant avec Docker Compose.

L'architecture de deploiement contient :

- `postgres` : base de donnees PostgreSQL 16.
- `pgadmin` : interface web d'administration PostgreSQL.
- `app` : backend Spring Boot expose sur le port `8087`, lance avec le profil Spring `dev` pour desactiver l'authentification.
- `frontend` : application Angular 19 servie par Nginx sur le port `4200`.

## 1. Prerequis serveur

Le serveur distant doit avoir :

- Un acces SSH.
- Docker installe.
- Docker Compose v2 installe.
- Les ports necessaires ouverts dans le firewall.

Verifier Docker :

```bash
docker --version
```

Cette commande confirme que Docker est installe.

Verifier Docker Compose :

```bash
docker compose version
```

Cette commande confirme que la commande moderne `docker compose` est disponible.

## 2. Ouvrir les ports reseau

Ports utilises par l'application :

- `4200` : frontend Angular.
- `8087` : backend Spring Boot.
- `5050` : pgAdmin.
- `5432` : PostgreSQL.

Sur un serveur de production, il est preferable de ne pas exposer `5432` publiquement. Si possible, limiter PostgreSQL au reseau Docker ou a une IP d'administration.

Exemple avec `ufw` :

```bash
sudo ufw allow 4200/tcp
sudo ufw allow 8087/tcp
sudo ufw allow 5050/tcp
```

Ces commandes autorisent l'acces au frontend, au backend et a pgAdmin.

Si PostgreSQL doit etre accessible depuis l'exterieur, ajouter :

```bash
sudo ufw allow 5432/tcp
```

Cette ouverture est a eviter sauf besoin explicite.

## 3. Preparer l'arborescence sur le serveur

Le fichier `docker-compose.yml` du backend reference le frontend avec le chemin relatif `../reconcillation-frontend`.

Il faut donc garder les deux projets cote a cote :

```text
/opt/reconciliation/
  reconcilliation-service/
  reconcillation-frontend/
```

Creer le dossier parent :

```bash
sudo mkdir -p /opt/reconciliation
sudo chown -R $USER:$USER /opt/reconciliation
```

La premiere commande cree le dossier de deploiement. La seconde donne les droits a l'utilisateur courant pour copier et gerer les fichiers.

## 4. Copier les projets sur le serveur

Depuis la machine locale, copier le backend :

```bash
scp -r C:/eclipse-workspace/reconcilliation-service user@IP_DU_SERVEUR:/opt/reconciliation/
```

Copier ensuite le frontend :

```bash
scp -r C:/eclipse-workspace/reconcillation-frontend user@IP_DU_SERVEUR:/opt/reconciliation/
```

Remplacer :

- `user` par l'utilisateur SSH du serveur.
- `IP_DU_SERVEUR` par l'adresse IP ou le nom DNS du serveur.

Alternative recommandee si les projets sont sur Git :

```bash
cd /opt/reconciliation
git clone URL_DU_BACKEND reconcilliation-service
git clone URL_DU_FRONTEND reconcillation-frontend
```

Git est preferable pour les mises a jour, car il evite de recopier tout le projet a chaque deploiement.

## 5. Verifier la configuration avant demarrage

Se connecter au serveur :

```bash
ssh user@IP_DU_SERVEUR
```

Aller dans le dossier backend :

```bash
cd /opt/reconciliation/reconcilliation-service
```

Verifier que Docker Compose comprend bien le fichier :

```bash
docker compose config
```

Cette commande affiche la configuration finale interpretee par Docker Compose. Elle permet de detecter les erreurs YAML, les chemins incorrects et les services mal declares.

## 6. Construire et demarrer les conteneurs

Lancer le deploiement :

```bash
docker compose up --build -d
```

Explication :

- `up` cree et demarre les services.
- `--build` force la construction des images backend et frontend.
- `-d` lance les conteneurs en arriere-plan.

Le premier demarrage peut etre long, car Docker telecharge les images de base et Maven/npm telechargent les dependances.

Le backend est lance avec :

```yaml
SPRING_PROFILES_ACTIVE: dev
```

Ce profil active la configuration `SecurityConfigForDev`, qui autorise les appels sans authentification. La connexion PostgreSQL reste forcee vers le service Docker `postgres` grace aux variables `SPRING_DATASOURCE_*` du fichier `docker-compose.yml`.

## 7. Suivre les logs

Afficher les logs de tous les services :

```bash
docker compose logs -f
```

Afficher seulement les logs du backend :

```bash
docker compose logs -f app
```

Afficher seulement les logs du frontend :

```bash
docker compose logs -f frontend
```

Quitter l'affichage des logs avec `CTRL+C`. Cela n'arrete pas les conteneurs si `docker compose up -d` a ete utilise.

## 8. Verifier les conteneurs

Lister les services :

```bash
docker compose ps
```

Les services attendus sont :

- `postgres`
- `pgadmin`
- `app`
- `frontend`

Verifier l'API backend :

```bash
curl http://localhost:8087/api
```

Selon les routes exposees, la reponse peut etre une erreur HTTP applicative. L'objectif est surtout de verifier que le serveur repond.

Verifier le frontend :

```bash
curl http://localhost:4200
```

Cette commande doit retourner du HTML servi par Nginx.

## 9. Acces depuis le navigateur

Depuis un poste client :

```text
Frontend : http://IP_DU_SERVEUR:4200
Backend  : http://IP_DU_SERVEUR:8087/api
pgAdmin  : http://IP_DU_SERVEUR:5050
```

Identifiants pgAdmin :

```text
Email    : admin@reconciliation.local
Password : admin
```

Connexion PostgreSQL depuis pgAdmin :

```text
Host     : postgres
Port     : 5432
Database : reconciliation_db
User     : postgres
Password : MAE2020
```

Le host est `postgres` parce que pgAdmin et PostgreSQL sont dans le meme reseau Docker Compose.

## 10. Mettre a jour l'application

Si les projets sont geres avec Git :

```bash
cd /opt/reconciliation/reconcilliation-service
git pull

cd /opt/reconciliation/reconcillation-frontend
git pull

cd /opt/reconciliation/reconcilliation-service
docker compose up --build -d
```

Ces commandes recuperent les nouvelles versions du backend et du frontend, puis reconstruisent les images Docker.

## 11. Redemarrer ou arreter

Redemarrer tous les services :

```bash
docker compose restart
```

Arreter les services sans supprimer les volumes :

```bash
docker compose down
```

Les donnees PostgreSQL, pgAdmin et les fichiers applicatifs restent conservees dans les volumes Docker.

Arreter et supprimer aussi les volumes :

```bash
docker compose down -v
```

Attention : cette commande supprime les donnees de la base PostgreSQL, les donnees pgAdmin et les fichiers stockes dans le volume applicatif.

## 12. Sauvegarde de la base PostgreSQL

Creer une sauvegarde SQL :

```bash
docker compose exec postgres pg_dump -U postgres reconciliation_db > backup_reconciliation_db.sql
```

Cette commande exporte la base dans un fichier SQL sur le serveur.

Restaurer une sauvegarde :

```bash
cat backup_reconciliation_db.sql | docker compose exec -T postgres psql -U postgres reconciliation_db
```

Cette commande reinjecte le fichier SQL dans PostgreSQL.

## 13. Recommandations production

Avant une vraie mise en production :

- Remplacer les mots de passe par des valeurs fortes.
- Ne pas exposer PostgreSQL publiquement.
- Placer un reverse proxy HTTPS devant le frontend, par exemple Nginx, Traefik ou Caddy.
- Utiliser un nom de domaine et un certificat TLS.
- Deplacer les secrets dans un fichier `.env` non versionne.
- Planifier une sauvegarde automatique PostgreSQL.
- Surveiller l'espace disque utilise par Docker avec `docker system df`.

## 14. Commandes utiles de diagnostic

Voir l'utilisation disque Docker :

```bash
docker system df
```

Voir les images :

```bash
docker images
```

Voir les volumes :

```bash
docker volume ls
```

Inspecter un volume :

```bash
docker volume inspect reconcilliation-service_pg_data
```

Entrer dans le conteneur backend :

```bash
docker compose exec app sh
```

Entrer dans PostgreSQL :

```bash
docker compose exec postgres psql -U postgres -d reconciliation_db
```

Ces commandes sont utiles pour diagnostiquer un probleme de configuration, de reseau ou de donnees.
