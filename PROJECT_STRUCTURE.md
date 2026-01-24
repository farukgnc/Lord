# Lord Core - Proje Yapısı ve Mimari Analiz

Bu doküman, **Lord Core** projesinin genel mimarisini, kullanılan tasarım desenlerini ve modüler yapısını detaylandırmak amacıyla hazırlanmıştır. Proje, modern Java standartları ve yüksek ölçeklenebilirlik hedeflenerek geliştirilmiştir.

---

## 1. Mimari Genel Bakış (Architecture Overview)

Lord Core, "Single Responsibility" (Tek Sorumluluk) ve "Decoupling" (Bağımsızlık) prensipleri üzerine inşa edilmiştir. Standart bir monolitik Minecraft eklentisinin aksine, her bir özellik kendi başına bir **Modül** ve **Servis** olarak tasarlanmıştır.

### Merkezi Kayıt Sistemi (Service Registry)
Projenin kalbinde, tüm servislerin ve bağımlılıkların yönetildiği bir `ServiceRegistry` bulunur. Bu yapı, eklentinin farklı parçaları arasında bağımlılıkların el ile (hardcoded) değil, merkezi bir sistem üzerinden yönetilmesini sağlar. (Basit bir Dependency Injection örneğidir).

### Modüler Tasarım
Tüm ana özellikler (`Rank`, `Grant`, `Punishment`, `Redis`, `Chat`) `Module` arayüzünü uygular. `ModuleManager` sayesinde bu modüllerin yaşam döngüsü (enable/disable) tek bir noktadan yönetilir.

---

## 2. Veri Katmanı (Data Layer)

Projenin en güçlü yönlerinden biri veritabanı bağımsızlığıdır.

### Repository Pattern
Veri erişim işlemleri `Repository` arayüzleri üzerinden yapılır. Bu sayede iş mantığı (Service), verinin nerede saklandığını (MongoDB veya Bellek) bilmek zorunda kalmaz.

- **MongoRepositoryFactory**: Verileri MongoDB üzerinde kalıcı olarak saklar.
- **InMemoryRepositoryFactory**: Özellikle test aşamalarında kullanılan, verileri RAM üzerinde geçici olarak tutan fabrika sınıfıdır.

### Asenkron İşlemler
Veritabanı ve Redis işlemleri `CompletableFuture` kullanılarak asenkron olarak gerçekleştirilir. Bu, sunucunun ana iş parçacığının (Main Thread) takılmasını önler ve performansı maksimize eder.

---

## 3. Ana Sistemler ve İşleyiş

### Rank & Grant Sistemi
- **RankService**: Rütbelerin oluşturulması, güncellenmesi ve silinmesi gibi iş mantığını yönetir.
- **GrantModule**: Oyunculara rütbe atanmasını (süreli veya kalıcı) sağlar.
- **PlayerDataCache**: Oyuncunun aktif yetkilerini ve rütbelerini hesaplayıp bellekte tutan, her değişiklikte (Redis senkronizasyonu dahil) kendisini asenkron olarak güncelleyen sistemdir.

### Ceza Sistemi (Punishment)
Yasaklamalar (Ban), susturmalar (Mute) ve uyarılar (Warn) tamamen `PunishmentService` üzerinden yönetilir. Her ceza, hem veritabanına kaydedilir hem de Redis üzerinden diğer sunuculara duyurulur.

### Komut Sistemi (Command System)
- **Annotation-Based**: Komutlar `plugin.yml` dosyasına eklenmek zorunda kalmadan, `@Command` annotasyonu ile sınıflar üzerinden tanımlanır.
- **Dynamic Registration**: `CommandManager` yansıma (reflection) kullanarak komutları çalışma anında otomatik olarak kaydeder.
- **CommandContext**: Argümanlar ve gönderici (sender) bilgisi, temiz bir yapı olan `CommandContext` içinde sarmalanarak komut sınıflarına iletilir.

---

## 4. PlayerData Cache ve Veri Akışı (Derinlemesine)

Projenin en karmaşık ve kritik parçası, oyuncu verilerinin nasıl yüklendiği ve izinlerin (permissions) nasıl yönetildiğidir. Bu sistem 3 ana aşamadan oluşur:

### 1. Veri Yükleme ve Isınma (Loading & Warming)
Oyuncu sunucuya bağlanmaya başladığında (`AsyncPlayerPreLoginEvent`):
- Oyuncunun tüm **Grant**'ları (aktif rütbeleri) ve **Punishment**'ları (cezaları) asenkron olarak veritabanından çekilir.
- `PlayerDataCalculator` bu ham verileri alır, rütbe miraslarını (inheritance) çözer ve oyuncunun sahip olması gereken **nihai izin setini** hesaplar.
- Bu veriler `PlayerDataCache` içine yerleştirilir.

### 2. İzin Enjeksiyonu (Permission Injection)
Oyuncu giriş yaptığında (`PlayerLoginEvent`):
- **Yansıma (Reflection)** kullanılarak Bukkit'in standart `PermissibleBase` sınıfı, bizim özel olarak yazdığımız `PlayerPermissible` ile değiştirilir.
- **Neden?** Bu sayede sunucudaki herhangi bir eklenti `player.hasPermission()` dediğinde, aslında bizim sistemimize soru sorulmuş olur. Bizim sistemimiz öncelikle önbellekteki (Cache) verilere bakar.

### 3. Dinamik Güncelleme (Real-time Refresh)
Oyuncu oyundayken rütbesi değişirse veya başka bir sunucudan ceza alırsa (Redis üzerinden):
- `PlayerDataCache.refreshPlayerData()` metodu tetiklenir.
- Sistem oyuncuyu oyundan atmadan, arka planda verileri tekrar çeker, hesaplar ve Cache'i günceller. Oyuncu bir sonraki izin kontrolünde yeni rütbesinin yetkilerini anında kullanmaya başlar.

---

## 5. Redis Senkronizasyonu
Çoklu sunucu (Proxy) ağlarında verilerin anlık olarak senkronize edilmesi için Redis Pub/Sub mimarisi kullanılır.
- Bir sunucuda yapılan rütbe değişikliği veya ceza işlemi, milisaniyeler içinde tüm ağdaki diğer sunuculara iletilir.
- **Sync Services**: `PunishmentSyncService`, `GrantSyncService` gibi sınıflar sadece mesajlaşma trafiğini yönetir.

---

## 4. Kullanılan Modern Teknolojiler

- **Lombok**: Boilerplate kodları (Getter, Setter, Constructor) azaltmak için kullanılır.
- **Adventure API & MiniMessage**: Minecraft mesajlarını renklendirmek ve biçimlendirmek için kullanılır (Standard & modern format).
- **Redis (Jedis)**: Sunucular arası anlık iletişim için.
- **MongoDB**: Yüksek performanslı ve esnek döküman tabanlı veritabanı.

---

## 5. Projenin Karakteristik Özellikleri

1.  **Temiz Kod (Clean Code)**: Metot isimleri ve sınıf yapıları kendini açıklayacak şekilde tasarlanmıştır.
2.  **Genişletilebilirlik**: Yeni bir modül eklemek, mevcut sistemi bozmadan sadece `Module` arayüzünü implement edip `Lord.java` içinde kaydetmekten ibarettir.
3.  **Hata Yönetimi**: Özel hata sınıfları (`RankAlreadyExistsException` vb.) ile kontrollü bir akış sağlanmıştır.

---

*Bu yapı, sadece bir Minecraft eklentisi değil, aynı zamanda profesyonel bir yazılım projesi standartlarında tasarlanmıştır.*
