package beer.thierry.centsible.integrations.manual

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.FieldType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManualProviderModuleTest {

    private val module = ManualProviderModule()

    @Test
    fun `descriptor key and display name are stable`() {
        assertEquals("manual", module.descriptor.key)
        assertEquals("Manual entry", module.descriptor.displayName)
    }

    @Test
    fun `descriptor declares no auth and no capabilities`() {
        // ManualProviderModule is the reference no-op provider; advertising a capability would
        // make the sync orchestrator route real work to it and fail.
        assertEquals(AuthType.NONE, module.descriptor.authType)
        assertTrue(module.descriptor.capabilities.isEmpty())
    }

    @Test
    fun `descriptor exposes a required label field and an optional notes field`() {
        val fields = module.descriptor.configFields.associateBy { it.name }

        val label = fields["label"]
        assertNotNull(label)
        assertTrue(label!!.required)
        assertEquals(FieldType.STRING, label.type)

        val notes = fields["notes"]
        assertNotNull(notes)
        assertTrue(!notes!!.required)
        assertEquals(FieldType.MULTILINE, notes.type)
    }

    @Test
    fun `no field is marked as secret`() {
        // Manual entry has no credentials to encrypt; nothing should be marked secret=true.
        assertTrue(module.descriptor.configFields.none { it.secret })
    }
}
