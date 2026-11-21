import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/core/utils/update_checker.dart';
import 'package:package_info_plus/package_info_plus.dart';

class ReleaseHeaders implements HttpHeaders {
  @override
  void set(String name, Object value, {bool preserveHeaderCase = false}) {}

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class ReleaseResponse extends Stream<List<int>> implements HttpClientResponse {
  ReleaseResponse(this.releases);
  final List<Map<String, Object>> releases;

  @override
  int get statusCode => HttpStatus.ok;

  @override
  StreamSubscription<List<int>> listen(
    void Function(List<int>)? onData, {
    Function? onError,
    void Function()? onDone,
    bool? cancelOnError,
  }) => Stream.value(utf8.encode(jsonEncode(releases))).listen(
    onData,
    onError: onError,
    onDone: onDone,
    cancelOnError: cancelOnError,
  );

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class ReleaseRequest implements HttpClientRequest {
  ReleaseRequest(this.releases);
  final List<Map<String, Object>> releases;

  @override
  final HttpHeaders headers = ReleaseHeaders();

  @override
  Future<HttpClientResponse> close() async => ReleaseResponse(releases);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class ReleaseClient implements HttpClient {
  ReleaseClient(this.releases);
  final List<Map<String, Object>> releases;

  @override
  Duration? connectionTimeout;

  @override
  Future<HttpClientRequest> getUrl(Uri url) async {
    expect(url.host, 'api.github.com');
    expect(url.path, '/repos/Lubara7/MAXGRAM-CLIENT/releases');
    return ReleaseRequest(releases);
  }

  @override
  void close({bool force = false}) {}

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Map<String, Object> release(String tag) => {
  'tag_name': tag,
  'draft': false,
  'html_url': 'https://example.invalid/release',
  'assets': <Object>[],
};

Future<AppUpdateInfo?> check(List<Map<String, Object>> releases) =>
    HttpOverrides.runZoned(
      UpdateChecker.fetchLatest,
      createHttpClient: (_) => ReleaseClient(releases),
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(
    () => PackageInfo.setMockInitialValues(
      appName: 'MAXGRAM',
      packageName: 'io.github.lubara7.maxgram.preview',
      version: '0.4.0-dev.3',
      buildNumber: '2018',
      buildSignature: 'synthetic-signature',
    ),
  );

  test(
    'a later preview build is offered within the same core version',
    () async {
      final update = await check([
        release('v99.0.0+999'),
        release('v0.4.0-dev.4+19-komet-ui'),
      ]);
      expect(update?.build, 19);
      expect(update?.tag, 'v0.4.0-dev.4+19-komet-ui');
    },
  );

  test('the installed split-ABI build is not offered again', () async {
    expect(await check([release('v0.4.0-dev.3+18-komet-ui')]), isNull);
  });

  test('the legacy native APK channel is ignored', () async {
    expect(await check([release('v99.0.0+999')]), isNull);
  });
}
