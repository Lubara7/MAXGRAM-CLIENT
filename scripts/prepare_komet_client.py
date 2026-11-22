import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
INTEGRATION = ROOT / 'integrations/komet'
PROTECTED = ('lib/backend', 'lib/core/transport', 'lib/core/protocol', 'lib/core/storage')


def run(*args, cwd=None):
    return subprocess.check_output(args, cwd=cwd, text=True).strip()


def fingerprints(root):
    return {
        str(file.relative_to(root)): hashlib.sha256(file.read_bytes()).hexdigest()
        for directory in PROTECTED
        for file in sorted((root / directory).rglob('*'))
        if file.is_file()
    }


def prepare(source, target):
    lock = json.loads((INTEGRATION / 'komet.lock.json').read_text())
    if run('git', 'rev-parse', 'HEAD', cwd=source) != lock['commit']:
        raise RuntimeError('Komet checkout does not match integrations/komet/komet.lock.json')
    if target.exists():
        raise RuntimeError(f'Refusing to overwrite existing directory: {target}')
    target.parent.mkdir(parents=True, exist_ok=True)
    run('git', 'clone', '--no-hardlinks', '--no-checkout', str(source), str(target))
    run('git', 'checkout', '--detach', lock['commit'], cwd=target)
    before = fingerprints(target)
    if not before or not any(path.startswith('lib/backend/') for path in before):
        raise RuntimeError('Pinned MAX backend is missing')
    patch = str(INTEGRATION / 'telegram-ui.patch')
    run('git', 'apply', '--check', patch, cwd=target)
    run('git', 'apply', patch, cwd=target)
    shutil.copytree(INTEGRATION / 'lib', target / 'lib', dirs_exist_ok=True)
    shutil.copytree(INTEGRATION / 'test', target / 'test', dirs_exist_ok=True)
    for directory in ('assets', 'shaders'):
        if (INTEGRATION / directory).is_dir():
            shutil.copytree(INTEGRATION / directory, target / directory, dirs_exist_ok=True)
    design_path = INTEGRATION / 'telegram-design.lock.json'
    design = json.loads(design_path.read_text()) if design_path.is_file() else None
    if design is not None:
        shutil.copy2(design_path, target / 'telegram-design.lock.json')
        shutil.copy2(ROOT / 'docs/telegram-android-design.md', target / 'MAXGRAM_UI_REFERENCE.md')
        for asset in design['assets']:
            actual = hashlib.sha256((target / asset['asset']).read_bytes()).hexdigest()
            if actual != asset['sha256']:
                raise RuntimeError('Telegram UI asset hash mismatch: ' + asset['asset'])
    if f"version: {lock['version']}" not in (target / 'pubspec.yaml').read_text().splitlines():
        raise RuntimeError('Prepared application version does not match the lock')
    after = fingerprints(target)
    changed = {path for path in before.keys() | after.keys() if before.get(path) != after.get(path)}
    reviewed = lock.get('backendFixes', {})
    if changed != set(reviewed):
        raise RuntimeError(f'Unexpected MAX backend changes: {sorted(changed ^ set(reviewed))}')
    for path, expected in reviewed.items():
        if before.get(path) != expected['beforeSha256'] or after.get(path) != expected['afterSha256']:
            raise RuntimeError(f'Reviewed MAX backend patch changed: {path}')
    result = {
        'upstreamCommit': lock['commit'],
        'protectedFiles': len(before),
        'protectedFilesUnchanged': not changed,
        'protectedFilesPreserved': len(before) - len(changed),
        'reviewedBackendFixes': sorted(changed),
        'patchSha256': hashlib.sha256(Path(patch).read_bytes()).hexdigest(),
        'applicationId': lock['applicationId'],
        'status': lock['status'],
        'telegramUiSourceCommit': design['commit'] if design is not None else None,
        'telegramUiAssetsVerified': len(design['assets']) if design is not None else 0,
    }
    (target / 'maxgram-preparation.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/komet-client')
    args = parser.parse_args()
    lock = json.loads((INTEGRATION / 'komet.lock.json').read_text())
    source = (args.source or ROOT / lock['source']).resolve()
    target = args.output.resolve()
    prepare(source, target)


if __name__ == '__main__':
    main()
