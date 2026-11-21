import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/backend/api.dart';
import 'package:komet/backend/modules/messages.dart';
import 'package:komet/core/protocol/opcode_map.dart';
import 'package:komet/core/protocol/packet.dart';

class HistoryApi extends Api {
  HistoryApi(this.response);

  final Future<Packet> Function() response;
  Map<dynamic, dynamic>? request;

  @override
  Future<Packet> sendRequest(
    int opcode,
    Map<dynamic, dynamic> payload, {
    bool silent = false,
  }) {
    expect(opcode, Opcode.chatHistory);
    request = payload;
    return response();
  }
}

void main() {
  test(
    'an explicit empty history is accepted with the requested cursor',
    () async {
      final api = HistoryApi(
        () async => Packet(cmd: CmdType.ok, payload: {'messages': []}),
      );
      addTearDown(api.dispose);
      final messages = await MessagesModule(
        api,
      ).fetchHistory(101, 202, fromTime: 1700000000000);
      expect(messages, isEmpty);
      expect(api.request?['from'], 1700000000000);
      expect(api.request?['chatId'], 202);
    },
  );

  test(
    'a rejected page cannot be interpreted as the end of a conversation',
    () async {
      final api = HistoryApi(
        () async => Packet(cmd: CmdType.notFound, payload: {}),
      );
      addTearDown(api.dispose);
      await expectLater(
        MessagesModule(api).fetchHistory(101, 202),
        throwsA(isA<PacketError>()),
      );
    },
  );

  for (final payload in [
    null,
    <String, dynamic>{},
    {'messages': 'invalid'},
  ]) {
    test(
      'malformed history ${payload.runtimeType} remains retryable',
      () async {
        final api = HistoryApi(
          () async => Packet(cmd: CmdType.ok, payload: payload),
        );
        addTearDown(api.dispose);
        await expectLater(
          MessagesModule(api).fetchHistory(101, 202),
          throwsFormatException,
        );
      },
    );
  }

  test(
    'a transport timeout propagates without creating an empty history page',
    () async {
      final api = HistoryApi(
        () async => throw TimeoutException('synthetic timeout'),
      );
      addTearDown(api.dispose);
      await expectLater(
        MessagesModule(api).fetchHistory(101, 202),
        throwsA(isA<TimeoutException>()),
      );
    },
  );
}
