import unittest

from smoke_komet_preview import keyboard_top


class KeyboardBoundsTest(unittest.TestCase):
    def test_multiple_sources_on_one_android_11_dump_line(self):
        windows = (
            'Window #0 Window{synthetic InputMethod}:\n'
            '  mGivenContentInsets=[0,1106][0,0]\n'
            '  mFrame=[0,63][1080,1920]\n'
            '  mRequestedInsetsState: InsetsState: {mSources= { '
            'InsetsSource: {mType=ITYPE_STATUS_BAR, mFrame=[0,0][1080,63], mVisible=true}, '
            'InsetsSource: {mType=ITYPE_NAVIGATION_BAR, mFrame=[0,1794][1080,1920], mVisible=true}, '
            'InsetsSource: {mType=ITYPE_IME, mFrame=[0,1169][1080,1920], mVisible=true} }'
        )
        self.assertEqual(keyboard_top(windows), 1169)

    def test_hidden_keyboard_has_no_visible_top(self):
        self.assertIsNone(keyboard_top(
            'InsetsSource: {mType=ITYPE_IME, mFrame=[0,1169][1080,1920], mVisible=false}'
        ))

    def test_other_insets_do_not_establish_keyboard_visibility(self):
        self.assertIsNone(keyboard_top(
            'InsetsSource: {mType=ITYPE_NAVIGATION_BAR, mFrame=[0,1794][1080,1920], mVisible=true}'
        ))

    def test_named_ime_source(self):
        self.assertEqual(keyboard_top(
            'InsetsSource: {type=ime, frame=[0,800][1080,1920], visible=true}'
        ), 800)

    def test_zero_frame_is_not_a_visible_keyboard(self):
        self.assertIsNone(keyboard_top(
            'InsetsSource: {mType=ITYPE_IME, mFrame=[0,0][0,0], mVisible=true}'
        ))


if __name__ == '__main__':
    unittest.main()
