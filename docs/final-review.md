# Végső ellenőrzés – KAN-22

## Automatizált ellenőrzések

Ellenőrzött KAN-16 commit: `ab850434dda0e0d5ddde2702e5b18add6702560d`. A [teljes GitHub Actions-futás](https://github.com/peterkolozsvari/parcel-tracker/actions/runs/41492371988) sikeres. A későbbi kódmódosításoknál mindig az adott commithoz tartozó Actions-eredmény a mérvadó. A KAN-16 tesztcsomag 198 gyors backendtesztet, 7 PostgreSQL-integrációs tesztesetet és 68 frontendtesztet tartalmaz. A frontend production buildje is része az ellenőrzésnek.

A kezdeti integrációs próbában a migrációszámlálás a Flyway séma-metaadatsorát is beleszámolta; az ellenőrzés most kizárólag a verziózott migrációkat számolja. Az adatbázist ez a javítás nem módosítja.

## Kézi elfogadási lista

Ezeket a lépéseket a tényleges helyi alkalmazásban kell kipróbálni. A dokumentum elkészítése nem jelenti a kipipálásukat.

- [ ] Új adatbázissal tiszta indítás és automatikus migrációk, meglévő adatbázissal változatlan adatok.
- [ ] Regisztráció, jó/hibás belépés, oldalfrissítés után megmaradó session, kijelentkezés.
- [ ] Profil betöltése, cím nélküli régi fiók, név/telefon/cím mentése és újratöltése.
- [ ] Mentett felvételi cím alapértelmezése, eltérő cím szerkesztése és visszaállítása.
- [ ] Több csomag feladása, dinamikus hozzáadás/törlés, kötelező mezők és súlyvalidáció.
- [ ] Sikeroldal, követési szám másolása, lista és részletek megnyitása.
- [ ] Üres lista, több oldal, szűrés és időrendi rendezés.
- [ ] Admin futárhozzárendelés és felhasználókezelés; saját adminhozzáférés védelme.
- [ ] Futár csak saját küldeményeit látja és csak engedett státuszátmenetet végezhet.
- [ ] Ügyfél saját küldeményeinek előzményeit látja; más azonosítóval nincs adatkiadás.
- [ ] Hálózati hibánál érthető üzenet, újrapróbálás; gyors dupla kattintás nem indít dupla mentést.
- [ ] Billentyűzetes navigáció, menünyitás/-zárás, fókusz, látható mezőhibák.
- [ ] Például 360×800, 390×844, 768×1024 és 1440×900 CSS-pixeles nézetben használható elrendezés. Ezek tesztméretek, nem kötelező képernyőszabványok.
- [ ] Magyar ékezetek, dátumok és a böngésző helyi időzónájának megjelenítése.

## Ismert korlátok és további munka

| Terület | Jelenlegi korlát / következő feladat |
|---|---|
| Buildméret | Az ellenőrzött production build 500 kB-os kezdeti figyelmeztetési keretét kb. 40 kB-bal lépi túl. A login- és profil-CSS szintén figyelmeztetési keret fölött van. A build sikeres; a limiteket nem emeltük a figyelmeztetés elrejtéséért. |
| Lapozási szerződés | A backend közvetlen `Page`-választ ad. A jelenlegi frontend ezt használja, de a Spring Data figyelmeztet a JSON-formátum hosszú távú stabilitására. Saját lapozott DTO-ra váltás külön kompatibilitási feladat. |
| Képek | A `logo.png`, `home_pic.png` és `pagenotfound.png` nagy raszterképek. Méretezés/tömörítés és hálózati ellenőrzés még javíthat a betöltésen. |
| Nyelvválasztás | A HU/EN kapcsoló futás közben váltja a felület szövegeit, a választást a böngésző tárolja. A felhasználói nevek, címadatok és egyedi leírások eredeti nyelvükön maradnak. |
| Profil | Email- és jelszóváltoztatás, jelszó-visszaállítás, valamint mentett cím teljes törlése nincs elkészítve. |
| Futárkezelés | Meglévő küldeményen futárcsere még nincs. Futár inaktiválása vagy szerepkörének elvétele előtt ellenőrizni kell a folyamatban lévő küldeményeit; az admin a megengedett státuszváltásokat továbbra is elvégezheti. |
| Első admin | Regisztrációból mindig ügyfél lesz; első adminhoz fejlesztői adatbázis-beállítás kell. Nincs alapértelmezett adminjelszó. |
| Nyilvános követés | A követési szám alapján elérhető részletek hitelesítést és jogosultságot igényelnek. Személyes adatokat nem tartalmazó nyilvános követőoldal külön funkció lehet. |
| Email | Feladási/megerősítő email nincs bekötve. |
| Biztonsági üzemeltetés | Éles konfigurációhoz HTTPS, környezetből kezelt jelszavak, megfelelő cookie-/session-beállítások, éles CORS-originlista és a Swagger elérhetőségének eldöntése szükséges. Belépési próbálkozások korlátozása külön feladat. |
| Session | Újraindítás után az alapértelmezett memóriabeli sessionök elvesznek. Több backendpéldányhoz közös sessiontár vagy megfelelő üzemeltetési terv kell. |
| Naplózás | A feladási controller jelenleg a teljes feladási DTO-t naplózza. Éles használat előtt személyes adatokat nem tartalmazó eseménynaplózásra kell váltani. |
| Terhelés és E2E | Nincs teljes böngészős E2E- vagy terheléses tesztcsomag. |
| Korábbi források | A repóban korábbról maradt oktatási fejlécű források jogállását nyilvános portfólió/megosztás előtt tisztázni kell. Az új hibakezelő önálló forrás, a korábbi fájlok újraközlése nélkül készült. |

Ezek nem automatikusan új Jira-ticketek: prioritásukat a mentorral, a bemutató és a tervezett használat alapján érdemes eldönteni.

## Ágak és integráció

A KAN-15 a kész KAN-11-re, a KAN-16 a KAN-15-re, a KAN-22 a KAN-16-ra épül. A távoli `develop` a munka elején még a korábbi állapotot tartalmazta. A branchek feltöltése nem jelenti a developba merge-elésüket.

Integráláskor a függőségi sorrend: KAN-10 → KAN-11 → KAN-15 → KAN-16 → KAN-22. A végső branch az előzőek teljes kódját tartalmazza; ha összevont PR-rel kerül developba, ezt a PR leírásában egyértelműen jelezni kell. A Jira-lezárás előtt a végleges developon is fusson az automatikus és a kézi ellenőrzés.
