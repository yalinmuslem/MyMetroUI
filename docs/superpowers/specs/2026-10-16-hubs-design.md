# Hubs — Design

Sub-project 6. Tanggal: 2026-10-16.

## Tujuan

Hub adalah halaman tematik yang menggabungkan satu topik di satu tempat. Di launcher ini Hub berupa **tile yang bisa disematkan di Start**; tap membuka Hub layar penuh. Tiga Hub: **Photos**, **Pengaturan cepat**, **People**.

## Keputusan pengguna

- Cakupan: ketiga Hub.
- Navigasi: tile Hub di Start (bisa disematkan seperti live tile), bukan halaman pager atau daftar terpisah.

## Asumsi

- Satu tile per Hub, ditambahkan dari bagian "Hubs" di Settings.
- Isi Hub berupa satu kolom yang bisa digulir (bukan panorama geser ke samping).
- Nama dan ikon asli buatan sendiri; tanpa merek/aset Microsoft.

## Di luar cakupan

Hub Music/Games/Videos, panorama dengan parallax, tile Hub yang membalik, mengedit kontak, riwayat panggilan, pencarian kontak.

## Rancangan

### 1. Tile Hub

- `HubKind { PEOPLE, PHOTOS, SETTINGS }` dengan `PREFIX = "hub:"`, `key`, `fromKey` (mirip `LiveKind`). Key: `hub:people`, `hub:photos`, `hub:settings`.
- `TileContent.Hub(kind)` sebagai varian baru. `StartViewModel.contentFor` memetakan key `hub:*`; key tak dikenal menjadi null (dilewati, tidak dihapus).
- Tile Hub bisa dipindah, diubah ukuran, dan dilepas di mode edit. Tidak membalik dan tanpa lencana. Label nama Hub dengan ikon sederhana buatan kita.
- `HubTileAdder`: satu tile per jenis. People meminta `READ_CONTACTS` dulu dan menyematkan hanya bila diberikan; Photos dan Pengaturan cepat langsung menyematkan.
- Settings mendapat bagian "Hubs" dengan satu tombol per Hub (nonaktif dan berlabel "On Start" bila sudah tersemat), pola sama dengan live tile.
- Tap tile membuka `HubScreen` layar penuh (state `openHub: HubKind?` di `HomeScreen`, seperti `showSettings`). Back menutup. Layar sambutan tetap di atasnya.

### 2. Kerangka `HubScreen`

Judul besar tipis (`headlineLarge`), tombol tutup, isi kolom yang bisa digulir. Menelan sentuhan agar tidak tembus ke Start. Isi ditentukan `HubKind`.

### 3. Tiga Hub

- **Photos** — grid dari `PhotoStore.uris()`, tap membuka foto lewat `ACTION_VIEW` (izin baca sudah dipersistenkan oleh picker). Tombol "Add photos" memakai picker yang ada. Thumbnail didekode dengan `BitmapFactory` + `inSampleSize` di latar, tanpa library baru, maksimal 60 foto. Keadaan kosong dan URI yang gagal dimuat ditangani (kotak kosong, tidak crash). Tanpa izin baru.
- **Pengaturan cepat** — tombol Wi-Fi, Bluetooth, mode pesawat, lokasi (`QuickKind`), senter, kecerahan, dan tombol ke Settings (tema). Bagian cepat Action Center diekstrak menjadi komposabel `QuickSettingsSection` yang dipakai bersama oleh Action Center dan Hub ini.
- **People** — `ContactsQuery` (antarmuka: `hasPermission()`, `contacts(): List<Contact>`) dengan implementasi `ContactsContract`. `Contact(id, lookupKey, name)`. Daftar urut nama; tap membuka kartu kontak (`ACTION_VIEW` pada `Contacts.getLookupUri`), ikon telepon memakai `ACTION_DIAL` (tanpa `CALL_PHONE`). Tanpa izin: tombol "Allow contacts access". Kosong: pesan "No contacts". `SecurityException` saat izin dicabut → diperlakukan sebagai tanpa izin. Data hanya di memori.

### 4. Izin dan kebijakan

Satu izin baru: `READ_CONTACTS`, diminta saat menambah tile People atau membuka Hub tanpa izin. Play Store wajib mengungkap penggunaan data kontak; README dicatat.

## Pengujian (JVM, Robolectric; belum ada perangkat)

- `HubKindTest`: round-trip key, key tak dikenal null.
- `StartViewModel`: key `hub:*` dipetakan ke `TileContent.Hub`, key tak dikenal dilewati.
- `HubTileAdderTest`: satu per jenis; People hanya tersemat setelah izin diberikan; Photos/Settings langsung.
- `HubScreenTest`: judul, tombol tutup memanggil callback.
- `PhotosHubTest`: kosong, grid dengan tag per foto, tombol tambah, tap foto membuka.
- `QuickSettingsHubTest` dan tes ActionCenter yang sudah ada tetap lulus setelah ekstraksi.
- `PeopleHubTest` dengan `FakeContactsQuery`: tanpa izin, kosong, berisi (urut), tap kontak, ikon telepon.
- `HomeScreen`: tap tile Hub membuka Hub; Back menutup. Settings: tombol Hub memanggil `onAddHub`, nonaktif bila tersemat.

Tidak teruji pada perangkat nyata: kueri `ContactsContract` pada data kontak sungguhan, dekode thumbnail untuk foto besar/format langka, kartu kontak dan dialer pada aplikasi berbagai merek.
