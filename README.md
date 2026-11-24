# DS Microservices — Foyer / Université (Spring Cloud, Resilience4j, CQRS)

Projet individuel du module **Architecture des composants** (GL5, FST).
Point de départ : le projet DevOps **tpFoyer** (module Foyer + Université, plus Bloc), découpé en microservices puis enrichi
(résilience, surveillance, équilibrage de charge, CQRS avec Axon).

---

## 1. Architecture

```
                         ┌──────────────────────┐
  Angular (4200) ──────▶ │  API Gateway  :8080  │── CircuitBreaker + fallback, CORS
  Postman / Swagger      └──────────┬───────────┘
                                    │ lb://  (Spring Cloud LoadBalancer + Eureka)
                 ┌──────────────────┴──────────────────┬─────────────────────────────┐
                 ▼                                     ▼                             ▼
   ┌───────────────────────────┐   OpenFeign   ┌───────────────────────────┐  OpenFeign ┌──────────────────────┐
   │ universite-service :8082  │ ────────────▶ │ foyer-service :8081/:8083 │ ◀───────── │ bloc-service :8084   │
   │ Resilience4j : Retry,     │ ◀──────────── │ CRUD + CQRS (Axon)        │            │ contrôle de capacité │
   │ RateLimiter, CircuitBreak.│  RestTemplate │ simulation de pannes      │            │ + Resilience4j       │
   └─────────────┬─────────────┘  @LoadBalanced└───────┬──────────┬────────┘            └──────────┬───────────┘
                 ▼                                     ▼          ▼                                ▼
        MySQL universite_db (3308)       MySQL foyer_db (3307)  Axon Server (8024/8124)   MySQL bloc_db (3309)

  config-server :8888 (config-repo/)   eureka-server :8761
  Zipkin :9411   Prometheus :9090   Grafana :3000
```

| Composant | Port | Rôle |
|---|---|---|
| config-server | 8888 | Configuration centralisée (`config-repo/`, en local ou depuis Git) |
| eureka-server | 8761 | Enregistrement / découverte des services |
| gateway-service | 8080 | Point d'entrée unique, routage `lb://`, CORS, circuit breaker + fallback |
| foyer-service | 8081 (+ 8083) | Foyers : CRUD du projet DevOps, CQRS/Axon, endpoints de simulation de pannes |
| universite-service | 8082 | Universités : appelle foyer-service via OpenFeign, protégé par Resilience4j |
| bloc-service | 8084 | Blocs : affectation à un foyer avec contrôle de capacité (OpenFeign + Resilience4j) |
| mysql-foyer / mysql-universite / mysql-bloc | 3307 / 3308 / 3309 | Une base indépendante par microservice |
| Axon Server | 8024 (UI), 8124 (gRPC) | Event store + bus de commandes / requêtes |
| Zipkin | 9411 | Traces distribuées (Micrometer Tracing) |
| Prometheus / Grafana | 9090 / 3000 | Métriques (Micrometer) et tableau de bord fourni |
| frontend | 4200 | Angular du projet DevOps, branché sur la gateway (+ page CQRS) |

### Ce qui vient du projet DevOps et ce qui change

| Projet DevOps (monolithe) | Microservices |
|---|---|
| Une seule base `tpFoyer17` | `foyer_db` et `universite_db`, une par service |
| `Universite` → `@OneToOne Foyer foyer` | `Universite.idFoyer` (simple identifiant) ; le foyer est lu dans foyer-service via **Feign** |
| `ajouterFoyerEtAffecterAUniversite` dans une transaction | foyer-service enregistre le foyer puis appelle universite-service (**RestTemplate @LoadBalanced**) ; en cas d'échec le foyer est supprimé (**compensation**) |
| `Bloc` → `@ManyToOne Foyer foyer`, `@OneToMany chambres` | `Bloc.idFoyer` ; bloc-service lit la capacité du foyer via **Feign** pour refuser un bloc qui la dépasserait. Les chambres (autre module) sont retirées |
| `api/univeristes` (faute de frappe) | `api/universites` |
| `@CrossOrigin` sur chaque contrôleur | CORS géré une seule fois par la gateway |
| `context-path /tpFoyer17`, port 8082 | Plus de context-path, tout passe par la gateway (8080) |

