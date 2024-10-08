NoDPI — Обход блокировок DPI по SNI для Android TV и телефонов Android 
=========================

Это приложение создано для обхода систем Deep Packet Instection («Глубокой Инспекции Пакетов»), применяющихся у многих провайдеров для блокировки ресурсов или замедления отдельных сервисов по SNI.

Главная цель создания этого приложения это **обход замедления YouTube в России** на телевизорах с **Android TV**, т.к. удобных готовых решений не нашлось. Приложение прекрасно **работает и на Android телефонах**.

Приложение работает **без root прав**.

Приложение работает полностью **в автоматическом режиме** и не требует какой-либо настройки. 

# Как пользоваться

* **Телевизоры Android TV**: **[скачайте .apk файл в разделе Releases](https://github.com/raspabamos/nodpi/releases)** и установите на телефон или телевизор с Android TV. При нажатии на кнопку Запустить, приложение запросит разрешения на установку связи VPN: разрешите и пользуйтесь. 
* **Смартфоны на Android**: **[скачайте .apk файл в разделе Releases](https://github.com/raspabamos/nodpi/releases)** . Установка точно такая же, но на смартфонах приложение может запросить дополнительные разрешения (зависит от используемой версии Android) на показ уведомлений и фоновую работу. Уведомления нужны для корректной [фоновой работы](https://developer.android.com/develop/background-work/services/foreground-services) приложения. Фоновая работа необходима для поддержания активным VPN соединения к самому себе.

# Как работает

В основе приложения лежит библиотека tun2socks, которая превращает подключение к socks5 прокси в сетевой интерфейс, который превращается в VPN, а также специальный локальный socks5 сервер, который осуществляет изменение пакетов, проходящих через него (фрагментация первого пакета). Фактически, приложение устанавливает VPN соединение само с собой и пропускает все сетевые пакеты через себя.

Приложение использует Cloudflare для определения IP адресов и Cloudflare для определения успешности подключения. Если у Вашего провайдера заблокирован Cloudflare, напишите в Issues.

# Известные ограничения

* Приложение не позволит подключиться к сервису, если сервис заблокирован по IP адресу, а не с помощью SNI
* Для успешного обхода замедления YouTube иногда требуется сначала запустить приложение YouTube (включать видео не обязательно) с отключенным приложением **NoDPI**, а затем запустить приложение **NoDPI** и начать смотреть видео без замедления.
* На некоторых провайдерах может не работать. Пожалуйста, сообщайте об этом в раздел Issues с указанием региона, провайдера и даты проверки. 

# Похожие проекты

- **[GoodbyeDPI](https://github.com/ValdikSS/GoodbyeDPI)** by @ValdikSS (for Windows)
- **[zapret](https://github.com/bol-van/zapret)** by @bol-van (for MacOS, Linux and Windows)
- **[Green Tunnel](https://github.com/SadeghHayeri/GreenTunnel)** by @SadeghHayeri (for MacOS, Linux and Windows)
- **[DPI Tunnel CLI](https://github.com/nomoresat/DPITunnel-cli)** by @zhenyolka (for Linux and routers)
- **[DPI Tunnel for Android](https://github.com/nomoresat/DPITunnel-android)** by @zhenyolka (for Android)
- **[PowerTunnel](https://github.com/krlvm/PowerTunnel)** by @krlvm (for Windows, MacOS and Linux)
- **[PowerTunnel for Android](https://github.com/krlvm/PowerTunnel-Android)** by @krlvm (for Android)
- **[SpoofDPI](https://github.com/xvzc/SpoofDPI)** by @xvzc (for macOS and Linux)
- **[SpoofDPI-Platform](https://github.com/r3pr3ss10n/SpoofDPI-Platform)** by @r3pr3ss10n (for Android, macOS, Windows)
- **[GhosTCP](https://github.com/macronut/ghostcp)** by @macronut (for Windows)
- **[ByeDPI](https://github.com/hufrea/byedpi)** for Linux/Windows + **[ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid/)** for Android (no root)
- **[youtubeUnblock](https://github.com/Waujito/youtubeUnblock/)** by @Waujito (for OpenWRT/Entware routers and Linux)

