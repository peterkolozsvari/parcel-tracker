# Tesztelés – KAN-16

## Gyors backendtesztek

```sh
./mvnw test
```

A service- és MockMvc-controller tesztek adatbázis nélkül futnak. A repository-függőségeket Mockito helyettesíti, a security-controller tesztek a Spring Security filterláncát is használják.

Ellenőrzött területek: tulajdonosi és szerepköralapú hozzáférés, hamisított feladó tiltása, inaktív felhasználó, lapozás és szűrés átadása, futárhozzárendelés, státuszátmenetek, előzmények hozzáférése, admin saját hozzáférésének védelme, valamint a KAN-15 egységes hibaválaszai.

A külön `ApiErrorControllerTest` hibás JSON-t, mezővalidációt, 401/403-at a security filterből, CSRF-hiányt, 404/409/500 válaszokat és a belső adatok visszaadásának tiltását is ellenőrzi. A tesztben megadott érzékeny mintaszöveg nem kerülhet a válaszba.

A korábbi, fejlesztői adatbázist igénylő üres `contextLoads` teszt helyett az integrációs teszt indítja el és ellenőrzi a teljes alkalmazást.

## PostgreSQL-integráció

```sh
docker compose -f compose.test.yaml up -d --wait
./mvnw -Pintegration verify
docker compose -f compose.test.yaml down
```

Windows alatt `mvnw.cmd -Pintegration verify` használandó. A `verify` a gyors teszteket is lefuttatja, majd a Maven Failsafe a `*IT` osztályokat futtatja.

Alapértékek:

| Változó | Alapérték |
|---|---|
| `POSTGRES_TEST_URL` | `jdbc:postgresql://localhost:55432/parcel_tracker_test` |
| `POSTGRES_TEST_USER` | `postgres` |
| `POSTGRES_TEST_PASSWORD` | `test-only-password` |

Másik PostgreSQL 17 használatakor ezeket a környezeti változókat kell megadni. A céladatbázisnak már léteznie kell, nevének `_test` végűnek kell lennie; a felhasználónak sémalétrehozási jogosultság kell. A fejlesztői `parcel_tracker` adatbázist a teszt elutasítja.

Minden osztályfutás egyedi `it_…` sémát használ. A Flyway ebben futtatja az eredeti migrációkat, a Hibernate `validate` ellenőrzi az entitásokat. A tesztek előtt kizárólag e séma táblái ürülnek. A futás végén a séma törlődik; megszakított folyamat után egy `it_…` séma megmaradhat a tesztadatbázisban, ezt ellenőrzés után kézzel lehet eltávolítani.

A `ShipmentWorkflowIT` valódi HTTP-kéréseket és cookie-tárolót használ, nem mockolt belépést. Az összetett tesztesetek:

1. Verziózott migrációk, regisztráció, jelszóhash, ismételt email, hibás/jó belépés és kijelentkezés.
2. Csomagfeladás → admin futárhozzárendelés → futár státuszváltásai → kézbesítés, rendezett és hiánytalan előzményekkel; másik ügyfél és futár tiltása.
3. Többoldalas saját lista, státuszszűrés, mentett cím és a régi küldemény címadatainak megőrzése.
4. Admin szerepkörváltás, aktivitásszűrés, saját hozzáférés védelme és letiltott session elutasítása.
5. Súlyvalidáció és CSRF-védelem, részleges mentés nélkül.
6. Valódi adatbázis-constraint megsértése esetén a teljes feladási tranzakció visszagörgetése.
7. Két párhuzamos futárhozzárendelés: egy siker, egy állapotütközés, egyetlen hozzárendelési előzmény.

## Frontend

```sh
cd frontend
npm ci
npm test -- --watch=false
npm run build
```

A teljes frontendtesztcsomag fut; nem csak egy kiválasztott fájllista. A tesztek többek között a session-visszaállítás megvárását, route guardokat, API-cookie-kat, listákat, mentett feladási címet, részleteket/előzményeket, műveleti visszajelzéseket és a többszörös mentés tiltását ellenőrzik. A korábbi, Angular kezdősablonra hivatkozó gyökértesztet valódi navigációs ellenőrzés váltotta fel.

## GitHub Actions

A `.github/workflows/verify.yml` a `KAN-*` és `develop` pushokra, valamint a `develop`/`main` célú pull requestekre fut.

- Backend job: Java 21, külön PostgreSQL 17 service, `./mvnw -Pintegration verify`.
- Frontend job: Node 22, `npm ci`, teljes frontendtesztcsomag, production build.
- Backend riportok: a futás `backend-test-reports` artifactjában, sikertelenség esetén is, ha a riportok létrejöttek.
- Helyi riportok: `target/surefire-reports` és `target/failsafe-reports`.
- A workflow csak olvasási repository-jogosultságot kap; nem deployol és nem módosít fejlesztői/éles adatbázist.

## A tesztek határai

A HTTP-integráció a teljes backendfolyamatot ellenőrzi, de nem valódi böngésző. A frontendtesztek DOM-szimulációban futnak; nem bizonyítják a reszponzivitást, a böngésző tényleges cookie-/CORS-szabályait vagy a teljes UI-folyamatot. Ehhez a [kézi ellenőrzési listát](final-review.md) is végig kell járni.

Az egyidejű hozzárendelési teszt egy konkrét ütközést fed le; nem helyettesít terheléses tesztelést vagy az összes párhuzamos üzleti művelet vizsgálatát.
