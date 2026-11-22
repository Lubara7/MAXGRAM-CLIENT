import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import urllib.parse
import zipfile


ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'.git', '.dart_tool', '.gradle', 'build', 'local.properties', 'key.properties', '.env'}


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def include_source(info):
    parts = Path(info.name).parts
    if any(part in EXCLUDED or part.startswith('.env.') for part in parts) or info.name.endswith(('.keystore', '.jks')):
        return None
    return info


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--client', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    client, output = args.client.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT / 'integrations/komet/komet.lock.json').read_text())
    label = 'MAXGRAM-CLIENT-' + lock['version'].split('+')[0] + '-komet-ui'
    tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
    artifacts = []
    for abi in ('arm64-v8a', 'x86_64'):
        source = client / f'build/app/outputs/flutter-apk/app-{abi}-maxgram-release.apk'
        target = output / f'{label}-{abi}.apk'
        shutil.copy2(source, target)
        with zipfile.ZipFile(target) as apk:
            required = {'libapp.so', 'libflutter.so', 'libkolibri_dart.so', 'libkomet_crypto.so', 'librlottie.so'}
            found = {Path(name).name for name in apk.namelist() if name.startswith(f'lib/{abi}/')}
            if required - found:
                raise RuntimeError(f'{abi}: missing native libraries: {sorted(required - found)}')
        signature = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(target)], text=True)
        (output / f'signature-{abi}.txt').write_text(signature)
        manifest = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(target)], text=True)
        if f"name='{lock['applicationId']}'" not in manifest:
            raise RuntimeError('Preview package identity mismatch')
        (output / f'manifest-{abi}.txt').write_text(manifest)
        artifacts.append({'name': target.name, 'abi': abi, 'sha256': digest(target), 'bytes': target.stat().st_size})
    source_archive = output / f'{label}-source.tar.gz'
    config = json.loads((client / '.dart_tool/package_config.json').read_text())
    kolibri = next(package for package in config['packages'] if package['name'] == 'kolibri')
    kolibri_source = Path(urllib.parse.urlparse(kolibri['rootUri']).path)
    (client / 'MAXGRAM_BUILD.md').write_text(
        '# MaxGram Komet preview\n\n'
        'Prepared source from https://github.com/Lubara7/MAXGRAM-CLIENT at ' + os.environ['GITHUB_SHA'] + '.\n\n'
        'Use Flutter 3.44.3, JDK 17, Android SDK 36 and NDK 28.2.13676358. '
        'The application flavor is maxgram and the package is io.github.lubara7.maxgram.preview.\n\n'
        'Run flutter pub get, flutter analyze lib test, flutter test, then '
        'flutter build apk --release --flavor maxgram --target-platform android-arm64.\n\n'
        'Signing keys are deliberately excluded. Configure your own android/key.properties '
        'and optional android/app/src/maxgram/google-services.json before distributing a fork. '
        'Background FCM is unavailable without a matching Firebase project.\n'
    )
    with tarfile.open(source_archive, 'w:gz') as archive:
        archive.add(client, arcname=label, filter=include_source)
        archive.add(kolibri_source, arcname=f'{label}/vendor/kolibri-0.1.4', filter=include_source)
        for checkout in (Path.home() / '.cargo/git/checkouts').glob('kolibri-*/*'):
            if (checkout / 'Cargo.toml').is_file():
                archive.add(checkout, arcname=f'{label}/vendor/kolibri-core-{checkout.name}', filter=include_source)
    screenshots = output / 'telegram-ui-screenshots.zip'
    with zipfile.ZipFile(screenshots, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((client / 'test/goldens').glob('telegram-*.png')):
            archive.write(path, path.name)
        archive.write(client / 'MAXGRAM_UI_REFERENCE.md', 'source-comparison.md')
        archive.write(client / 'telegram-design.lock.json', 'telegram-design.lock.json')
    preparation = json.loads((client / 'maxgram-preparation.json').read_text())
    provenance = {
        'sourceCommit': os.environ['GITHUB_SHA'],
        'workflowRun': os.environ['GITHUB_RUN_ID'],
        'upstreamCommit': lock['commit'],
        'applicationId': lock['applicationId'],
        'version': lock['version'],
        'flutter': lock['flutter'],
        'artifacts': artifacts,
        'sourceArchiveSha256': digest(source_archive),
        'uiScreenshotsSha256': digest(screenshots),
        'preparation': preparation,
        'realAccountAcceptance': False,
        'observation72hComplete': False,
        'backgroundFcmConfigured': False,
    }
    (output / 'build-provenance.json').write_text(json.dumps(provenance, indent=2) + '\n')
    (output / 'SHA256SUMS.txt').write_text(''.join(f'{digest(path)}  {path.name}\n' for path in sorted(output.iterdir()) if path.is_file() and path.name != 'SHA256SUMS.txt'))
    print(json.dumps(provenance, indent=2))


if __name__ == '__main__':
    main()
