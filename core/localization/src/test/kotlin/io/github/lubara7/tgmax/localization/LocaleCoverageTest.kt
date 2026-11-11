package io.github.lubara7.tgmax.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LocaleCoverageTest {
    private fun document(file: File) = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(file)

    private fun resources(file: File): Map<String, Element> {
        val nodes = document(file).documentElement.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
            .filter { it.getAttribute("translatable") != "false" }
            .associateBy { it.getAttribute("name") }
    }

    @Test fun `every allowed locale translates all strings and preserves placeholders`() {
        val root = File("src/main/res")
        val expected = resources(File(root, "values/strings.xml"))
        val placeholder = Regex("%[0-9]+\\$[sdf]")
        for (qualifier in listOf("ru", "es", "uz", "pt", "pt-rBR", "fr", "be")) {
            val actual = resources(File(root, "values-$qualifier/strings.xml"))
            assertEquals(qualifier, expected.keys, actual.keys)
            for ((key, node) in expected) {
                val target = actual.getValue(key)
                assertEquals("$qualifier/$key resource type", node.tagName, target.tagName)
                assertTrue("$qualifier/$key is blank", target.textContent.isNotBlank())
                assertEquals("$qualifier/$key placeholders",
                    placeholder.findAll(node.textContent).map { it.value }.toSet(),
                    placeholder.findAll(target.textContent).map { it.value }.toSet())
                if (target.tagName == "plurals") {
                    val items = target.getElementsByTagName("item")
                    assertTrue((0 until items.length).any { (items.item(it) as Element).getAttribute("quantity") == "other" })
                }
            }
        }
    }

    @Test fun `system language list contains exactly the supported tags`() {
        val nodes = document(File("../../tools-auth-harness/src/main/res/xml/locales_config.xml")).getElementsByTagName("locale")
        val tags = (0 until nodes.length).map {
            (nodes.item(it) as Element).getAttribute("android:name")
        }
        assertEquals(AppLanguage.entries.map { it.tag }.toSet(), tags.toSet())
        assertEquals(8, tags.size)
    }

    @Test fun `regional Portuguese and unsupported locales resolve deliberately`() {
        assertEquals("pt", AppLanguage.normalize("pt-PT"))
        assertEquals("pt-BR", AppLanguage.normalize("pt-BR"))
        assertEquals("pt-BR", AppLanguage.normalize("pt-Latn-BR"))
        assertEquals("en", AppLanguage.normalize("en-US"))
        assertEquals("", AppLanguage.normalize("de-DE"))
    }
}
