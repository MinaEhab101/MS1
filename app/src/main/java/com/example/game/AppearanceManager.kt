package com.example.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BoardFelt
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointDark
import com.example.ui.theme.PointLight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 3D Board Material Theme
 */
data class BoardMaterial(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val priceCoins: Int,
    val woodDark: Color,
    val woodMedium: Color,
    val feltBackground: Color,
    val pointDark: Color,
    val pointLight: Color,
    val borderAccent: Color,
    val previewIcon: String
) {
    val frameGradient: List<Color>
        get() = listOf(woodMedium, woodDark, Color(0xFF150A04))

    val surfaceColors: List<Color>
        get() = listOf(feltBackground, feltBackground.copy(alpha = 0.85f), woodDark)

    val accentBorder: Color
        get() = borderAccent
}

/**
 * 3D Checker Piece Skin Theme
 */
data class PieceSkin(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val priceCoins: Int,
    val whiteBaseColors: List<Color>,
    val whiteRimColor: Color,
    val blackBaseColors: List<Color>,
    val blackRimColor: Color,
    val crownColorWhite: Color,
    val crownColorBlack: Color,
    val previewIcon: String
)

/**
 * Appearance Manager for purchasing, unlocking, and equipping 3D Board Materials and Piece Skins.
 * Integrates persistence via Android SharedPreferences.
 */
object AppearanceManager {

    private const val PREFS_NAME = "royal_appearance_prefs"
    private const val KEY_EQUIPPED_BOARD = "equipped_board_material"
    private const val KEY_EQUIPPED_SKIN = "equipped_piece_skin"
    private const val KEY_UNLOCKED_ITEMS = "unlocked_appearance_items"

    // --- Predefined 3D Board Materials ---
    val WALNUT_WOOD = BoardMaterial(
        id = "board_walnut",
        nameAr = "خشب الجوز الملكي",
        nameEn = "Royal Walnut Wood",
        descriptionAr = "خشب جوز طبيعي فاخر مع رقعة لباد مخملية كلاسيكية وتطعيمات خشبية عريقة.",
        priceCoins = 0,
        woodDark = BoardWoodDark,
        woodMedium = BoardWoodMedium,
        feltBackground = BoardFelt,
        pointDark = PointDark,
        pointLight = PointLight,
        borderAccent = GoldSecondary,
        previewIcon = "🪵"
    )

    val CARRARA_MARBLE = BoardMaterial(
        id = "board_marble",
        nameAr = "رخام كارارا الإيطالي",
        nameEn = "Italian Carrara Marble",
        descriptionAr = "رقعة رخام إيطالي مصقولة مع عروق ذهبية مشعة ونقاط صلبة من حجر اللازورد.",
        priceCoins = 2500,
        woodDark = Color(0xFF1E242B),
        woodMedium = Color(0xFF323B44),
        feltBackground = Color(0xFFEFEBE4),
        pointDark = Color(0xFF1A3B5C),
        pointLight = Color(0xFFD4AF37),
        borderAccent = Color(0xFFFFD700),
        previewIcon = "🏛️"
    )

    val BLACK_OBSIDIAN = BoardMaterial(
        id = "board_obsidian",
        nameAr = "الأوبسيديان والذهب الأسود",
        nameEn = "Black Obsidian & Gold",
        descriptionAr = "حجر أوبسيديان بركاني أسود شديد اللمعان محاط بزخارف ذهبية هندسية متلألئة.",
        priceCoins = 5000,
        woodDark = Color(0xFF0D0E12),
        woodMedium = Color(0xFF181A20),
        feltBackground = Color(0xFF12141C),
        pointDark = Color(0xFFD4AF37),
        pointLight = Color(0xFF262C38),
        borderAccent = Color(0xFFFFDF70),
        previewIcon = "💎"
    )

    val EMERALD_PALACE = BoardMaterial(
        id = "board_emerald",
        nameAr = "القصر الزمردي المخملي",
        nameEn = "Emerald Velvet Palace",
        descriptionAr = "رقعة ملكية من المخمل الزمردي الإمبراطوري ونقاط عاجية محاطة بإطار باروكي مذهب.",
        priceCoins = 8000,
        woodDark = Color(0xFF0F2617),
        woodMedium = Color(0xFF1C442B),
        feltBackground = Color(0xFF0D331D),
        pointDark = Color(0xFF9E782F),
        pointLight = Color(0xFFFAF6EE),
        borderAccent = Color(0xFF50C878),
        previewIcon = "👑"
    )

    val ALL_BOARD_MATERIALS = listOf(WALNUT_WOOD, CARRARA_MARBLE, BLACK_OBSIDIAN, EMERALD_PALACE)