### Correspondance avec la grille d'évaluation

| Critère | Où le montrer |
|---|---|
| Travail effectué (9 pts) | Étapes 1 à 9 ci-dessous |
| Scénario (1 pt) | Étape 7 : 5 défaillances + rate limiter, collection Postman dossier 4 |
| Excellence (3 pts) | Frontend Angular (avec page CQRS et badge « mode dégradé »), fallback de la gateway, tableau de bord Grafana fourni, compensation, tests unitaires (Mockito, Axon Test Fixture), Docker Compose complet, Sonar |
| Maîtrise du code et des concepts (5 pts) | Section 7 : questions probables |
| Git / JIRA (2 pts) | Section 3 : Git Flow + `docs/jira-backlog.csv` |

---

## 2. Prérequis

- JDK 17 ou plus, Maven 3.9, IntelliJ IDEA
- Docker Desktop
- Node.js 22 (pour le frontend), Postman
- **Libérer les ports** : arrêter les conteneurs du projet DevOps (Jenkins utilise 8080, Nexus 8081, Prometheus 9090, Grafana 3000) :
  `docker ps` puis `docker stop jenkins nexus prometheus grafana ...`

> **Ribbon** (TP4) ne fonctionne plus avec Spring Boot 3 : il a été retiré de Spring Cloud.
> L'énoncé autorise « Ribbon **ou** OpenFeign **ou** Spring Cloud LoadBalancer » : on utilise OpenFeign + Spring Cloud LoadBalancer
> (déjà inclus dans `spring-cloud-starter-netflix-eureka-client`). `RestTemplateConfig` est l'équivalent du `RibbonConfig` du TP.

---

## 3. Git Flow et JIRA

1. Créer un dépôt GitHub **public** `foyer-microservices` (public : le Config Server pourra le lire en mode Git).
2. Initialiser :
   ```bash
   git init && git checkout -b main
   git add pom.xml .gitignore README.md && git commit -m "chore: initialisation du projet multi-modules"
   git checkout -b develop
   git remote add origin https://github.com/<compte>/foyer-microservices.git
   git push -u origin main develop
   ```
3. Pour chaque étape : `git checkout develop && git checkout -b feature/<nom>`, ajouter les fichiers de l'étape,
   committer, pousser, ouvrir une Pull Request vers `develop`, fusionner. À la fin : `release/1.0.0` → `main` + tag `v1.0.0`.
4. JIRA : créer un projet Scrum, importer `docs/jira-backlog.csv` (Paramètres système → Import externe → CSV ;
   associer `Issue Id` et `Parent Id`). Mettre la clé du ticket dans chaque commit : `FM-12 feat(feign): ...`.

| Étape | Branche |
|---|---|
| 1 | `feature/config-server` |
| 2 | `feature/eureka-server` |
| 3 | `feature/foyer-service` |
| 4 | `feature/universite-service-feign` |
| 5 | `feature/api-gateway` |
| 6 | `feature/load-balancing` |
| 7 | `feature/resilience4j` |
| 8 | `feature/monitoring` |
| 9 | `feature/cqrs-axon` |
| 4b | `feature/bloc-service` |
| 10 | `feature/frontend-gateway` |
| 11 | `feature/docker-compose` |
| 12 | `feature/tests-sonar` |

