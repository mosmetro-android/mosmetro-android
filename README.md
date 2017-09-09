# Wi-Fi в метро [![Build Status](https://local.thedrhax.pw/jenkins/job/MosMetro-Android/branch/master/badge/icon)](https://local.thedrhax.pw/jenkins/job/MosMetro-Android/branch/master/) [![ВКонтакте](.github/resources/vk-box.png)](https://vk.com/wifi_v_metro) [![Telegram](.github/resources/telegram.png)](https://t.me/joinchat/BVgshUHjD3rzVCmguodb4Q)

Данное приложение предназначено для автоматической авторизации Android-устройств в сетях московского общественного транспорта. Поддерживаются версии Android 3.0 (SDK 11) и выше.

[<img src="https://play.google.com/intl/ru_ru/badges/images/generic/ru_badge_web_generic.png" alt="Доступно в Google Play" height="80">](https://play.google.com/store/apps/details?id=pw.thedrhax.mosmetro) [<img src="https://gitlab.com/fdroid/artwork/raw/master/badge/get-it-on-ru.png" alt="Доступно в F-Droid" height="80">](https://f-droid.org/packages/pw.thedrhax.mosmetro)

## Список поддерживаемых сетей

:white_check_mark: Поддерживается; :large_blue_circle: Тестируется; :no_entry: Не поддерживается.

| Название сети | Провайдер | Где применяется | ? | Алгоритм |
| --- | --- | --- | --- | --- |
| MosMetro_Free | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Метро | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV2.java) |
| AURA | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Метро, Аэроэкспресс (?) | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV2.java) |
| MosMetro_Free | [NetByNet](http://www.netbynet.ru) | МЦК | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV1.java) |
| MosGorTrans_Free | [NetByNet](http://www.netbynet.ru) | Автобусы | :white_check_mark: | [MosMetro v1](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV1.java) |
| MosGorTrans_Free | [Enforta](http://www.enforta.ru/) | Остановки | :no_entry: | [MosGorTrans](/src/pw/thedrhax/mosmetro/authenticator/providers/Unknown.java) |
| MT_FREE, MT_FREE_ | [МаксимаТелеком](http://maximatelecom.ru/ru#/) + [NetByNet](http://www.netbynet.ru) | Метро, Автобусы, ... | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV2.java) |
| Air_WiFi_Free | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Аэропорт Внуково | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV2.java) |
| CPPK_Free | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Пригородные поезда | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/providers/MosMetroV1.java) |

## Сборка

Проект импортируется в среду разработки IntelliJ IDEA или совместимые, например Android Studio. Для сборки проекта потребуется Android SDK и Gradle.

Сборка в AIDE также поддерживается, но нужно вручную скачать [все](https://github.com/TheDrHax/mosmetro-android/tree/fc75df9b0c26489522ccf5581061fa57b5e6cd0f/libs) используемые библиотеки в директорию /libs. При этом в приложении не будет отображаться название и код версии, если специально не отредактировать для этого AndroidManifest.xml.

## Тестирование в Google Play

Вы можете записаться на тестирование [здесь](https://play.google.com/apps/testing/pw.thedrhax.mosmetro). Регистрация открыта для всех (просто нужно нажать на кнопку и бета версии начнут приходить вместо обычных). От тестировщиков ничего не требуется, но, если не трудно, сообщайте мне об ошибках на GitHub или отправляйте отчеты напрямую из приложения.

## Помочь проекту

По многочисленным просьбам активно настроенных пользователей был создан следующий перечень способов поддержать проект как материально, так и интеллектуально.

> Примечание: я никого не принуждаю! Это важно, так как было много случаев, когда меня отчитывали за то, что приложение требует поставить оценку в Google Play, хотя такого никогда не происходило.

* Материальная поддержка:
    * [Яндекс.Деньги](https://money.yandex.ru/to/410014087156910)
    * Bitcoin: 12biWJwYNqSqLF8ChrrW1xBGfjr1LAjEcb
* Интеллектуальная помощь:
    * Создание Pull Request'ов крайне приветствуется (если, конечно, они ничего не ломают)
* Популяризация:
    * [Оставить отзыв в Google Play](https://play.google.com/store/apps/details?id=pw.thedrhax.mosmetro)
    * [Вступить в сообщество ВКонтакте](https://vk.com/wifi_v_metro)
    * [Вступить в группу Telegram](https://t.me/joinchat/BVgshUHjD3rzVCmguodb4Q)
