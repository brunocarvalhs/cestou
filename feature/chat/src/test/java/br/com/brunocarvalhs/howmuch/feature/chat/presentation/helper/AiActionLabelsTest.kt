package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import br.com.brunocarvalhs.howmuch.feature.chat.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AiActionLabelsTest {

    @Test
    fun `no action or an unknown one reads as thinking`() {
        assertEquals(R.string.ai_chat_activity_thinking, activityLabelFor(null))
        assertEquals(R.string.ai_chat_activity_thinking, activityLabelFor("something_new"))
    }

    @Test
    fun `actions of the same intent share a label`() {
        assertEquals(R.string.ai_chat_activity_adding_items, activityLabelFor("save_product_to_cart"))
        assertEquals(R.string.ai_chat_activity_adding_items, activityLabelFor("add_recipe_ingredients"))
        assertEquals(R.string.ai_chat_activity_removing, activityLabelFor("delete_shopping_list"))
        assertEquals(R.string.ai_chat_activity_removing, activityLabelFor("clear_purchased_products"))
    }
}
