# Disaster Relief Management System

A Java 17 application for relief workers after a disaster. It tracks the people affected, where they are, the supplies at each shelter, medical treatment, dietary needs, relationships between people and calls from relatives looking for someone.

It has two parts that share one PostgreSQL database:

- A desktop app (Swing) with one tab for central relief workers and one for workers at a location.
- An HTTP API for the family graph: who is whose parent, and who is or was a couple.

## Requirements

- JDK 17 or newer
- Maven 3.9
- PostgreSQL 16, or Docker to run it for you

## Quick start

Start the database, create the tables and load the demo data:

```
docker compose up -d db migrate
docker compose exec -T db psql -U oop -d ensf380project < database/sample/demo_data.sql
```

Build and start the desktop app:

```
mvn -q -DskipTests package
java -jar target/app.jar
```

Start the API instead:

```
java -jar target/app.jar api
curl localhost:8080/persons
```

Or run the API in a container with `docker compose up -d api`.

## Configuration

Both parts read the same environment variables.

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ensf380project` | JDBC URL of the database |
| `DB_USER` | `oop` | Database user |
| `DB_PASSWORD` | `ucalgary` | Database password |
| `PORT` | `8080` | API port |

The defaults match `docker-compose.yml` and only make sense on your own machine. Set real values everywhere else.

Without Docker, run `database/local_setup.sql` once as a PostgreSQL superuser to create the `oop` role and the `ensf380project` database, then apply the migrations:

```
PGHOST=localhost PGDATABASE=ensf380project PGUSER=oop PGPASSWORD=ucalgary sh database/migrate.sh
```

## Using the desktop app

The app loads everything from the database when it starts. Each button opens a short form. If a rule is broken (a date in the future, a phone number in the wrong format, more supplies than the shelter has), the app says why and changes nothing.

The central relief worker tab can log a call from someone looking for a person, search victims by any part of a name, show a victim's relationships, add a location, and add supplies to a shelter.

The location relief worker tab can list missing people, list who is at a location, register, move or discharge a victim, show a victim's details, show a shelter's stock, give supplies to a victim, and record a treatment.

A person counts as missing when they are not at a shelter. Victims are looked up by their id, which the lists show as `#3`.

Changes made in the app stay in memory and are lost when it closes. The database is only read.

## Family API

| Method and path | Parameters | Result |
|---|---|---|
| GET `/health` | | 200 while the process runs |
| GET `/ready` | | 200 if the database answers, otherwise 503 |
| GET `/persons` | | Every person, with id and name |
| GET `/persons/{id}/ancestors` | `maxDepth` (default 50) | The person, their ancestors with distance, and the links between them |
| GET `/persons/{id}/descendants` | `maxDepth` | The same, downward |
| POST `/parent-child` | `parent`, `child`, `kind` | 201, or 400 with a reason |
| DELETE `/parent-child` | `parent`, `child` | 200, or 400 if the link does not exist |
| POST `/partnerships` | `person1`, `person2`, `start`, optional `end` | 201 with the new id |

`kind` is `BIOLOGICAL`, `ADOPTIVE` or `STEP`. Dates are `yyyy-mm-dd`. Parameters go in the query string or as a form body:

```
curl -X POST localhost:8080/parent-child -d 'parent=3&child=1&kind=BIOLOGICAL'
curl 'localhost:8080/persons/3/descendants?maxDepth=2'
```

The database enforces the family rules: nobody is their own parent or ancestor, a pair has at most one link, and a child has at most two biological parents. Step and adoptive parents have no limit.

The API has no authentication. Keep it on a private network.

## Tests

```
mvn verify
```

Unit tests need nothing else. The database tests run only when `TEST_DB_URL` is set, and they wipe that database, so give them an empty one:

```
TEST_DB_URL=jdbc:postgresql://localhost:5432/drms_test TEST_DB_USER=drms TEST_DB_PASSWORD=drms mvn verify
```

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/edu/ucalgary/oop/model` | Domain classes and their rules |
| `src/main/java/edu/ucalgary/oop/service` | The in-memory registry, the two worker services, the `FamilyStore` interface |
| `src/main/java/edu/ucalgary/oop/persistence` | JDBC: loading the registry, the PostgreSQL family store |
| `src/main/java/edu/ucalgary/oop/api` | The HTTP API |
| `src/main/java/edu/ucalgary/oop/ui` | The Swing window |
| `src/main/resources/gender-options.txt` | Allowed gender values, one per line |
| `database/migrations` | Schema, applied in name order by `database/migrate.sh` |
| `database/sample/demo_data.sql` | Invented demo data |
| `Dockerfile`, `k8s`, `.github/workflows` | Container image, Kubernetes manifests, CI pipeline |

## More

- [docs/architecture.md](docs/architecture.md): class and table diagrams, where each rule is enforced
- [docs/deployment.md](docs/deployment.md): deploying the API to Azure Kubernetes Service
