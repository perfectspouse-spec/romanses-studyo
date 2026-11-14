# RomanSes Stüdyo

Kotlin, Jetpack Compose, Room, Android Text-to-Speech ve Firebase AI Logic ile çalışan Android uygulaması.

## Yerel çalıştırma

1. Android Studio ile proje kökünü açın. Gradle 9.3.1 ve Android SDK 36.1 kullanılır.
2. Android Studio'da debug varyantını derleyip çalıştırın. Proje kökünde AI Studio'nun debug imza dosyası varsa o kullanılır. Yerel geliştirmede Android'in varsayılan debug imzası kullanılır.
3. Gemini özellikleri için kendi Firebase projenizin Android yapılandırmasını, Firebase AI Logic ve App Check ayarlarını doğrulayın. Yerel debug için Firebase'in debug sağlayıcısı kullanılır. Üretim App Check ayarını ve faturalandırmayı Firebase Console üzerinden doğrulayın.
4. Yayın imzası için ortamda KEYSTORE_PATH, STORE_PASSWORD ve KEY_PASSWORD değerlerini sağlayın. İmza dosyalarını ve parolaları kaynak arşivine eklemeyin.

GitHub kopyasında Firebase proje yapılandırması yerel olarak eklenir. Firebase Console'dan aldığınız Android yapılandırmasını `app/google-services.json` konumuna koyun. Üretimde reCAPTCHA Enterprise App Check kullanıyorsanız sitenizin anahtarını `app/src/main/res/values/firebase_applet_config.xml` içinde `recaptcha_site_key` adlı bir Android metin kaynağı olarak ekleyin. Kitapların cihazda çözümlenmesi için Firebase yapılandırması gerekmez.

## İçe aktarma

- PDF, EPUB ve UTF-8 TXT işlenir. PDF'de metin katmanı gerekir. Taranmış PDF sayfaları ve fotoğraflar Sayfa Analizi'nde cihazda OCR ile işlenebilir. Kullanıcı metni düzenleyip onayladıktan sonra kitaplığa ekleyebilir.
- Dosya sınırı 32 MB, açılmış EPUB verisi sınırı 32 MB, metin sınırı 2 milyon karakterdir. Sınır aşımı içe aktarmayı durdurur.
- Yerel çözümleyici kaynak dilini korur. Gemini seçilirse kitap metni parçalar halinde Google Firebase AI Logic'e iletilir. Bir parça tamamlanamazsa uygulama bütün metni yerel olarak işler.
- Kütüphane, senaryo ve dinleme konumu cihazdaki veritabanında saklanır. Kitaplık Android bulut yedekleme ve cihaz aktarımı kapsamından çıkarılmıştır.
