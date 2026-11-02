# API és jogosultságok

Alapcím helyi futtatáskor: `http://localhost:8080/api`. Az azonosítók egész számok. A felhasználó aktuális email-címét a backend a hitelesített sessionből veszi.

## Végpontok

| Metódus | Útvonal az `/api` után | Hozzáférés | Siker |
|---|---|---|---|
| GET | `/auth/csrf` | Nyilvános | 200, tokenadatok |
| POST | `/auth/login` | Nyilvános, CSRF szükséges | 204 |
| POST | `/auth/logout` | Session + CSRF | 204 |
| GET | `/auth/me` | Aktív bejelentkezett felhasználó | 200, profil és mentett cím |
| POST | `/users` | Nyilvános, CSRF szükséges | 201 |
| GET | `/users/{id}` | Saját profil vagy aktív admin | 200 |
| PATCH | `/users/{id}` | Saját profil vagy aktív admin | 200 |
| GET | `/users/couriers` | Admin | 200, aktív futárok neve és azonosítója |
| GET | `/admin/users` | Admin | 200, lapozott felhasználólista |
| PATCH | `/admin/users/{id}/access` | Admin | 200, módosított felhasználó |
| POST | `/shipments` | Aktív bejelentkezett felhasználó | 201, `id` és `trackingNumber` |
| GET | `/shipments/my` | Aktív bejelentkezett felhasználó | 200, saját küldemények |
| GET | `/shipments/assigned` | Aktív futár | 200, hozzárendelt küldemények |
| GET | `/shipments` | Aktív admin | 200, összes küldemény |
| GET | `/shipments/{id}` | Feladó, hozzárendelt futár vagy admin | 200, részletek |
| GET | `/shipments/tracking/{trackingNumber}` | Feladó, hozzárendelt futár vagy admin | 200, részletek |
| GET | `/shipments/{id}/tracking-events` | Feladó, hozzárendelt futár vagy admin | 200, előzmények |
| PATCH | `/shipments/{id}/courier` | Admin | 204 |
| PATCH | `/shipments/{id}/status` | Admin vagy hozzárendelt futár | 204 |

Minden POST/PATCH kéréshez CSRF-token kell, a regisztrációhoz és loginhoz is. A böngészős kliensnek cookie-val kell küldenie a kéréseket. A Swagger nem kap automatikusan kitöltött CSRF-fejlécet minden műveletnél; az Angular kliens ezt a `csrf.service.ts` segítségével végzi.

A követési szám szerinti lekérdezés sem nyilvános, mert személyes adatokat tartalmaz. A kijelentkezési végpontot a Spring Security kezeli; ismételt, érvényes CSRF-tokenes kijelentkezés nem igényel külön aktív felhasználót.

## Listák

- Küldemények: `page` nullától indul, alapértelmezett `size=10`, `sort=createdAt,desc` és `id,desc`. Opcionális `status` mindhárom listán. A globális adminlistán opcionális `senderId` is használható. Saját és hozzárendelt listán a kliens nem választhat másik tulajdonost/futárt.
- Admin felhasználólista: `page`, `size` (legfeljebb 100), opcionális `role=CUSTOMER|COURIER|ADMIN` és `active=true|false`. Sorrend: `fullName`, majd `id`, növekvően.
- Lapozott válasz fontos mezői: `content`, `number`, `size`, `totalElements`, `totalPages`. Üres eredmény 200 és üres `content`, nem 404.

## Módosító kérések példái

Profil részleges módosítása:

```json
{
  "fullName": "Teszt Felhasználó",
  "phoneNumber": "+36 30 123 4567",
  "address": {
    "country": "Magyarország",
    "postalCode": "1211",
    "city": "Budapest",
    "street": "Példa utca",
    "houseNumber": "1",
    "door": null
  }
}
```

A profil-PATCH nem módosít emailt vagy jelszót. Elhagyott/null mező nem törli a meglévő értéket; a telefon üres stringgel üríthető. `address: null` nem törli a mentett címet. A teljes cím megadásakor az ország, irányítószám, város, utca és házszám kötelező.

Hozzáférés módosítása: `{"role":"COURIER","active":true}`. Mindkét mező kötelező; a felhasználó saját adminjogát nem veheti el és saját fiókját nem inaktiválhatja.

Futárhozzárendelés: `{"courierId":2}`. Csak még hozzá nem rendelt, `REGISTERED` küldeményen és aktív futárral végezhető.

Státuszváltás: `{"status":"PICKED_UP","description":"A csomagot átvettem."}`. A leírás opcionális, legfeljebb 500 karakter. A megengedett átmenetek az [architektúrában](architecture.md) találhatók.

Csomagfeladás:

```json
{
  "senderId": 1,
  "receiverName": "Teszt Címzett",
  "pickupAddress": {
    "country": "Magyarország", "postalCode": "1211", "city": "Budapest",
    "street": "Példa utca", "houseNumber": "1", "door": null
  },
  "deliveryAddress": {
    "country": "Magyarország", "postalCode": "1111", "city": "Budapest",
    "street": "Másik utca", "houseNumber": "2", "door": null
  },
  "parcels": [{"weight":1.25,"description":"Könyvek","fragile":false}]
}
```

A `senderId: 1` csak példa: a kliensnek a bejelentkezett felhasználó valódi azonosítóját kell elküldenie. A backend az eltérő feladót 403-mal elutasítja, admin esetében is. A súly kilogrammban értendő, a tizedeselválasztó JSON-ban pont.

## Egységes hibaválasz – KAN-15

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [{"field":"parcels[0].weight","message":"Enter the parcel weight in kilograms with up to two decimal places."}]
}
```

A `fieldErrors` validáció nélkül üres lista. A válasz nem tartalmaz visszaküldött jelszót, rejected value-t, SQL-t vagy stack trace-t. A backend hibaszövegei angolok; a frontend a státuszkód alapján magyar üzenetet jelenít meg.

| HTTP | Példa `errorCode` | Jelentés |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_REQUEST` | Mezőhiba, hibás JSON, érvénytelen enum/paraméter |
| 401 | `UNAUTHORIZED` | Nincs session, hibás belépés vagy nem elérhető fiók a `/auth/me` hívásánál |
| 403 | `FORBIDDEN` | Hiányzó/hibás CSRF-token, tiltott szerepkör vagy tulajdonosi hozzáférés |
| 404 | `USER_NOT_FOUND`, `SHIPMENT_NOT_FOUND`, `NOT_FOUND` | A kért erőforrás nem található |
| 405 | `METHOD_NOT_ALLOWED` | Az útvonalon nem támogatott HTTP-metódus; `Allow` fejléc is érkezik |
| 409 | `EMAIL_ALREADY_EXISTS`, `SHIPMENT_STATE_CONFLICT`, `USER_ACCESS_CONFLICT`, `DATA_CONFLICT` | Ütköző adat vagy üzleti állapot |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Nem támogatott kérés-content-type |
| 500 | `INTERNAL_ERROR` | Nem várt hiba; részletei csak a szervernaplóba kerülnek |

A 401/403 értelmezésénél a security filterek sorrendje számít: egy CSRF nélküli módosító kérés már a hitelesítés vizsgálata előtt 403-at kaphat. Inaktivált, korábban bejelentkezett fióknál a `/auth/me` 401-et, a tiltott üzleti művelet 403-at adhat.
