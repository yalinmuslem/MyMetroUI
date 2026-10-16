# Layar Sambutan (Lock Screen) — Design

Sub-project 5. Tanggal: 2026-10-16.

## Tujuan

Android tidak mengizinkan launcher mengganti keyguard sistem yang aman. Maka launcher menyediakan **layar sambutan di dalam launcher**: halaman pertama bergaya Metro berisi jam besar, tanggal, dan ringkasan notifikasi, yang disingkap dengan geser ke atas untuk mencapai Start. Layar ini hanya tampilan, **bukan pengaman**; PIN/biometrik tetap milik sistem.

## Keputusan pengguna

- Pendekatan: layar sambutan di dalam launcher (bukan overlay di atas keyguard, ditolak karena berisiko dan tidak andal).
- Pemicu: tampil setelah layar mati lalu menyala ketika launcher di depan. Tombol Home tidak memicunya.
- Sakelar aktif/nonaktif di Settings.
- Tanpa izin berbahaya baru; aman untuk kebijakan Play.

## Asumsi

- Kalender dan notifikasi memakai izin/akses yang sudah ada; tidak ada permintaan izin baru dari layar ini.
- Default sakelar: aktif.

## Di luar cakupan

Keamanan sungguhan, tampil di atas keyguard sistem, foto/wallpaper kustom sebagai latar, widget atau kontrol musik, teks kustom, pemicu dari tombol Home.

## Rancangan

### 1. Pemicu — `WelcomeGate` (logika murni)

Urutan broadcast `ACTION_SCREEN_OFF` dan `onPause` tidak pasti, jadi dua sinyal menandai "layar mati":
- `ACTION_SCREEN_OFF` diterima saat launcher sedang resumed.
- `onPause` terjadi saat `PowerManager.isInteractive` false.

Pada `HomeActivity.onResume`: bila ada tanda tertunda dan sakelar aktif, layar sambutan tampil dan tanda dihapus. Tombol Home dan mulai dingin tidak menandai. Sakelar mati menahan tanda.

`ACTION_SCREEN_OFF` hanya bisa lewat receiver dinamis, didaftarkan di `LauncherApp`. Status tampil disimpan di `HomeEvents` (ViewModel) agar tahan rotasi.

### 2. Tampilan — `WelcomeScreen`

- Jam dan tanggal besar (memakai ulang `clockFace`).
- Baris kalender dari sumber live tile (acara berikutnya atau "No events today"), hanya bila izin kalender sudah diberikan.
- Ringkasan notifikasi, hanya bila akses listener aktif: jumlah total dan sampai 3 judul terbaru (`WelcomeSummary`, logika murni). Bila `KeyguardManager.isDeviceLocked` true, hanya jumlah yang tampil.
- Latar: gradien backdrop yang sama dengan Start.
- Geser ke atas melewati ambang 25% atau fling cepat menyingkap Start (`PanelDrag.settle` dipakai ulang). Back juga menutup. Durasi animasi dari `MotionSpec`.
- Selama tampil, Action Center dan gestur tepi atas dinonaktifkan.

### 3. Pengaturan

Satu sakelar "Show welcome screen after screen off" di Settings, disimpan di `WelcomePrefs` (SharedPreferences boolean, default true), terpisah dari `ThemeSettings`.

### 4. Penanganan error

- `isInteractive`/receiver gagal: tidak menampilkan layar (gagal-aman, tidak mengunci pengguna di luar Start).
- Kalender/notifikasi tidak tersedia: bagian itu disembunyikan, jam dan tanggal tetap tampil.

## Pengujian (JVM, Robolectric; belum ada perangkat)

- `WelcomeGateTest`: layar mati saat resumed menandai; layar mati saat paused dengan `isInteractive` false menandai; Home tanpa layar mati tidak menandai; tanda dihapus setelah dipakai; sakelar mati menahan.
- `WelcomeSummaryTest`: jumlah dan 3 judul terbaru; terkunci hanya jumlah; tanpa akses tidak ada bagian notifikasi.
- `WelcomeScreenTest`: jam dan tanggal; baris kalender hanya untuk wajah teks; drag naik memanggil dismiss; drag kecil tidak.
- `HomeScreen`: layar tampil; Back menutup; Action Center nonaktif selama tampil.
- `WelcomePrefsTest` dan tes sakelar di Settings.

Tidak teruji pada perangkat nyata: urutan broadcast layar mati vs `onPause` pada berbagai OEM, perilaku setelah keyguard sistem dibuka.
