# Cagan IP TV v0.1

Android TV / TV Box için IPTV player.

Bu uygulama IPTV yayını sağlamaz. Kullanıcı kendi erişim yetkisine sahip M3U / M3U8 listesini ekler.

## v0.1

- Android TV / TV Box launcher desteği
- M3U / M3U8 URL ekleme
- EXTINF parse
- group-title kategori desteği
- Kanal grid arayüzü
- Media3 / ExoPlayer
- Cleartext HTTP stream desteği
- GitHub Actions APK build
- Java 17 / Kotlin JVM 17

## Sonraki sürüm hedefleri

- Xtream Codes API
- EPG / XMLTV
- Favoriler
- Son izlenen kanallar
- Search
- Playlist kalıcı kayıt
- Kanal logoları
- CH+ / CH- kumanda kontrolü
- Player bilgi overlay

## v0.2 - Remote varsayılan playlist

Uygulama açılış önceliği:
1. Kullanıcının daha önce kaydettiği manuel M3U URL'si
2. Remote `config.json` içindeki aktif playlistler (sırayla fallback)
3. Son başarılı M3U içeriğinin yerel önbelleği

Remote config örneği `remote-config/config.json` dosyasındadır. Bu dosyayı kendi GitHub/repo veya HTTPS sunucunuzda yayınlayın.

GitHub repository > Settings > Secrets and variables > Actions altında `CAGAN_CONFIG_URL` secret'ı oluşturup raw config URL'sini girin. Workflow bu değeri APK içine BuildConfig olarak ekler.

Örnek config:
```json
{
  "version": 1,
  "playlists": [
    {"name":"Varsayılan Liste","url":"https://example.org/default.m3u","enabled":true},
    {"name":"Yedek Liste","url":"https://example.org/backup.m3u","enabled":true}
  ]
}
```

Not: Yalnızca dağıtma/erişim hakkınız bulunan yayın listelerini kullanın.
