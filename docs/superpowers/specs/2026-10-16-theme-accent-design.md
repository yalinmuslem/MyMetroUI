# Tema dan Aksen (Sub-proyek 2) — Design

Tanggal: 2026-10-16

Melanjutkan [Core Launcher](2026-10-15-core-launcher-design.md). Nama/aset tetap orisinal; tidak ada aset Microsoft.

## Tujuan

Pengguna memilih warna aksen dari palet dan mode gelap/terang. Pilihan tersimpan lokal dan berlaku ke seluruh launcher (tile, header, tombol, menu, prompt).

## Keputusan yang disepakati

- Cakupan: aksen dari palet + mode gelap/terang.
- Entry point: satu baris "Settings" di akhir app list, membuka layar Tema.
- **Di luar cakupan:** warna kustom bebas, wallpaper, tile transparan, warna per tile, mengikuti tema sistem.

## Arsitektur

```
ui/theme/Accents.kt        daftar AccentColor(id, name, Color), DefaultAccent
ui/theme/Theme.kt          MyMetroUITheme(settings) memetakan ThemeSettings -> ColorScheme
data/ThemeSettings.kt      data class ThemeSettings(accentId, mode); enum ThemeMode { DARK, LIGHT }
data/ThemeRepository.kt    StateFlow<ThemeSettings> di SharedPreferences; implements ThemeSource
ui/settings/SettingsScreen.kt   toggle mode + grid swatch aksen
```

- `ThemeSource` (interface: `settings: StateFlow<ThemeSettings>`, `setAccent(id)`, `setMode(mode)`) agar UI dan tes memakai fake. `LauncherApp` memegang satu `ThemeRepository`.
- `HomeActivity` mengumpulkan `settings` dan memberikannya ke `MyMetroUITheme`; gaya system bar (ikon terang/gelap) ikut mode.
- `AppListScreen` mendapat parameter `onOpenSettings` dan baris "Settings" setelah grup terakhir. `HomeScreen` menampilkan `SettingsScreen` sebagai overlay; Back menutupnya (di atas handler edit/pager).

## Data

- `ThemeSettings(accentId: String, mode: ThemeMode)`; default `accentId = DefaultAccent.id` (`0xFF1BA1E2`), `mode = DARK`.
- Palet ±20 warna bernama orisinal, id stabil (disimpan sebagai string).
- Nilai tersimpan yang tidak dikenal (id tak ada di palet, mode tak valid, prefs rusak) → jatuh ke default per field, tidak crash.

## Pemetaan warna

| Peran | Gelap | Terang |
|---|---|---|
| background/surface | hitam | putih |
| onBackground/onSurface | putih | hitam |
| primary | aksen | aksen |
| teks di atas tile/header aksen | putih | putih |

Warna yang saat ini tertulis langsung (`Color.Black`, `Color.White`, `0xFF1F1F1F` di HomeScreen, AppMenu, JumpList, DefaultLauncherPrompt) diganti ke `MaterialTheme.colorScheme`. Backdrop parallax memakai gradien dari background ke aksen yang digelapkan/diterangkan sesuai mode.

## Error handling

- Prefs gagal dibaca: default; tulis gagal: state tetap berubah di memori, tidak crash.
- Perubahan konfigurasi: state di `ThemeRepository` (singleton proses), tidak hilang saat rotasi.

## Pengujian (JVM, Robolectric)

1. `ThemeRepositoryTest`: default; `setAccent`/`setMode` tersimpan dan terbaca instance baru; id/mode korup → default.
2. Pemetaan: background putih saat LIGHT, hitam saat DARK; `primary` = aksen terpilih.
3. `SettingsScreenTest`: tap swatch memanggil `setAccent`; toggle mengganti mode; swatch terpilih ditandai.
4. `AppListScreenTest`: baris Settings tampil di akhir dan memanggil `onOpenSettings`.
5. Manual di perangkat: kontras teks dan ikon status bar di mode terang, semua aksen.
