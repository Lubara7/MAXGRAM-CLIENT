import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def adb(*args):
    return subprocess.run(['adb', *args], check=True, capture_output=True).stdout


def snapshot(filename):
    adb('shell', 'uiautomator', 'dump', '/sdcard/tgmax-ui.xml')
    adb('pull', '/sdcard/tgmax-ui.xml', filename)
    return list(ET.parse(filename).iter('node'))


def wait_for(filename, predicate):
    deadline = time.monotonic() + 100
    while time.monotonic() < deadline:
        try:
            nodes = snapshot(filename)
            if predicate(nodes):
                return nodes
        except (subprocess.CalledProcessError, ET.ParseError, OSError):
            pass
        time.sleep(2)
    raise RuntimeError('Expected Android login UI did not appear')


def tap(node):
    left, top, right, bottom = map(int, re.findall(r'\d+', node['bounds']))
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))


try:
    nodes = wait_for('login.xml', lambda rows: any(n.get('content-desc') == 'Phone number' for n in rows))
    assert any(n.get('text') == 'Check the country code and enter the phone number from your MAX account.' for n in nodes)
    Path('login.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    country = next(n for n in nodes if n.get('content-desc') == 'Country')
    tap(country.attrib)
    expected = ['Russia', 'Belarus', 'Azerbaijan', 'Armenia']
    nodes = wait_for('countries.xml', lambda rows: all(any(name in n.get('text', '') for n in rows) for name in expected))
    positions = [next(i for i, n in enumerate(nodes) if name in n.get('text', '')) for name in expected]
    assert positions == sorted(positions)
    Path('countries.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    tap(nodes[positions[0]].attrib)
    nodes = wait_for('login-country.xml', lambda rows: any(n.get('content-desc') == 'Country code' and n.get('text') == '7' for n in rows))
    assert any('Russia' in n.get('text', '') for n in nodes)
    print('Published APK shows the requested login copy, country order and +7 selection')
except Exception:
    for name, args in [('startup-logcat.txt', ('logcat', '-d')), ('activity-state.txt', ('shell', 'dumpsys', 'activity', 'activities'))]:
        try:
            Path(name).write_bytes(adb(*args))
        except subprocess.CalledProcessError:
            pass
    try:
        Path('failure.png').write_bytes(adb('exec-out', 'screencap', '-p'))
        adb('root')
        adb('wait-for-device')
        adb('pull', '/data/anr', 'anr-traces')
    except subprocess.CalledProcessError:
        pass
    raise
