# Wi-Fi в метро [![Build Status](https://local.thedrhax.pw/jenkins/job/MosMetro-Android/branch/master/badge/icon)](https://local.thedrhax.pw/jenkins/job/MosMetro-Android/branch/master/) [![Google Play](img/google-play.png)](https://play.google.com/store/apps/details?id=pw.thedrhax.mosmetro) [![ВКонтакте](img/vk-box.png)](https://vk.com/wifi_v_metro)

Данное приложение предназначено для автоматической авторизации Android-устройств в сетях московского общественного транспорта. Поддерживаются версии Android 3.0 (SDK 11) и выше.

## Список поддерживаемых сетей

:white_check_mark: Поддерживается; :large_blue_circle: Тестируется; :no_entry: Не поддерживается.

| Название сети | Провайдер | Где применяется | ? | Алгоритм |
| --- | --- | --- | --- | --- | --- |
| MosMetro_Free | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Метро | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/networks/MosMetro.java) |
| AURA | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Метро, Аэроэкспресс (?) | :white_check_mark: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/networks/MosMetro.java) |
| MosMetro_Free | [NetByNet](http://www.netbynet.ru) | МЦК | :white_check_mark: | [MosMetro v1](/src/pw/thedrhax/mosmetro/authenticator/networks/MosMetro.java) |
| MosGorTrans_Free | [NetByNet](http://www.netbynet.ru) | Автобусы | :white_check_mark: | [MosMetro v1](/src/pw/thedrhax/mosmetro/authenticator/networks/MosMetro.java) |
| MosGorTrans_Free | [Enforta](http://www.enforta.ru/) | Остановки | :no_entry: | [MosGorTrans](/src/pw/thedrhax/mosmetro/authenticator/networks/MosGorTrans.java) |
| MT_Free | [МаксимаТелеком](http://maximatelecom.ru/ru#/) | Метро, ... | :large_blue_circle: | [MosMetro v2](/src/pw/thedrhax/mosmetro/authenticator/networks/MosMetro.java) |

## Сборка

Проект, импортируется в среду разработки IntelliJ IDEA или совместимые (изначально проект разрабатывался в AIDE, поэтому он не использует Gradle). Для сборки проекта потребуется Android SDK.

## Тестирование в Google Play

Вы можете записаться на тестирование [здесь](https://play.google.com/apps/testing/pw.thedrhax.mosmetro). Регистрация открыта для всех (просто нужно нажать на кнопку и бета версии начнут приходить вместо обычных). От тестировщиков ничего не требуется, но, если не трудно, сообщайте мне об ошибках на GitHub или отправляйте отчеты напрямую из приложения.
