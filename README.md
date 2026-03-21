# game-api

REST API backend du projet Toon City, développée avec **Spring Boot 3.2**.

## Stack

- **Java 21** + Spring Boot 3.2
- **PostgreSQL** — accès via Spring Data JPA
- **Flyway** — migrations versionnées (`V1` → `V8`)
- **JWT** — authentification stateless (HS256)
- **Gradle** (wrapper inclus)

## Endpoints principaux

| Méthode | Route | Auth | Description |
|---------|-------|------|-------------|
| `POST` | `/api/auth/register` | — | Inscription |
| `POST` | `/api/auth/token` | — | Connexion → JWT |
| `GET` | `/api/auth/me` | ✓ | Profil courant |
| `GET` | `/api/deditoons` | — | 10 dernières déditoons |
| `POST` | `/api/deditoons` | ✓ | Publier une déditoon (10 kreds) |
| `GET` | `/api/users` | — | Liste paginée des joueurs |
| `GET/POST` | `/api/rooms` | — / ✓ | Salles publiques |
| `GET/POST` | `/api/houses` | — / ✓ | Logements privés |

## Lancer en développement

```bash
# Terminal A — bootRun avec hot reload
make dev-api

# Terminal B — recompilation à chaque sauvegarde
make watch-api
```

Variables d'environnement requises (voir `.env.example`) :

```env
DB_URL=jdbc:postgresql://localhost:5432/toonlive
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=<min 32 caractères>
```

## Migrations

Les migrations Flyway sont dans `src/main/resources/db/migration/`.  
Chaque fichier suit le pattern `V{n}__{description}.sql`.

## Build production

```bash
./gradlew bootJar
# ou via Docker
docker build -t toon-city/game-api .
```
