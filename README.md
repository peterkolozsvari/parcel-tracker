# Parcel Tracker

Csomagkövető alkalmazás Java/Spring Boot backenddel és Angular frontenddel. Az ügyfél küldeményt adhat fel és követheti annak állapotát; az admin futárt rendelhet hozzá és felhasználókat kezelhet; a futár a hozzá rendelt küldemények állapotát módosíthatja.

## Technológiák

| Terület | Megoldás |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA |
| Adatbázis | PostgreSQL 17, Flyway migrációk |
| Hitelesítés | Spring Security, szerveroldali session, CSRF-védelem |
| Frontend | Angular 21.2, standalone komponensek, signals, Reactive Forms, RxJS, Bootstrap |
| Tesztelés | JUnit 5, Mockito, MockMvc, PostgreSQL-integrációs tesztek; Vitest |
| Automatizálás | GitHub Actions backend- és frontendellenőrzéssel |

## Helyi indítás

Szükséges: JDK 21, Node.js 22, npm, Docker és Docker Compose. Az adatbázis helyett saját PostgreSQL 17 is használható.

A repó gyökerében indítsd a fejlesztői adatbázist:

```sh
docker compose up -d
```

A `compose.yaml` fejlesztői beállításai: adatbázis `parcel_tracker`, port `5433`, felhasználó `postgres`, jelszó `root`. Ezek kizárólag helyi fejlesztési alapértékek. A fejlesztői adatokat névvel ellátott Docker-volume őrzi; a `docker compose down -v` ezeket törli.

Ezután indítsd a backendet:

```sh
./mvnw spring-boot:run
```

Windows alatt: `mvnw.cmd spring-boot:run`. A backend címe `http://localhost:8080`, a Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

Másik terminálban indítsd a frontendet:

```sh
cd frontend
npm ci
npm start
```

A felület címe `http://localhost:4200`. A frontend API-alapcíme a `frontend/src/app/config/api.config.ts` fájlban található. A backend jelenlegi CORS-konfigurációja a `http://localhost:4200` és `http://localhost:3000` origineket engedi cookie-val; más origint külön be kell állítani.

A backend adatbázis-kapcsolata környezeti változókkal felülírható: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Működés

- A regisztráció ügyfélfiókot hoz létre; nem jelentkeztet be automatikusan.
- A profilban mentett cím alapértelmezett felvételi cím. Egy konkrét feladáshoz ettől eltérő cím adható meg.
- A küldemény a feladáskor kapott címeket őrzi. A profil későbbi módosítása nem írja át ezeket.
- Legalább egy csomag szükséges, a súly pozitív, legfeljebb két tizedesjeggyel.
- A saját küldeménylista lapozható és státusz szerint szűrhető; alapból a legújabb küldemények kerülnek előre.
- Az admin összes küldeményt láthat, aktív futárt rendelhet hozzá, valamint felhasználói szerepkört és aktivitást módosíthat.
- A futár kizárólag a hozzá rendelt küldeményeket kezelheti. Az ügyfél nem módosíthat küldeménystátuszt.
- A saját adminhozzáférés visszavonása tiltott. Szerepkörváltás után az érintett felhasználónak újra be kell jelentkeznie.

Az első adminfiók beállítását és a bemutató lépéseit a [demóútmutató](docs/demo.md) tartalmazza. Nincs beépített, közös adminjelszó.

## Ellenőrzés

```sh
./mvnw test
```

Ez a gyors backendtesztcsomag, nem igényel futó adatbázist. A valódi PostgreSQL-integrációhoz:

```sh
docker compose -f compose.test.yaml up -d --wait
./mvnw -Pintegration verify
docker compose -f compose.test.yaml down
```

Az integráció külön `parcel_tracker_test` adatbázist használ a `55432` porton. A tesztadatbázis neve kötelezően `_test` végű; minden futás saját, egyedi sémát kap, amelyet sikeres takarításkor eltávolít. A részleteket és az egyedi kapcsolat beállítását a [tesztelési útmutató](docs/testing.md) tartalmazza.

```sh
cd frontend
npm test -- --watch=false
npm run build
```

## További dokumentáció

- [Architektúra és adatmodell](docs/architecture.md)
- [API, jogosultságok és hibaválaszok](docs/api.md)
- [Tesztelés és automatizálás](docs/testing.md)
- [Bemutató forgatókönyv](docs/demo.md)
- [Végső ellenőrzés és ismert korlátok](docs/final-review.md)

## Migrációs szabály

Indításkor a Flyway futtatja a még nem alkalmazott migrációkat, a Hibernate pedig ellenőrzi a sémát (`ddl-auto: validate`). Már lefutott migrációt ne szerkessz: új változtatáshoz új, következő verziójú fájlt készíts. A V2 és V3 szándékosan megőrzött, üres történeti migráció; utólagos kitöltésük checksum-hibát okozna. A séma módosításait a V4 és V5 tartalmazza.

Az alkalmazás jelenleg helyi fejlesztési/bemutató konfigurációt tartalmaz. Az éles üzemhez hiányzó lépéseket a végső ellenőrzés dokumentuma sorolja fel.
