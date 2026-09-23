package com.example.ui.screens.shop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.game.AppearanceManager
import com.example.game.BoardMaterial
import com.example.game.PieceSkin
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceShopScreen(
    userProfile: UserProfile? = null,
    authRepository: AuthRepository? = null,
    firestoreRepository: FirestoreRepository? = null,
    soundManager: SoundManager? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { AppearanceManager.initialize(context) }

    val equippedBoard by AppearanceManager.equippedBoard.collectAsState()
    val equippedSkin by AppearanceManager.equippedSkin.collectAsState()
    val unlockedItems by AppearanceManager.unlockedItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    val currentCoins = userProfile?.coins ?: 1000

    fun purchaseItem(price: Int, itemId: String, onEquip: () -> Unit) {
        if (currentCoins < price) {
            notificationMessage = "رصيدك غير كافٍ! تحتاج إلى $price كوينز."
            soundManager?.playBlotHit()
            return
        }

        // Deduct coins & update profile
        val updatedCoins = currentCoins - price
        userProfile?.let { prof ->
            val updated = prof.copy(coins = updatedCoins)
            authRepository?.saveUser(updated)
            prof.userId.let { uid ->
                if (uid.isNotEmpty()) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        firestoreRepository?.updateUserCoins(uid, updatedCoins)
                    }
                }
            }
        }

        AppearanceManager.unlockItem(itemId)
        onEquip()
        soundManager?.playVictory()
        notificationMessage = "مبروك! تم الشراء والتفعيل بنجاح! 👑"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BoardWoodDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "متجر المظهر الملكي 3D",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldSecondary
                        )
                        Text(
                            text = "تخصيص رقعة الطاولة والقطع بأجود المواد الفاخرة",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("shop_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldSecondary
                        )
                    }
                },
                actions = {
                    // Coins Chip
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(GoldDark, Color(0xFF2E1C0C))))
                            .border(1.2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("shop_coins_chip")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentCoins",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0B08)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Notification toast banner
            AnimatedVisibility(
                visible = notificationMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                notificationMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF388E3C), Color(0xFF1B5E20))))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .clickable { notificationMessage = null }
                                    .padding(4.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Material 3 Custom Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF17120D),
                contentColor = GoldSecondary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) GoldPrimary else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "رقعة الطاولة 3D",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) GoldSecondary else TextMuted
                            )
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) GoldPrimary else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "طراز القطع 3D",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) GoldSecondary else TextMuted
                            )
                        }
                    }
                )
            }

            // Content List
            if (selectedTab == 0) {
                BoardMaterialsTab(
                    currentEquipped = equippedBoard,
                    unlockedItems = unlockedItems,
                    onEquip = { material ->
                        AppearanceManager.equipBoardMaterial(material)
                        soundManager?.playCheckerMove()
                        notificationMessage = "تم تفعيل رقعة ${material.nameAr} بنجاح!"
                    },
                    onBuy = { material ->
                        purchaseItem(material.priceCoins, material.id) {
                            AppearanceManager.equipBoardMaterial(material)
                        }
                    }
                )
            } else {
                PieceSkinsTab(
                    currentEquipped = equippedSkin,
                    unlockedItems = unlockedItems,
                    onEquip = { skin ->
                        AppearanceManager.equipPieceSkin(skin)
                        soundManager?.playCheckerMove()
                        notificationMessage = "تم تفعيل مظهر قطع ${skin.nameAr} بنجاح!"
                    },
                    onBuy = { skin ->
                        purchaseItem(skin.priceCoins, skin.id) {
                            AppearanceManager.equipPieceSkin(skin)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BoardMaterialsTab(
    currentEquipped: BoardMaterial,
    unlockedItems: Set<String>,
    onEquip: (BoardMaterial) -> Unit,
    onBuy: (BoardMaterial) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(AppearanceManager.ALL_BOARD_MATERIALS) { material ->
            val isUnlocked = unlockedItems.contains(material.id)
            val isEquipped = currentEquipped.id == material.id

            BoardMaterialCard(
                material = material,
                isUnlocked = isUnlocked,
                isEquipped = isEquipped,
                onEquip = { onEquip(material) },
                onBuy = { onBuy(material) }
            )
        }
    }
}

@Composable
private fun BoardMaterialCard(
    material: BoardMaterial,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                brush = if (isEquipped) Brush.linearGradient(listOf(GoldPrimary, GoldSecondary))
                else Brush.linearGradient(listOf(CardBorder, CardBorder)),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("material_card_${material.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.94f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = material.previewIcon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = material.nameAr,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = material.nameEn,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(PointHighlight, Color(0xFF2E7D32))))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مُفعل",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = material.descriptionAr,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3D Texture Preview Swatch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(material.woodDark)
                    .border(1.5.dp, material.borderAccent, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Felt Center
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(material.feltBackground)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Points Swatch
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(material.pointDark)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(material.pointLight)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when {
                    isEquipped -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF2C3238),
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("الرقعة المفعلة", fontSize = 13.sp)
                        }
                    }
                    isUnlocked -> {
                        Button(
                            onClick = onEquip,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تفعيل الرقعة", color = BoardWoodDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onBuy,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شراء مقابل ${material.priceCoins} 🪙",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceSkinsTab(
    currentEquipped: PieceSkin,
    unlockedItems: Set<String>,
    onEquip: (PieceSkin) -> Unit,
    onBuy: (PieceSkin) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(AppearanceManager.ALL_PIECE_SKINS) { skin ->
            val isUnlocked = unlockedItems.contains(skin.id)
            val isEquipped = currentEquipped.id == skin.id

            PieceSkinCard(
                skin = skin,
                isUnlocked = isUnlocked,
                isEquipped = isEquipped,
                onEquip = { onEquip(skin) },
                onBuy = { onBuy(skin) }
            )
        }
    }
}

@Composable
private fun PieceSkinCard(
    skin: PieceSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                brush = if (isEquipped) Brush.linearGradient(listOf(GoldPrimary, GoldSecondary))
                else Brush.linearGradient(listOf(CardBorder, CardBorder)),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("skin_card_${skin.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.94f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = skin.previewIcon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = skin.nameAr,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = skin.nameEn,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(PointHighlight, Color(0xFF2E7D32))))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مُفعل",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = skin.descriptionAr,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3D Checkers Visual Swatch (White & Black pieces preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14100C))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White Piece Preview
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(skin.whiteBaseColors))
                        .border(2.dp, skin.whiteRimColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(28.dp))

                Text(text = "ضد", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(28.dp))

                // Black Piece Preview
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(skin.blackBaseColors))
                        .border(2.dp, skin.blackRimColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when {
                    isEquipped -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF2C3238),
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("المظهر المفعل", fontSize = 13.sp)
                        }
                    }
                    isUnlocked -> {
                        Button(
                            onClick = onEquip,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تفعيل المظهر", color = BoardWoodDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onBuy,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شراء مقابل ${skin.priceCoins} 🪙",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
