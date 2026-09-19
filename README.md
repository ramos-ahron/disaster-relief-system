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
