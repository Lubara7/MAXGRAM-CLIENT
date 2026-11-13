import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../integrations/nagram/src/main/assets/tgmax-captcha.html', import.meta.url), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace('__TGMAX_CONFIG__', JSON.stringify({ url: 'https://captcha.vk.com/synthetic', language: 'ru', dark: false }));
function page() {
    const listeners = new Map();
    const messages = [];
    const frame = { contentWindow: { postMessage() {} } };
    const context = {
        URL, scrollX: 0, scrollY: 0, setTimeout: () => 1, clearTimeout() {},
        window: { addEventListener: (type, callback) => listeners.set(type, callback) },
        document: { getElementById: () => frame, documentElement: { style: {} }, addEventListener() {} },
        tgmaxCaptcha: { postMessage: value => messages.push(JSON.parse(value)) }
    };
    vm.runInNewContext(script, context);
    return { frame, messages, receive: event => listeners.get('message')(event) };
}
const ready = page();
assert.equal(new URL(ready.frame.src).searchParams.get('origin'), 'https://web.max.ru');
const event = { origin: 'https://captcha.vk.com', source: ready.frame.contentWindow,
    data: { type: 'vk-sak-sdk', handler: 'VKCaptchaGetResult', params: { token: 'synthetic-proof' } } };
ready.receive({ ...event, origin: 'https://untrusted.example' });
ready.receive({ ...event, source: {} });
ready.receive({ ...event, data: { ...event.data, type: 'other-widget' } });
assert.deepEqual(ready.messages, []);
ready.receive(event);
ready.receive(event);
assert.deepEqual(ready.messages, [{ type: 'token', token: 'synthetic-proof' }]);
const cancelled = page();
cancelled.receive({ ...event, source: cancelled.frame.contentWindow, data: { type: 'vk-sak-sdk', handler: 'VKCaptchaCloseCaptcha' } });
cancelled.receive({ ...event, source: cancelled.frame.contentWindow });
assert.deepEqual(cancelled.messages, [{ type: 'cancel' }]);
console.log('CAPTCHA origin, source and cancellation checks passed');
