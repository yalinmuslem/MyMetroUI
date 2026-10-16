# Action Center (Sub-proyek 4) — Design

Tanggal: 2026-10-16

Melanjutkan [Core Launcher](2026-10-15-core-launcher-design.md), [Tema dan Aksen](2026-10-16-theme-accent-design.md), dan [Live Tiles](2026-10-16-live-tiles-design.md). Nama/aset orisinal; tidak ada aset Microsoft.

## Tujuan

Panel di dalam launcher yang menampilkan notifikasi aktif, tombol cepat, senter, dan kecerahan, plus badge jumlah notifikasi di tile app (ditunda dari sub-proyek 3). Launcher tidak bisa mengganti notification shade sistem; panel ini adalah UI sendiri.

## Keputusan yang disepakati

- Cakupan: daftar notifikasi, badge di tile app, tombol cepat (shortcut ke pengaturan), senter, kecerahan.
- Pembuka: geser turun dari strip tipis di tepi atas Start dan app list; tombol di Settings sebagai cadangan.
- **Di luar cakupan:** aksi/balas notifikasi, grup yang bisa dibuka, mute per app, toggle Do Not Disturb, riwayat notifikasi, lock screen (sub-proyek 5), toggle langsung Wi-Fi/Bluetooth/mode pesawat/lokasi (tidak diizinkan Android modern).

## Arsitektur

```
data/notif/NotifItem.kt              data class NotifItem
data/notif/NotificationRepository.kt singleton proses: StateFlow<List<NotifItem>>, StateFlow<Boolean> connected
data/notif/NotificationListener.kt   NotificationListenerService (manifest, BIND_NOTIFICATION_LISTENER_SERVICE)
data/notif/NotificationAccess.kt     isNotificationAccessGranted(context), settingsIntent()
data/notif/BadgeCounts.kt            fun badgeCounts(items): Map<String, Int>; fun badgeFor(app, counts): Int
data/quick/QuickSettings.kt          enum QuickKind; fun quickIntent(kind, sdk): Intent
data/quick/Torch.kt                  interface Torch + CameraManager implementation
data/quick/Brightness.kt             interface Brightness (read/write/canWrite) + fractionToSystem/systemToFraction
ui/actioncenter/ActionCenter.kt      panel (quick row, brightness, notification list, empty/permission states)
ui/actioncenter/PanelDrag.kt         logika murni ambang buka/tutup
ui/start/TileView.kt                 badge (modify)
ui/settings/SettingsScreen.kt        tombol "Open Action Center" (modify)
```

## Notifikasi

`NotifItem(key, packageName, userId, title, text, whenMs, clearable, visibility, contentIntent: PendingIntent?, autoCancel)`.

