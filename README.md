# TG-MAX Ultimate

Android-клиент MAX на основе интерфейса [NagramXF](https://github.com/Keeperorowner/NagramXF), с клиентским протоколом MAX по [Komet](https://github.com/KometTeam/Komet).

**Статус: экспериментальная альфа.** Перенесён базовый транспорт MAX и начальный слой совместимости с Nagram. Полная замена всех подсистем ещё не завершена. Успешная сборка APK не означает проверенный вход на реальном аккаунте.

## Основа

- Настоящие Java/Kotlin UI-компоненты Nagram: экраны входа, список чатов, пузыри, профили и настройки.
- Отдельный Java backend MAX: бинарные WebSocket-пакеты, MessagePack, LZ4, handshake, корреляция ответов и push.
- Начальные операции: телефон/код/пароль MAX, загрузка диалогов и истории, текст, reply/edit/delete/read, известные каналы.
- Сессия шифруется AES-GCM ключом Android Keystore. Временные коды, пароль и challenge не сохраняются в login preferences.
- MTProto-вызовы ConnectionsManager ограждены; неподдержанные операции завершаются ошибкой без перехода в Telegram.
- Разрешены языки `ru`, `en`, `es`, `uz`, `pt`, `pt-BR`, `fr`, `be`; auth-строки включены для всех вариантов. Полный перевод большого интерфейса Nagram ещё требует покрытия: непереведённые строки используют upstream English.
- QR-вход отключён. Готового сервера-посредника TG-MAX нет.

## Ограничения альфы

Не реализованы: исходящий звонок для подтверждения входа, восстановление через почту, медиа/uploads, стикеры, истории, аудио/видеозвонки, MAX push в фоне, полные серверные настройки/приватность/папки и каталог рекомендаций. В имеющихся экранах Nagram остаются действия, для которых MAX-адаптер возвращает `MAX_UNSUPPORTED_*`. Полного соответствия 45 экранам MAX альфа пока не достигает.

На аккаунте, требующем неподдержанную проверку входа, авторизация не завершается. Рабочий код `12345` относится только к отдельному историческому auth-harness и никогда не используется в Nagram/MAX backend.

## Подготовка настоящего клиента

```bash
git submodule update --init --depth 1 upstream/nagramxf upstream/komet
python3 scripts/prepare_nagram_client.py
python3 scripts/init_nagram_dependencies.py build/nagram-client
```

Результат — `build/nagram-client`, отдельный checkout Nagram с интеграцией. Исходные submodules не изменяются. Скрипт отказывается перезаписывать уже существующий output; для повторной подготовки используйте новый `--output`.

Нужны JDK 21, Android SDK 36, Build Tools 36.0.0, NDK 27.2.12479018 и CMake 3.31.6. Для 64-битного ARM:

```bash
cd build/nagram-client
NATIVE_TARGET=arm64-v8a ./gradlew :TMessagesProj:assembleNormalDebug
```

Минимальная версия Android у закреплённого Nagram — API 27 (Android 8.1). Debug-сборка подписывается тестовым Android debug key. Ключ релизной подписи не включён в репозиторий.

## Проверки backend и адаптера

В корне проекта нужны JDK 17 и SDK 36:

```bash
./gradlew :max-backend:test :max-backend:writeAdapterClasspath
python3 scripts/check_nagram_api.py
```

`check_nagram_api.py` проверяет типы используемых полей/классов по закреплённым исходникам Nagram. Это отдельная быстрая проверка; она не заменяет полную компиляцию Android UI или сквозные тесты на устройстве.

`tools-auth-harness`, `core/*`, `feature/auth` сохраняют прежний отдельный Compose стенд авторизации под другим applicationId. Его APK не является клиентом Nagram и не публикуется как TG-MAX.

## Документация

[Карта 45 скриншотов и целевая архитектура](docs/screen-map.md), [интеграция Nagram](docs/nagram-migration.md), [происхождение исходников](SOURCE_PROVENANCE.md).
