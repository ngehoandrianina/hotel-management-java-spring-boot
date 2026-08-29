# Hotel Room Management API

API REST Spring Boot pour la gestion de chambres d'hotel : chambres, clients et
reservations, avec verification de disponibilite.

## Stack technique

- Java 21, Spring Boot 3.3
- Spring Data JPA + PostgreSQL (H2 pour les tests)
- Flyway (migrations de schema versionnees)
- MapStruct + Lombok
- Bean Validation
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, MockMvc
- JaCoCo (couverture de code)
- Docker + Jenkins (CI/CD)

## Architecture

```
controller/   -> endpoints REST (validation des entrees, codes HTTP)
service/      -> logique metier (interfaces + impl)
repository/   -> acces aux donnees (Spring Data JPA)
entity/       -> modele de persistance JPA
dto/          -> objets d'echange (entree/sortie API)
mapper/       -> conversion entity <-> DTO (MapStruct)
exception/    -> exceptions metier + handler global (@RestControllerAdvice)
```

## Fonctionnalites

- CRUD chambres (`/api/v1/rooms`), filtre par statut, changement de statut
- CRUD clients (`/api/v1/clients`)
- Reservations (`/api/v1/reservations`) avec :
  - verification des chevauchements de dates sur une meme chambre
  - passage automatique de la chambre en `RESERVEE` / `DISPONIBLE`
  - annulation (`PATCH /api/v1/reservations/{id}/cancel`)
- Gestion centralisee des erreurs (404, 409, 400 avec details de validation)
- Documentation interactive : `/swagger-ui.html`
- Healthcheck : `/actuator/health`

## Lancer en local

### Avec Docker Compose (recommande)

```bash
docker compose up --build
```

L'API est disponible sur `http://localhost:8080`, Postgres sur le port `5432`.
Flyway applique automatiquement les migrations (`src/main/resources/db/migration`)
au demarrage.

### Sans Docker

Pre-requis : Java 21, Maven 3.9+, PostgreSQL local.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Tests

```bash
mvn test                 # tests unitaires + integration (H2 en memoire)
mvn verify                # + verification du seuil de couverture JaCoCo (60%)
```

Repartition des tests :
- `service/*Test.java` : tests unitaires (Mockito) de la logique metier
- `controller/*Test.java` : tests `@WebMvcTest` (MockMvc) de la couche REST
- `repository/*Test.java` : tests `@DataJpaTest` sur H2
- `HotelManagementApplicationTests` : test de chargement du contexte Spring

Rapport de couverture genere dans `target/site/jacoco/index.html`.

## CI/CD - Jenkins

Le `Jenkinsfile` (pipeline declaratif) enchaine :

1. **Checkout**
2. **Build** (`mvn compile`)
3. **Tests unitaires/integration** + publication des rapports JUnit
4. **Couverture de code** JaCoCo + quality gate (seuil 60% de lignes couvertes)
5. **Analyse statique SonarQube** (optionnelle si le token est configure)
6. **Quality Gate** SonarQube
7. **Package** du jar + archivage
8. **Build & push de l'image Docker**
9. **Deploiement staging** (branche `develop`) puis **production** (branche
   `main`, avec validation manuelle)

### Pre-requis Jenkins

- Plugins : Pipeline, Git, JUnit, JaCoCo (ou `recordCoverage` via Warnings NG /
  Coverage plugin), SonarQube Scanner, Docker Pipeline
- Outils configures dans *Manage Jenkins > Tools* : `Maven-3.9`, `JDK-21`
- Credentials Jenkins a creer :
  - `docker-registry-url` (Secret text)
  - `docker-registry-credentials` (Username/password)
  - `sonarqube-token` (Secret text, optionnel)

## Variables d'environnement (production)

| Variable      | Description                  | Defaut       |
|---------------|-------------------------------|--------------|
| `DB_HOST`     | Hote PostgreSQL                | `localhost`  |
| `DB_PORT`     | Port PostgreSQL                | `5432`       |
| `DB_NAME`     | Nom de la base                 | `hotel_db`   |
| `DB_USERNAME` | Utilisateur base de donnees    | `hotel_user` |
| `DB_PASSWORD` | Mot de passe base de donnees   | `hotel_pass` |
| `SERVER_PORT` | Port d'ecoute de l'API         | `8080`       |
