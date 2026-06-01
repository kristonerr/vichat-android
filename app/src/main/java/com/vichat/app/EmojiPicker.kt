package com.vichat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val emojiCategories = listOf(
    "Лица" to listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","😊","😇","🥰","😍","🤩","😘","😗","😚","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🥴","😵","🤯","😎","🥳","😟","😢","😭","😤","😠","😡","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖"),
    "Жесты" to listOf("👍","👎","👊","✊","🤛","🤜","🤚","✋","🖐️","✌️","🤞","🫰","🤟","🤘","🤙","👌","🤌","🤏","🫳","🫴","✍️","👏","🙌","🫶","🤲","🤝","🙏","💪","🦵","🦶","👂","🦻","👃","🧠","🫀","🫁","👀","👅","👄"),
    "Сердца" to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💕","💞","💓","💗","💖","💘","💝","💟","❣️","💔","❤️‍🔥"),
    "Предметы" to listOf("💋","💌","💎","💍","👑","🎒","👝","👛","👜","💼","🎓","👓","🕶️","🌂","🧵","🧶","🎁","🎊","🎉","🎈","🎀","🪄","🔮","💣","🔫","💊","🩸","🔪","🗡️","🛡️","🚬","⚰️","🪦","⚱️"),
    "Еда" to listOf("🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌽","🥕","🧄","🧅","🥔","🍠","🥐","🍞","🥖","🥨","🧀","🥚","🍳","🥞","🧇","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕","🥪","🥙","🧆","🌮","🌯","🥗","🥘","🫕","🥫","🍝","🍜","🍲","🍛","🍣","🍱","🥟","🦪","🍤","🍙","🍚","🍘","🍥","🥠","🥮","🍢","🍡","🍧","🍨","🍦","🥧","🧁","🍰","🎂","🍮","🍭","🍬","🍫","🍿","🍩","🍪","🌰","🥜","🍯"),
    "Животные" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🐣","🐥","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜","🪰","🪲","🪳","🦟","🦗","🕷️","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈"),
    "Природа" to listOf("🌺","🌸","🌼","🌻","🌹","🥀","🌷","🌿","🍀","🍃","🍂","🍁","🪺","🪹","🌵","🌲","🌳","🌴","☘️","🎋","🍄","🐚","🌍","🌎","🌏","🌞","🌝","🌛","🌜","🌚","🌕","🌖","🌗","🌘","🌑","🌒","🌓","🌔","🌈","☀️","🌤️","⛅","🌥️","☁️","🌦️","🌧️","⛈️","🌩️","🌨️","❄️","☃️","⛄","🌬️","💨","🌪️","🌫️","☔","💧","💦","🌊"),
    "Спорт" to listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","🪁","🏹","🎣","🤿","🥊","🥋","🎽","🛹","🛼","🛷","⛸️","🥌","🎿","⛷️","🏂","🏋️","🤼","🤸","🤺","⛹️","🤾","🏌️","🏇","🧘","🏄","🏊","🤽","🚣","🧗","🚴"),
    "Транспорт" to listOf("🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🏍️","🛵","🛺","🚲","🛴","🛹","🚏","🎢","🎠","🚂","🚃","🚄","🚅","🚆","🚇","🚈","🚉","🚝","🚞","✈️","🚁","🚀","🛸","🚢","⛵","🛥️","🚤","🛶","⛴️"),
)

@Composable
fun EmojiPicker(
    visible: Boolean,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var selectedCategory by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE))
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = emojiCategories.map { it.first }
            categories.forEachIndexed { index, name ->
                val isSelected = index == selectedCategory
                Text(
                    text = when (name) {
                        "Лица" -> "😀"
                        "Жесты" -> "👍"
                        "Сердца" -> "❤️"
                        "Предметы" -> "💎"
                        "Еда" -> "🍔"
                        "Животные" -> "🐱"
                        "Природа" -> "🌺"
                        "Спорт" -> "⚽"
                        "Транспорт" -> "🚗"
                        else -> "😀"
                    },
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(
                            if (isSelected) Color(0xFFD0D0D0) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategory = index }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "✕",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }

        val emojis = emojiCategories[selectedCategory].second
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(emojis) { emoji ->
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .padding(2.dp)
                        .clickable { onEmojiSelected(emoji) }
                        .padding(6.dp)
                )
            }
        }
    }
}
