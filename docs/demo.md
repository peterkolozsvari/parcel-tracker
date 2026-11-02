# Bemutató – KAN-16

## Előkészítés

- Backend: Java 21, PostgreSQL 17, `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
- Frontend: `cd frontend`, `npm ci`, `npm start`, majd `http://localhost:4200`.
- Legyen három külön regisztrált, kizárólag bemutatóra használt fiók: ügyfél, admin és futár.
- A regisztráció mindig ügyfelet hoz létre. Az első admin/futár szerepkörét a fejlesztői adatbázisban kell beállítani, például az alábbi utasításokkal. A címeket a saját, már regisztrált demófiókok email-címére cseréld. Jelszót ne írj SQL-be vagy Gitbe.

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.test';
UPDATE users SET role = 'COURIER' WHERE email = 'courier@example.test';
```

Ezután jelentkezz be újra. A további szerepkörváltásokhoz már az admin felhasználókezelő oldala használható.

A három szereplőhöz külön böngészőprofilt vagy külön böngészőt használj, hogy a session cookie-k ne írják felül egymást. Két inkognitóablak ugyanabban a böngészőben általában közös munkamenetet használ; erre ne építs.

## 7–10 perces forgatókönyv

| Lépés | Szereplő | Mit mutass meg? | Elvárt eredmény |
|---|---|---|---|
| 1 | Ügyfél | Belépés, profil betöltése, mentett cím beállítása | A cím menthető és frissítés után is megmarad |
| 2 | Ügyfél | Csomagfeladás, mentett felvételi cím, több csomag | Követési szám jelenik meg, a lista tartalmazza az új küldeményt |
| 3 | Ügyfél | Küldemény részletei és előzményei | Kezdeti állapot: regisztrált |
| 4 | Admin | Küldeménykezelés, futár hozzárendelése | A státusz futárhoz rendeltre változik, új előzmény keletkezik |
| 5 | Futár | Futárfeladatok, felvétel, szállítás, kézbesítés | Csak a hozzárendelt küldemény kezelhető |
| 6 | Ügyfél | Részletek újratöltése | Kézbesítve státusz és rendezett állapottörténet |
| 7 | Admin | Felhasználókezelés és szűrés | Másik demófiók szerepköre/aktivitása menthető; saját hozzáférés nem vonható vissza |

## Rövid negatív próbák

- Hibás jelszó: érthető üzenet, nincs belépés.
- Másik ügyfél küldeményének azonosítója az URL-ben: 403, nincs adatkiadás.
- 1.234 kg súly: validációs hiba, nincs létrehozott küldemény.
- Kijelentkezés után védett oldal megnyitása: bejelentkezésre irányít.
- Érvénytelen státuszváltás közvetlen API-kéréssel: 409, állapot és előzmények változatlanok.

## Automatizált bemutató

```sh
./mvnw test
docker compose -f compose.test.yaml up -d --wait
./mvnw -Pintegration verify
docker compose -f compose.test.yaml down
cd frontend
npm test -- --watch=false
npm run build
```

Windows alatt a Maven-parancsokban `mvnw.cmd` használandó. Az integrációs teszt külön `parcel_tracker_test` adatbázist és futásonként új sémát használ; saját tesztadatait hozza létre. Nincs szüksége a kézi demófiókokra.
