# Parcel Tracker frontend

Angular 21.2, standalone komponensek, signals, Reactive Forms, RxJS, Bootstrap és Ionicons.

```sh
npm ci
npm start
```

A felület `http://localhost:4200` alatt érhető el. A backend külön indul a `http://localhost:8080` címen. Az API-címek a `src/app/config/api.config.ts` fájlban találhatók.

```sh
npm test -- --watch=false
npm run build
```

A tesztfuttató Vitest. A production build kimenete: `dist/frontend/browser`. Az Angular kliensútvonalak miatt tárhelyen a nem statikus fájlra mutató frontendkéréseket az `index.html` fájlra kell visszairányítani.

Az `auth.service.ts` kezeli az aktuális felhasználó állapotát és a session visszaállítását. A `csrf.service.ts` tokent kér a módosító hívásokhoz. Az `authGuard` belépést, a `roleGuard` szerepkört ellenőriz a navigációhoz; a backend minden hozzáférést önállóan is ellenőriz.

A frontendtesztek nem helyettesítik a valódi böngészős és mobilos próbát. Teljes indítási, API- és tesztelési dokumentáció: [projekt README](../README.md).

A HU/EN kapcsoló újratöltés nélkül vált nyelvet. A `language.service.ts` a választást localStorage-ban tárolja és frissíti a dokumentum `lang` attribútumát; a fordítások a `src/app/i18n/translations.ts` fájlban találhatók. A `translate` pipe kezeli a felületi címkéket és üzeneteket, a felhasználói adatok változatlanok maradnak.
