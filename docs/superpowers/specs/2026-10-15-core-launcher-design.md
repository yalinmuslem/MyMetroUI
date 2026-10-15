# Core Launcher (Sub-proyek 1) — Design

Tanggal: 2026-10-15

## Tujuan

Launcher Android bergaya Metro/Windows Phone 8.1 untuk dirilis di Play Store. Semua fungsi Android tetap ada; hanya UI/UX yang berubah. Nama dan aset harus orisinal (tanpa "Windows Phone", logo, ikon, atau font Microsoft). Font: Open Sans atau Selawik. Ikon digambar sendiri.

## Keputusan yang disepakati

- Stack: Kotlin + Jetpack Compose, `minSdk` 26.
- Cakupan akhir: replika penuh, dipecah menjadi 6 sub-proyek (masing-masing punya siklus spec → plan → implementasi):
  1. **Core launcher** (dokumen ini)
  2. Tema dan aksen
  3. Live tiles
  4. Action Center (`NotificationListenerService`)
  5. Lock screen (berisiko: batasan Android dan kebijakan Play Store)
  6. Hubs (opsional, terakhir)
- Pendekatan: Compose murni, satu Activity, grid tile kustom.

## Cakupan sub-proyek 1

Start screen dengan tile (small/medium/wide, pin/unpin, drag reorder, resize), app list alfabetis dengan jump list, animasi Metro (tilt, turnstile, parallax, mode edit).

**Di luar cakupan:** live tiles, tema/aksen kustom (satu aksen default), lock screen, Action Center, Hubs, pencarian, widget.

## Arsitektur

Satu modul `app`, package dipisah per tanggung jawab:

```
com.mymetroui.launcher/
├─ HomeActivity            Activity HOME, singleTask, edge-to-edge
├─ ui/
│  ├─ home/                HomeScreen: HorizontalPager (Start | App list)
│  ├─ start/               StartGrid, TileView, mode edit
│  ├─ applist/             AppListScreen, jump list
│  ├─ motion/              tilt, turnstile, parallax; MotionSpec
│  └─ theme/               warna, tipografi, ukuran grid
├─ data/
│  ├─ AppRepository        LauncherApps + event install/uninstall (Flow)
│  ├─ TileRepository       Room
│  └─ db/                  TileEntity, TileDao
└─ domain/
   └─ TileLayout           logika murni Kotlin
```

Alur data: repository mengeluarkan `Flow`; ViewModel menggabungkannya menjadi `StartState` dan `AppListState`; UI menampilkan state dan mengirim intent (`pin`, `move`, `resize`, `launch`).

`HomeActivity` terdaftar dengan intent filter `HOME` + `DEFAULT`. Tombol home kembali ke Start; back dari app list kembali ke Start.

## Model data

Grid 4 kolom satuan, tinggi tak terbatas, scroll vertikal.

| Ukuran | Kolom × Baris |
|---|---|
| SMALL | 1×1 |
| MEDIUM | 2×2 |
| WIDE | 4×2 |

Tile small dikelompokkan dalam blok 2×2 (4 tile kecil), seperti WP8.1.

`TileEntity`:

| Field | Tipe | Keterangan |
|---|---|---|
| `id` | Long (PK) | auto |
| `componentKey` | String | `package/activity` + `userId`, unik |
| `size` | enum | SMALL, MEDIUM, WIDE |
| `col` | Int | 0–3 |
| `row` | Int | ≥ 0 |

Urutan tampil diturunkan dari `(row, col)`. Hanya tile Start yang disimpan; daftar app selalu diambil dari `LauncherApps`.

### TileLayout (Kotlin murni, tanpa dependensi Android)

- `place(tiles, new)`: posisi kosong pertama.
- `move(tiles, id, col, row)`: tile yang tertimpa terdorong ke bawah (reflow).
- `resize(tiles, id, size)`: membesar mendorong tetangga; mengecil meninggalkan celah.
- `compact(tiles)`: menutup baris kosong; dipanggil setelah unpin dan setelah mode edit.
- Invarian: tidak ada tumpang tindih, `col + width ≤ 4`, posisi unik.

### Kasus tepi

- App di-uninstall atau komponen hilang setelah update: tile dihapus, lalu `compact`.
- Profil kerja: `componentKey` memuat `userId`.
- Pertama kali dijalankan: Start diisi tile default (telepon, pesan, browser, kamera, galeri) sesuai app terpasang.

## Gesture dan animasi

Dibangun dengan `graphicsLayer` (rotasi 3D, `cameraDistance`, `transformOrigin`) dan spring/tween Compose. Semua durasi, easing, dan sudut berada di satu file `MotionSpec` agar mudah di-tuning.

1. **Tilt:** tile miring ke titik sentuh (rotasi X/Y beberapa derajat, scale turun ~5–6%); spring kembali saat dilepas. Dibatalkan jika jari mulai scroll.
2. **Turnstile:** saat tile di-tap, semua tile berputar keluar dengan engsel di sisi kiri, stagger per baris/kolom, lalu app diluncurkan. Saat kembali ke Start, urutan dibalik. Hanya sisi launcher; transisi di dalam app lain tidak berubah.
3. **Navigasi Start ↔ App list:** geser horizontal atau tombol panah bulat kanan bawah; parallax antara tile dan latar. Scroll vertikal dengan fling dan overscroll elastis. Tile fly-in saat Start dibuka dari tombol home.
4. **Mode edit (tekan lama):** tile terangkat; tile lain mengecil dan meredup; tombol unpin (kanan atas) dan resize (kanan bawah, siklus S→M→W→S); drag dengan reflow beranimasi; keluar via tap area kosong atau back, lalu `compact`.
5. **App list:** header huruf; tap header membuka jump list (huruf tanpa app diredupkan); tekan lama app → menu "pin to Start" dan "uninstall".

**Keterbatasan:** nilai persis WP8.1 tidak terdokumentasi publik; ditiru dari ingatan dan referensi video, disetel lewat `MotionSpec`.

## Error handling

- Peluncuran app gagal: try/catch di sekitar `startActivity`, hapus tile, `compact`.
- Database gagal dibaca: fallback ke tata letak default.
- Belum jadi launcher default: layar pandu memakai `RoleManager` (API 29+) atau intent pengaturan (API 26–28).
- Perubahan konfigurasi: state di ViewModel dan Room; mode edit tidak perlu bertahan setelah rotasi.
- Event app: `LauncherApps.Callback` → `Flow`.

## Pengujian

1. Unit test `TileLayout` (JVM): invarian, `move`/`resize`/`compact`, serta uji properti acak (operasi acak dari tata letak valid tetap valid).
2. Test `TileRepository` dengan Room in-memory.
3. Compose UI test: pin dari app list muncul di Start; unpin menghilangkannya; resize mengubah ukuran.
4. Animasi: pemeriksaan manual di emulator/perangkat; tuning lewat `MotionSpec`.