    // --- Predefined 3D Piece Skins ---
    val ROYAL_PORCELAIN = PieceSkin(
        id = "skin_porcelain",
        nameAr = "الخزف العاجي والياقوت",
        nameEn = "Ivory Porcelain & Sapphire",
        descriptionAr = "قطع كلاسيكية من الخزف العاجي الناعم ضد أحجار الياقوت الأزرق الملكي المصقول.",
        priceCoins = 0,
        whiteBaseColors = listOf(Color(0xFFFAF7EE), Color(0xFFE8DECC), Color(0xFFC7BBA5)),
        whiteRimColor = Color(0xFFB5A386),
        blackBaseColors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1), Color(0xFF06183B)),
        blackRimColor = Color(0xFF1E5BB0),
        crownColorWhite = GoldPrimary,
        crownColorBlack = GoldSecondary,
        previewIcon = "⚪"
    )

    val GOLD_RUBY = PieceSkin(
        id = "skin_gold_ruby",
        nameAr = "ذهب خالص وياقوت إمبراطوري",
        nameEn = "24K Gold & Imperial Ruby",
        descriptionAr = "قطع مصبوبة من الذهب الخالص عيار 24 ضد بلورات الياقوت الإمبراطوري القرمزي النادر.",
        priceCoins = 3000,
        whiteBaseColors = listOf(Color(0xFFFFECB3), Color(0xFFFFD54F), Color(0xFFFFA000)),
        whiteRimColor = Color(0xFFFF8F00),
        blackBaseColors = listOf(Color(0xFFE53935), Color(0xFFB71C1C), Color(0xFF4A0007)),
        blackRimColor = Color(0xFFFF5252),
        crownColorWhite = Color(0xFFFFFFFF),
        crownColorBlack = GoldPrimary,
        previewIcon = "✨"
    )

    val CYBER_TITANIUM = PieceSkin(
        id = "skin_titanium",
        nameAr = "تيتانيوم ونيون مستقبلي",
        nameEn = "Cyber Titanium & Neon",
        descriptionAr = "قطع معدنية من التيتانيوم غير اللامع مع حلقات طاقة نيون فوسفورية تشع في الظلام.",
        priceCoins = 6000,
        whiteBaseColors = listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF424242)),
        whiteRimColor = Color(0xFF00E5FF),
        blackBaseColors = listOf(Color(0xFF37474F), Color(0xFF212121), Color(0xFF101010)),
        blackRimColor = Color(0xFFFF0055),
        crownColorWhite = Color(0xFF00E5FF),
        crownColorBlack = Color(0xFFFF0055),
        previewIcon = "⚡"
    )

    val CARVED_AMBER = PieceSkin(
        id = "skin_amber",
        nameAr = "مرمر أبيض وعنبر فاخر",
        nameEn = "Alabaster & Carved Amber",
        descriptionAr = "حجارة المرمر الأبيض النقي مع قطع العنبر الذهبي الشفاف المنحوت يدوياً.",
        priceCoins = 9000,
        whiteBaseColors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFFD6D6D6)),
        whiteRimColor = Color(0xFFB0BEC5),
        blackBaseColors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00), Color(0xFFBF360C)),
        blackRimColor = Color(0xFFFFCC80),
        crownColorWhite = Color(0xFF78909C),
        crownColorBlack = Color(0xFFFFE082),
        previewIcon = "🔶"
    )

    val ALL_PIECE_SKINS = listOf(ROYAL_PORCELAIN, GOLD_RUBY, CYBER_TITANIUM, CARVED_AMBER)

    private var prefs: SharedPreferences? = null

    private val _equippedBoard = MutableStateFlow<BoardMaterial>(WALNUT_WOOD)
    val equippedBoard: StateFlow<BoardMaterial> = _equippedBoard.asStateFlow()

    private val _equippedSkin = MutableStateFlow<PieceSkin>(ROYAL_PORCELAIN)
    val equippedSkin: StateFlow<PieceSkin> = _equippedSkin.asStateFlow()

    private val _unlockedItems = MutableStateFlow<Set<String>>(setOf("board_walnut", "skin_porcelain"))
    val unlockedItems: StateFlow<Set<String>> = _unlockedItems.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPersistedSettings()
        }
    }

    private fun loadPersistedSettings() {
        val sp = prefs ?: return
        val savedUnlocked = sp.getStringSet(KEY_UNLOCKED_ITEMS, setOf("board_walnut", "skin_porcelain")) ?: setOf("board_walnut", "skin_porcelain")
        _unlockedItems.value = savedUnlocked.toSet()

        val savedBoardId = sp.getString(KEY_EQUIPPED_BOARD, "board_walnut")
        _equippedBoard.value = ALL_BOARD_MATERIALS.find { it.id == savedBoardId } ?: WALNUT_WOOD

        val savedSkinId = sp.getString(KEY_EQUIPPED_SKIN, "skin_porcelain")
        _equippedSkin.value = ALL_PIECE_SKINS.find { it.id == savedSkinId } ?: ROYAL_PORCELAIN
    }

    fun isUnlocked(itemId: String): Boolean {
        return _unlockedItems.value.contains(itemId)
    }

    fun equipBoardMaterial(material: BoardMaterial) {
        if (isUnlocked(material.id)) {
            _equippedBoard.value = material
            prefs?.edit()?.putString(KEY_EQUIPPED_BOARD, material.id)?.apply()
        }
    }

    fun equipPieceSkin(skin: PieceSkin) {
        if (isUnlocked(skin.id)) {
            _equippedSkin.value = skin
            prefs?.edit()?.putString(KEY_EQUIPPED_SKIN, skin.id)?.apply()
        }
    }

    fun unlockItem(itemId: String): Boolean {
        val current = _unlockedItems.value.toMutableSet()
        if (current.add(itemId)) {
            _unlockedItems.value = current
            prefs?.edit()?.putStringSet(KEY_UNLOCKED_ITEMS, current)?.apply()
            return true
        }
        return false
    }
}