> La présentation doit refléter le travail fait sur le PC étape par étape : faire les captures 📸 indiquées
> **au fur et à mesure**, et committer à chaque étape (l'historique Git fait partie de la note).

---

## 4. Démarrage rapide (ordre de lancement)

```bash
# 0. Infrastructure (MySQL x2, Axon Server, Zipkin, Prometheus, Grafana)
docker compose up -d

# 1. Compilation + tests unitaires
mvn clean install
```

Puis lancer dans IntelliJ (ou `mvn -pl <module> spring-boot:run`), **dans cet ordre** :

1. `config-server` → http://localhost:8888/foyer-service/default
2. `eureka-server` → http://localhost:8761
3. `foyer-service` (8081)
4. `foyer-service` 2e instance : *Run configuration → Modify options → Program arguments* `--server.port=8083`
   (ou `mvn -pl foyer-service spring-boot:run -Dspring-boot.run.arguments=--server.port=8083`)
5. `universite-service` (8082)
6. `bloc-service` (8084)
7. `gateway-service` (8080)
8. Frontend : `cd frontend && npm install && npm start` → http://localhost:4200

---

## 5. Étapes détaillées (= trame de la présentation)

### Étape 1 — Config Server
- `@EnableConfigServer` ; fichiers dans `config-repo/` : `application.yml` (commun : Eureka, Zipkin, Actuator),
  `foyer-service.yml`, `universite-service.yml`, `gateway-service.yml`.
- Les microservices n'ont presque rien en local : `spring.config.import=configserver:http://localhost:8888`
  (avec `fail-fast` + `retry` : ils attendent le Config Server au démarrage).
- Mode **Git** (comme dans le TP), une fois le dépôt poussé — variables d'environnement du run de config-server :
  `CONFIG_PROFILE=git` et `CONFIG_GIT_URI=https://github.com/<compte>/foyer-microservices.git` (`CONFIG_GIT_LABEL=develop` si besoin).
- 📸 http://localhost:8888/universite-service/default (on voit la config Resilience4j) ; la même URL en mode Git (champ `name` = URL GitHub).

### Étape 2 — Eureka Server
- `@EnableEurekaServer`, `register-with-eureka=false`, `fetch-registry=false`.
  Auto-préservation désactivée pour que les instances arrêtées disparaissent vite pendant la démo.
- 📸 Dashboard http://localhost:8761 avec `FOYER-SERVICE` (2 instances), `UNIVERSITE-SERVICE`, `GATEWAY-SERVICE`.

### Étape 3 — foyer-service
- Repris du projet DevOps : `Foyer`, `FoyerRepository`, `FoyerService`, `FoyerController` (mêmes endpoints).
- Supprimé : les relations JPA vers `Universite` et `Bloc` (autres bases / autres modules).
- Ajouté : métrique personnalisée `foyers.crees`, endpoint `/api/foyers/instance`, simulation de pannes (`chaos/`).
- 📸 Swagger http://localhost:8081/swagger-ui.html ; table `foyer` dans `foyer_db` :
  `docker exec -it mysql-foyer mysql -uroot foyer_db -e "select * from foyer"`.

### Étape 4 — universite-service + OpenFeign
- `Universite.idFoyer` remplace la relation `@OneToOne`.
- `FoyerClient` (`@FeignClient(name = "foyer-service")`) : l'adresse est résolue par Eureka.
- Toute la communication passe par `FoyerRemoteService` (Resilience4j, étape 7).
- La liste des universités fait **un seul** appel `getAll` à foyer-service (pas un appel par université).
- Communication inverse : `POST /api/foyers/ajouter-affecter` (foyer-service → universite-service via `RestTemplate @LoadBalanced`)
  avec **compensation** si universite-service ne répond pas.
- 📸 Postman dossier 2 : affectation d'un foyer, liste des universités avec le foyer rempli.

### Étape 4b — bloc-service
- Repris du projet DevOps : `Bloc`, `BlocDTO` (record), endpoints `add`, `getAll`, `get`, `delete`, `update`.
- `Bloc.idFoyer` remplace `@ManyToOne Foyer` ; `affecter-chambre-bloc` est retiré (les chambres sont le module d'une autre étudiante).
- Nouvelle règle métier inter-services : `PUT /api/blocs/affecter-bloc-foyer?idBloc=&idFoyer=` lit le foyer dans foyer-service
  (Feign + Resilience4j) et refuse (**409**) si la somme des capacités des blocs dépasse la capacité du foyer.
- `GET /api/blocs/par-foyer/{idFoyer}`, `PUT /api/blocs/desaffecter-bloc-foyer`.
- Même protection que universite-service (`config-repo/bloc-service.yml`) : foyer-service est ainsi appelé par **deux** clients,
  chacun avec son propre circuit breaker (visible séparément dans Grafana).
- 📸 Postman dossier 2b : affectation acceptée puis refusée (409 capacité dépassée) ; page « Blocs » du frontend.

### Étape 5 — API Gateway
- Routes `lb://foyer-service` (`/api/foyers/**`), `lb://universite-service` (`/api/universites/**`) et `lb://bloc-service` (`/api/blocs/**`).
- Filtre `CircuitBreaker` sur chaque route avec `fallbackUri: forward:/fallback/...` → réponse 503 propre si le service est absent.
- CORS global pour `http://localhost:4200` (les `@CrossOrigin` des contrôleurs ont été retirés).
- 📸 La même requête sur 8080 (gateway) ; `GET /api/foyers/getAll` avec foyer-service arrêté → JSON du fallback.

### Étape 6 — Équilibrage de charge
- 2 instances de foyer-service (8081, 8083) ; Spring Cloud LoadBalancer en round-robin.
- `GET http://localhost:8080/api/universites/lb-test?appels=10` → répartition 5 / 5 entre les deux instances.
- `scripts/demo-load-balancing.sh` (Mac) ou `.ps1` (Windows).
- 📸 Eureka avec 2 instances ; réponse de `lb-test` ; panneau Grafana « requêtes par instance ».

### Étape 7 — Résilience (Resilience4j) : le scénario de démo
Configuration dans `config-repo/universite-service.yml` (demandée par l'énoncé) :
**Retry** 3 tentatives / 500 ms, **RateLimiter** 5 req/s, **CircuitBreaker** (ouvert à ≥ 50 % d'échecs sur 10 appels, 10 s avant le demi-ouvert),
**Fallback** : l'université est renvoyée avec un foyer « dégradé » (`disponible: false`, badge rouge dans Angular).
Ordre d'exécution : `Retry( CircuitBreaker( RateLimiter( appel Feign ) ) )`.

| # | Défaillance (énoncé) | Comment la provoquer | Ce qu'on observe |
|---|---|---|---|
| 1 | Disponibilité | arrêter les 2 instances de foyer-service | 3 tentatives puis fallback ; après 2 requêtes le circuit s'**ouvre** (réponse immédiate) ; relancer le service → demi-ouvert → fermé |
| 2 | Temps de réponse > 5 s | `POST :8081/api/foyers/chaos?latenceMs=6000` (et :8083) | `read-timeout` Feign de 5 s dépassé → retry → fallback « délai de réponse dépassé » |
| 3 | Réponse incorrecte | `POST :8081/api/foyers/chaos?corrompre=true` (et :8083) | foyer sans nom / capacité -1 → `ReponseInvalideException` → fallback « réponse incorrecte » |
| 4 | Erreur réseau / URL mal configurée | `clients.foyer.name: foyer-servise` dans `config-repo/universite-service.yml`, redémarrer universite-service | LoadBalancer : aucune instance → 503 → fallback |
| 5 | Dépendance externe (base) | `docker stop mysql-foyer` | foyer-service répond 500 en 3 s (`connection-timeout`) → fallback ; `/actuator/health` de foyer-service : `db DOWN` |
| + | Rate limiter | `scripts/demo-rate-limiter.sh` (10 requêtes simultanées) | 5 réponses normales, 5 avec « limite de 5 requêtes/s atteinte » |

Remettre à zéro : `DELETE :8081/api/foyers/chaos` (et :8083), `docker start mysql-foyer`, remettre `foyer-service` dans le Config Server.
Une écriture (affecter un foyer) ne peut pas être « dégradée » : le fallback renvoie **503** (ou **429** pour le rate limiter) avec un message clair.

- 📸 Pour chaque ligne : la réponse Postman + `GET :8082/actuator/circuitbreakers` (état `OPEN`) + le panneau Grafana « Circuit breaker ».

### Étape 8 — Surveillance : Zipkin, Prometheus, Grafana
- Micrometer Tracing (Brave) + Zipkin, 100 % des requêtes tracées ; `feign-micrometer` propage le traceId dans les appels Feign.
- Chaque ligne de log contient `[application,traceId,spanId]` : on retrouve la même requête dans les logs des 3 services.
- Prometheus lit `/actuator/prometheus` de chaque instance (`monitoring/prometheus/prometheus-local.yml`).
- Grafana (admin / admin) : tableau de bord **« DS Microservices — Foyer / Université »** déjà provisionné
  (état du circuit breaker, retries, rate limiter, fallbacks par cause, requêtes par instance, p95, erreurs, mémoire).
- 📸 Zipkin : trace `gateway-service → universite-service → foyer-service` (http://localhost:9411) ;
  Prometheus → *Status → Targets* tout en UP ; le tableau de bord Grafana pendant un scénario de panne.

### Étape 9 — CQRS avec Axon (foyer-service)
- **Écriture** : `FoyerCommandController` → `CommandGateway` → `FoyerAggregate` (`@CommandHandler`, règles métier) →
  événements `FoyerCreeEvent`, `CapaciteFoyerModifieeEvent`, `FoyerSupprimeEvent` stockés dans **Axon Server** (Docker).
- **Lecture** : `FoyerProjection` (`@EventHandler`) remplit la table MySQL `foyer_view` ; `@QueryHandler` répond aux requêtes ;
  `FoyerQueryController` → `QueryGateway`.
- Event Sourcing : `GET /api/foyers/cqrs/queries/{id}/evenements` lit l'historique dans l'event store.
- Variante du TP (événements dans MySQL au lieu d'Axon Server) : lancer foyer-service avec `--spring.profiles.active=eventstore-mysql`
  (table `domain_event_entry`).
- Correction par rapport au TP : le TP n'a qu'un `@QueryHandler` ; sans `@EventHandler`, la table de lecture reste vide.
- 📸 Postman dossier 5 ; Axon Server http://localhost:8024 (onglet *Search* : `aggregateIdentifier = "<id>"`) ;
  `select * from foyer_view` ; page « Foyers (CQRS) » du frontend.

### Étape 10 — Frontend Angular
- Le frontend du projet DevOps appelle maintenant la gateway (`environment.ts` → `http://localhost:8080`).
- Nouveau : pages **Blocs** et **Foyers (CQRS)** (commandes, modèle de lecture, historique des événements), et badge rouge quand un foyer vient d'un fallback.
- 📸 Liste des universités avec le badge « indisponible » pendant une panne de foyer-service.

### Étape 11 (bonus) — Tout dans Docker
```bash
mvn clean package -DskipTests
PROMETHEUS_CONFIG=prometheus-docker.yml docker compose --profile apps up -d --build
```
Prometheus découvre alors les instances **via Eureka** (`eureka_sd_configs`). Frontend : http://localhost:4200.
Sous Windows PowerShell : `$env:PROMETHEUS_CONFIG="prometheus-docker.yml"; docker compose --profile apps up -d --build`.

### Étape 12 — Qualité
- `mvn clean verify` : tests Mockito (`FoyerServiceTest`, `UniversiteServiceTest`, `BlocServiceTest`) et tests d'agrégat Axon (`FoyerAggregateTest`), rapport JaCoCo.
- SonarQube (conteneur du projet DevOps, port 9000) : `mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>`.

---

## 6. Collection Postman et scripts

- `postman/DS-Microservices-Foyer.postman_collection.json` : dossiers 0 (infra) → 5 (CQRS), dans l'ordre de la démo.
- `scripts/demo-rate-limiter.sh|.ps1`, `scripts/demo-load-balancing.sh|.ps1`.

---

## 7. Questions probables à l'oral

- **Pourquoi un Config Server ?** Une seule source de configuration, versionnée dans Git ; on change un paramètre (ex. Resilience4j) sans recompiler.
- **Rôle d'Eureka ?** Les services s'enregistrent avec leur adresse ; les clients demandent « foyer-service » et non une IP fixe. Le heartbeat (5 s ici) retire les instances mortes.
- **Gateway vs LoadBalancer ?** La gateway est le point d'entrée externe (routage, CORS, fallback) ; le LoadBalancer choisit une instance (round-robin) pour chaque appel, y compris entre services (Feign, RestTemplate).
- **Feign vs RestTemplate ?** Feign : interface déclarative, moins de code. RestTemplate : impératif. Les deux sont utilisés ici (Feign université → foyer, RestTemplate foyer → université).
- **Pourquoi bloc-service appelle foyer-service ?** La capacité du foyer est dans foyer_db : bloc-service ne peut pas faire de jointure, il la demande par API (Feign). C'est la même idée que la clé étrangère, mais entre deux services.
- **Pourquoi une base par service ?** Indépendance (déploiement, schéma, panne). Conséquence : plus de jointure ni de transaction globale → on stocke `idFoyer` et on compense en cas d'échec (idée du pattern Saga).
- **Circuit breaker : les 3 états ?** CLOSED (les appels passent, on compte les échecs) → OPEN (≥ 50 % d'échecs : refus immédiat, fallback) → après 10 s HALF_OPEN (3 appels de test) → CLOSED ou OPEN.
- **Pourquoi ignorer certaines exceptions ?** Un 404 (foyer inexistant) est une erreur métier : ni retry ni ouverture du circuit. Un refus du rate limiter ou du circuit n'est pas réessayé.
- **Pourquoi désactiver le retry du LoadBalancer ?** Pour n'avoir qu'une politique de nouvelles tentatives (Resilience4j) et des chiffres de démo exacts.
- **CQRS ?** Séparer le modèle d'écriture (agrégat + événements) du modèle de lecture (vue optimisée). **Event Sourcing** : l'état est reconstruit à partir des événements ; on garde l'historique complet.
- **Cohérence à terme ?** La projection est mise à jour de façon asynchrone (processeur *tracking*) : une lecture juste après une commande peut être en retard de quelques millisecondes.
- **Axon Server ?** Event store + routage des commandes, événements et requêtes entre instances ; avec 2 instances, une commande pour un même agrégat va toujours à la même instance.
- **Traçabilité ?** Le traceId est propagé dans les en-têtes HTTP (B3) ; Zipkin reconstitue l'arbre des appels et le temps passé dans chaque service.

---

## 8. Dépannage

| Problème | Solution |
|---|---|
| Un service s'arrête au démarrage : « Could not locate PropertySource » | Lancer `config-server` d'abord (les services réessaient pendant ~1 min) |
| `Port 8080 already in use` | Jenkins (projet DevOps) tourne : `docker stop jenkins` |
| `Port 8081 already in use` | Nexus (projet DevOps) : `docker stop nexus` |
| `Port 8084` ou `3309` occupé | Arrêter le conteneur ou le service qui l'utilise (`docker ps`) |
| Prometheus : cibles DOWN | Normal pour les services non lancés ; sous Linux vérifier `host.docker.internal` |
| foyer-service : « No connection to AxonServer » | `docker compose up -d axonserver` puis attendre ~20 s |
| Lombok : symboles introuvables avec un JDK récent | Passer `lombok.version` à la dernière version dans le `pom.xml` racine |
| CORS dans le navigateur | Appeler la gateway (8080), pas les services directement |