- Listener memetakan `StatusBarNotification` → `NotifItem` dan memperbarui repository pada `onNotificationPosted/Removed/onListenerConnected/onListenerDisconnected`. Notifikasi app kita sendiri dan ringkasan grup (`FLAG_GROUP_SUMMARY`) dilewati. Urut terbaru di atas.
- Ongoing/tidak bisa dihapus (`!clearable`) tampil tanpa tombol hapus dan tidak ikut "Clear all".
- Hapus satu → `cancelNotification(key)`; Clear all → `cancelNotification` untuk setiap item yang clearable. Ketuk item → kirim `contentIntent` (try/catch `CanceledException`), lalu `cancelNotification(key)` bila `autoCancel`, lalu tutup panel.
- Privasi: data hanya di memori, tidak disimpan/dikirim. Saat perangkat terkunci (`KeyguardManager.isDeviceLocked`) item dengan `visibility` PRIVATE/SECRET hanya menampilkan nama app.
- Tanpa akses notifikasi → repository kosong, `connected = false`; panel menampilkan ajakan yang membuka `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.

## Badge

- `badgeCounts(items)`: `Map<"package#userId", Int>` menghitung item non-ongoing. `badgeFor(app, counts)` = hitungan untuk `app.component.packageName` + `app.userId`.
- Badge: lingkaran di pojok kanan atas tile app; teks angka, `99+` bila lebih dari 99. Tile SMALL: versi ringkas (`9+`). Tidak ada badge untuk tile live, atau bila hitungan 0.

## Panel

- Susunan atas→bawah: baris tombol cepat, slider kecerahan, daftar notifikasi (atau keadaan kosong / ajakan izin), tombol "Clear all" bila ada yang bisa dihapus.
- Pembuka: strip `TopEdgeHeight` (24dp di bawah status bar inset) menangkap drag vertikal; panel mengikuti jari. `PanelDrag.shouldOpen(offsetPx, panelHeightPx, velocity)`: buka bila offset ≥ 25% tinggi panel atau fling ke bawah cukup cepat, selain itu kembali tertutup. Tutup: geser naik di panel, ketuk scrim, Back (Back menutup panel lebih dulu daripada Settings/edit/pager).
- Tidak terbuka saat turnstile berjalan atau mode edit aktif. Konstanta animasi baru di `MotionSpec`: `ActionCenterDurationMs`, `ActionCenterOpenFraction = 0.25f`.

## Tombol cepat, senter, kecerahan

| Kontrol | Perilaku | Izin |
|---|---|---|
| Wi-Fi | API ≥ 29: `Settings.Panel.ACTION_INTERNET_CONNECTIVITY`; di bawahnya `ACTION_WIFI_SETTINGS` | tidak ada |
| Bluetooth | `Settings.ACTION_BLUETOOTH_SETTINGS` | tidak ada |
| Mode pesawat | `Settings.ACTION_AIRPLANE_MODE_SETTINGS` | tidak ada |
| Lokasi | `Settings.ACTION_LOCATION_SOURCE_SETTINGS` | tidak ada |
| Senter | toggle `CameraManager.setTorchMode` pada kamera berflash; status lewat `TorchCallback`; nonaktif bila tidak ada flash atau kamera sedang dipakai | tidak ada |
| Kecerahan | slider 0..1 ↔ `Settings.System.SCREEN_BRIGHTNESS` (0..255, minimum 1); menulis mematikan mode auto; tanpa izin, menyentuh slider membuka `Settings.ACTION_MANAGE_WRITE_SETTINGS` | `WRITE_SETTINGS` (khusus) |

Intent yang gagal dibuka (`ActivityNotFoundException`) diabaikan tanpa crash.

## Error handling

- Listener terputus/diaktifkan ulang → repository mengosongkan lalu mengisi ulang dari `getActiveNotifications()` saat terhubung.
- `contentIntent` null atau dibatalkan → ketuk hanya menutup panel.
- `setTorchMode` melempar `CameraAccessException`/`IllegalArgumentException` → senter kembali mati, tidak crash.
- `WRITE_SETTINGS` dicabut saat panel terbuka → slider kembali ke mode "minta izin".

## Pengujian (JVM, Robolectric)

1. `NotificationMappingTest`: `StatusBarNotification` → `NotifItem` (judul/teks/clearable/autoCancel); app sendiri dan ringkasan grup dilewati; urutan terbaru dulu.
2. `BadgeCountsTest`: hitung per package+user (profil kerja terpisah), ongoing tidak dihitung, cap `99+`, 0 → tanpa badge.
3. `PanelDragTest`: ambang 25%, fling cepat membuka, drag kecil kembali tertutup, offset negatif/berlebih di-clamp.
4. `QuickSettingsTest`: intent per jenis, termasuk cabang API 28 vs 29 untuk Wi-Fi.
5. `BrightnessTest`: konversi pecahan ↔ 0..255, minimum 1, nilai di luar rentang di-clamp.
6. `ActionCenterTest` (compose): daftar tampil; hapus memanggil callback; Clear all hanya item clearable; ongoing tanpa tombol hapus; keadaan kosong; ajakan izin saat `connected = false`; senter nonaktif tanpa flash; slider tanpa izin memicu permintaan izin; Back menutup panel lebih dulu.
7. `TileViewTest`: badge tampil dengan angka, `99+`, tidak tampil di 0 dan tile live.
8. `SettingsScreenTest` (diperluas): tombol buka Action Center.
9. Manual di perangkat: aktivasi akses notifikasi, notifikasi nyata, gestur tepi atas, senter, kecerahan, intent pengaturan per vendor.
