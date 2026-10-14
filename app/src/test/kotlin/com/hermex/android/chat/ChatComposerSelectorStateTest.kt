package com.hermex.android.chat

import com.hermex.android.core.network.dto.ModelCatalogGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatComposerSelectorStateTest {
    @Test
    fun `ChatComposerModelSelectorState-from maps model fields verbatim`() {
        val groups = listOf(ModelCatalogGroup(id = "openai-codex", name = "openai-codex", providerId = "openai-codex", models = emptyList()))
        val uiState = ChatUiState(
            modelCatalogGroups = groups,
            currentModel = "gpt-5.5",
            currentModelProvider = "openai-codex",
            isLoadingModelCatalog = true,
        )

        val selectorState = ChatComposerModelSelectorState.from(uiState)

        assertEquals(groups, selectorState.modelCatalogGroups)
        assertEquals("gpt-5.5", selectorState.currentModel)
        assertEquals("openai-codex", selectorState.currentModelProvider)
        assertEquals(true, selectorState.isLoadingModelCatalog)
    }
}
