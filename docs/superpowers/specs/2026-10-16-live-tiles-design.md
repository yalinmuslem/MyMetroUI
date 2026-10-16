# Live Tiles (Sub-proyek 3) — Design

Tanggal: 2026-10-16

Melanjutkan [Core Launcher](2026-10-15-core-launcher-design.md) dan [Tema dan Aksen](2026-10-16-theme-accent-design.md). Nama/aset orisinal; tidak ada aset Microsoft.

## Tujuan

Start screen punya tile mandiri yang "hidup": berputar (flip) antara muka depan (ikon + label) dan muka belakang berisi konten live. Konten hanya dari sumber yang kita kendalikan; Android tidak mengizinkan membaca konten app lain.

## Keputusan yang disepakati

- Sumber: tile bawaan (Jam, Kalender, Foto) + animasi flip.
- Bentuk: tile mandiri (tidak menempel ke tile app), ditambahkan dari layar Settings.
- Badge notifikasi **dipindah ke sub-proyek 4** (butuh `NotificationListenerService`).
- Foto memakai gambar yang dipilih pengguna lewat picker, bukan `READ_MEDIA_IMAGES` (dibatasi kebijakan Play Store untuk app non-galeri).
- **Di luar cakupan:** badge notifikasi, widget Android (AppWidgetHost), cuaca, pengaturan flip per pengguna, live tile untuk app pihak ketiga.

## Arsitektur

```
domain/Tile.kt                 tidak berubah (key string, size, col, row)
data/live/LiveKind.kt          enum LiveKind { CLOCK, CALENDAR, PHOTOS }; key "live:<kind>"
data/live/LiveFace.kt          sealed: Text(title, lines), Photo(uri), NeedsPermission
data/live/LiveTileSource.kt    interface: fun face(kind): Flow<LiveFace>
data/live/{Clock,Calendar,Photos}Source.kt
data/live/PhotoStore.kt        URI foto terpilih di SharedPreferences
ui/TileUi                      TileUi(tile, content: TileContent); TileContent = App(AppInfo) | Live(LiveKind)
ui/motion/FlipSchedule.kt      logika murni kapan tiap tile flip
ui/start/LiveTileView.kt       depan/belakang + rotasi
ui/settings/SettingsScreen     bagian "Live tiles"
```

- `StartViewModel` menggabungkan `tiles` dengan `apps`: key `live:*` menjadi `TileContent.Live`, key lain dicocokkan ke `AppInfo` seperti sekarang. Logika "app hilang → hapus tile" hanya untuk tile app. `StartViewModel` memegang `LiveTileSource` dan menyediakan `face(kind)` ke UI.
- Tile live selalu memakai `Tile` yang sama (size, col, row) sehingga `TileLayout`, drag, resize, dan unpin bekerja tanpa perubahan.

## Sumber live

| Jenis | Konten belakang | Izin | Refresh | Tap membuka |
|---|---|---|---|---|
| CLOCK | jam (HH:mm) + tanggal | tidak ada | tiap menit (selaras detik 0) | `AlarmClock.ACTION_SHOW_ALARMS` |
| CALENDAR | acara berikutnya hari ini (judul + jam); "tidak ada acara" bila kosong | `READ_CALENDAR`, diminta saat tile ditambahkan | tiap menit + `ContentObserver` | `content://com.android.calendar/time` (ACTION_VIEW) |
| PHOTOS | gambar terpilih bergantian tiap flip | tidak ada (picker + URI persisten) | tiap flip | `ACTION_VIEW` galeri default, fallback app picker |

- Tanpa izin kalender (belum diberi atau dicabut) → `LiveFace.NeedsPermission`; tap pada tile meminta izin, bukan membuka kalender.
- Foto: URI diambil dengan `takePersistableUriPermission`; URI yang gagal dibuka (file dihapus/izin hilang) dilewati; daftar kosong → muka belakang menampilkan "pilih foto". Bitmap di-decode terkecil (downsample) sesuai ukuran tile.
- Kegagalan lain di sumber (provider null, exception) → muka belakang tidak ditampilkan, tile tetap statis; tidak crash.

## Flip

- Hanya tile MEDIUM dan WIDE yang flip; SMALL tetap statis (ikon saja).
- Rotasi `rotationX` 0→180 lewat `graphicsLayer` (`cameraDistance`), dua sisi dibalik agar tidak terbalik.
- `FlipSchedule`: tile flip tiap `FlipIntervalMs`, diberi offset `(tile.id % N) * FlipStaggerMs`; satu flip = `FlipDurationMs` depan→belakang, tahan, lalu kembali. Fungsi murni: `nextFlipAt(tile, nowMs): Long`.
- Flip berhenti saat: mode edit, halaman Start tidak terlihat, turnstile berjalan, atau layar mati/aktivitas tidak STARTED. Tile tidak pernah berhenti di tengah rotasi saat kondisi berubah (selesaikan atau snap ke depan).
- Konstanta baru di `MotionSpec`: `FlipDurationMs`, `FlipIntervalMs`, `FlipHoldMs`, `FlipStaggerMs`.

## Menambah dan menghapus

- Settings: bagian "Live tiles" dengan tombol Jam, Kalender, Foto. Tiap jenis hanya satu; bila sudah dipin, tombol tampil nonaktif ("sudah di Start").
- Kalender: minta `READ_CALENDAR` dulu; bila ditolak, tile tidak ditambahkan.
- Foto: buka picker; bila batal atau tanpa pilihan, tile tidak ditambahkan.
- Hapus lewat unpin di mode edit (sama seperti tile app). Menghapus tile Foto tidak menghapus daftar foto terpilih (dipakai bila ditambahkan lagi).

## Error handling

- Izin kalender dicabut saat tile ada → `NeedsPermission`, tidak crash dan tidak dihapus.
- Foto terpilih tidak lagi dapat dibaca → dilewati; semua gagal → "pilih foto".
- Data Room lama berisi key `live:*` tak dikenal (versi mendatang) → tile dilewati, tidak dihapus.
- Perubahan konfigurasi: tidak ada state live di Activity; flow dikumpulkan dari ViewModel/singleton proses.

## Pengujian (JVM, Robolectric)

1. `FlipScheduleTest`: offset per tile, interval, SMALL tidak pernah dijadwalkan, nilai monoton.
2. `ClockSourceTest`: format wajah (jam/tanggal) dari jam fake; tick berikutnya selaras awal menit.
3. `CalendarSourceTest`: pemetaan hasil query ke `LiveFace` (acara berikutnya, kosong, tanpa izin) memakai penyedia fake.
4. `PhotoStoreTest`: simpan/baca URI, urutan, URI korup dibuang.
5. `StartViewModelTest` (diperluas): key `live:*` menjadi `Live`, tidak dihapus saat app list berubah; key tak dikenal dilewati.
6. `LiveTileViewTest`: depan menampilkan label; belakang menampilkan konten; `NeedsPermission` menampilkan ajakan; SMALL tanpa flip.
7. `EditModeTest` (diperluas): tile live bisa digeser, di-resize, dan di-unpin; flip berhenti saat edit.
8. `SettingsScreenTest` (diperluas): tombol tambah per jenis, nonaktif bila sudah dipin, permintaan izin/picker dipanggil via callback.
9. Manual di perangkat: tampilan dan timing flip, izin kalender nyata, picker foto, tap membuka app yang benar.
