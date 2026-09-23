package com.example.game

import android.content.Context
import android.content.SharedPreferences
import com.example.domino.model.DominoTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GameType {
    BACKGAMMON,
    DOMINOES
}

data class ReplayStep(
    val stepIndex: Int,
    val player: String,
    val commentary: String,
    val dice: List<Int> = emptyList(),
    val fromPoint: Int? = null,
    val toPoint: Int? = null,
    val isHit: Boolean = false,
    val isBearOff: Boolean = false,
    val dominoTileText: String? = null,
    val dominoEndText: String? = null
)

data class GameRecord(
    val id: String,
    val gameType: GameType,
    val title: String,
    val opponentName: String,
    val isWin: Boolean,
    val finalScore: String,
    val dateString: String,
    val steps: List<ReplayStep>
)

object GameHistoryManager {

    private const val PREFS_NAME = "royal_game_history_prefs"
    private const val KEY_RECORDS_JSON = "saved_game_records_json"

    private var prefs: SharedPreferences? = null

    private val _records = MutableStateFlow<List<GameRecord>>(emptyList())
    val records: StateFlow<List<GameRecord>> = _records.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadRecords()
        }
    }

    private fun loadRecords() {
        val sp = prefs ?: return
        val rawJson = sp.getString(KEY_RECORDS_JSON, null)
        if (rawJson.isNullOrEmpty()) {
            // Seed with spectacular championship demonstration replays
            val initialSeeds = createChampionshipDemoGames()
            _records.value = initialSeeds
            saveRecords(initialSeeds)
        } else {
            try {
                val list = mutableListOf<GameRecord>()
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(parseGameRecord(obj))
                }
                _records.value = list
            } catch (e: Exception) {
                _records.value = createChampionshipDemoGames()
            }
        }
    }

    fun saveCompletedGame(
        gameType: GameType,
        title: String,
        opponentName: String,
        isWin: Boolean,
        finalScore: String,
        steps: List<ReplayStep>
    ) {
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        val newRecord = GameRecord(
            id = "rec_${System.currentTimeMillis()}",
            gameType = gameType,
            title = title,
            opponentName = opponentName,
            isWin = isWin,
            finalScore = finalScore,
            dateString = dateStr,
            steps = steps
        )

        val updated = listOf(newRecord) + _records.value.take(19) // Keep last 20 games
        _records.value = updated
        saveRecords(updated)
    }

    private fun saveRecords(list: List<GameRecord>) {
        try {
            val arr = JSONArray()
            for (rec in list) {
                arr.put(serializeGameRecord(rec))
            }
            prefs?.edit()?.putString(KEY_RECORDS_JSON, arr.toString())?.apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun serializeGameRecord(rec: GameRecord): JSONObject {
        val obj = JSONObject()
        obj.put("id", rec.id)
        obj.put("gameType", rec.gameType.name)
        obj.put("title", rec.title)
        obj.put("opponentName", rec.opponentName)
        obj.put("isWin", rec.isWin)
        obj.put("finalScore", rec.finalScore)
        obj.put("dateString", rec.dateString)

        val stepsArr = JSONArray()
        for (s in rec.steps) {
            val sObj = JSONObject()
            sObj.put("stepIndex", s.stepIndex)
            sObj.put("player", s.player)
            sObj.put("commentary", s.commentary)
            sObj.put("dice", JSONArray(s.dice))
            s.fromPoint?.let { sObj.put("fromPoint", it) }
            s.toPoint?.let { sObj.put("toPoint", it) }
            sObj.put("isHit", s.isHit)
            sObj.put("isBearOff", s.isBearOff)
            s.dominoTileText?.let { sObj.put("dominoTileText", it) }
            s.dominoEndText?.let { sObj.put("dominoEndText", it) }
            stepsArr.put(sObj)
        }
        obj.put("steps", stepsArr)
        return obj
    }

    private fun parseGameRecord(obj: JSONObject): GameRecord {
        val stepsList = mutableListOf<ReplayStep>()
        val sArr = obj.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val sObj = sArr.getJSONObject(i)
            val diceArr = sObj.optJSONArray("dice") ?: JSONArray()
            val diceList = mutableListOf<Int>()
            for (d in 0 until diceArr.length()) {
                diceList.add(diceArr.getInt(d))
            }
            stepsList.add(
                ReplayStep(
                    stepIndex = sObj.getInt("stepIndex"),
                    player = sObj.getString("player"),
                    commentary = sObj.getString("commentary"),
                    dice = diceList,
                    fromPoint = if (sObj.has("fromPoint")) sObj.getInt("fromPoint") else null,
                    toPoint = if (sObj.has("toPoint")) sObj.getInt("toPoint") else null,
                    isHit = sObj.optBoolean("isHit", false),
                    isBearOff = sObj.optBoolean("isBearOff", false),
                    dominoTileText = sObj.optString("dominoTileText", null),
                    dominoEndText = sObj.optString("dominoEndText", null)
                )
            )
        }

        return GameRecord(
            id = obj.getString("id"),
            gameType = GameType.valueOf(obj.getString("gameType")),
            title = obj.getString("title"),
            opponentName = obj.getString("opponentName"),
            isWin = obj.getBoolean("isWin"),
            finalScore = obj.getString("finalScore"),
            dateString = obj.getString("dateString"),
            steps = stepsList
        )
    }

    private fun createChampionshipDemoGames(): List<GameRecord> {
        val backgammonSteps = listOf(
            ReplayStep(1, "أنت", "افتتاحية سريعة: رمية [6-4]، تقدم القطعة من النقطة 24 إلى 14 لبناء جسر أمان.", listOf(6, 4), 24, 14),
            ReplayStep(2, "الذكاء الاصطناعي", "رد الخصم: رمية [5-3]، بناء سد دفاعي حصين على النقطة 8.", listOf(5, 3), 13, 8),
            ReplayStep(3, "أنت", "رمية [3-1]: تحصين النقطة 5 وبناء جدار أمان ذهبي في بيتك الداخلي.", listOf(3, 1), 6, 5),
            ReplayStep(4, "الذكاء الاصطناعي", "الخصم يترك بلطة مكشوفة على النقطة 10 بتهور!", listOf(4, 2), 12, 10),
            ReplayStep(5, "أنت", "ضربة معلم! رمية [5-2]: صيد بلطة الخصم على النقطة 10 وإرسالها للحاجز (Bar)!", listOf(5, 2), 15, 10, isHit = true),
            ReplayStep(6, "الذكاء الاصطناعي", "الخصم محبوس على الحاجز! رمية فاشلة تمنعه من الدخول.", listOf(6, 6)),
            ReplayStep(7, "أنت", "دبل ملكي أسطوري [4-4-4-4]! تقدم كاسح بـ4 قطع مباشرة نحو البيت.", listOf(4, 4, 4, 4), 14, 10),
            ReplayStep(8, "أنت", "مرحلة الحسم (Bearing Off): إخراج القطعة الأخيرة بالنرد [6] وإعلان الفوز الكاسح (Gammon)!", listOf(6, 1), 6, -1, isBearOff = true)
        )

        val dominoSteps = listOf(
            ReplayStep(1, "أنت", "افتتاحية قوية بالدومينو المزدوج [6|6] في منتصف الطاولة الملكية.", dominoTileText = "[6|6]", dominoEndText = "CENTER"),
            ReplayStep(2, "الخصم", "الخصم يلعب [6|4] متصلاً بالطرف الأيمن.", dominoTileText = "[6|4]", dominoEndText = "RIGHT"),
            ReplayStep(3, "أنت", "لعب قطعة تكتيكية [6|2] وإجبار الخصم على السحب من البنك!", dominoTileText = "[6|2]", dominoEndText = "LEFT"),
            ReplayStep(4, "الخصم", "الخصم يسحب مرتين من البنك ويعجز عن اللعب (Pass)!", dominoTileText = "PASS"),
            ReplayStep(5, "أنت", "حركة الإغلاق التاريخية (تكفيل): لعب [2|4] وإعلان الفوز بـ15 نقطة صافية!", dominoTileText = "[2|4]", dominoEndText = "RIGHT")
        )

        return listOf(
            GameRecord(
                id = "champ_bg_01",
                gameType = GameType.BACKGAMMON,
                title = "نهائي بطولة الشرق الأوسط 2026 🏆",
                opponentName = "الماستر الملكي (Grandmaster)",
                isWin = true,
                finalScore = "15 - 4 (فوز كامل)",
                dateString = "2026/09/20 21:30",
                steps = backgammonSteps
            ),
            GameRecord(
                id = "champ_dom_01",
                gameType = GameType.DOMINOES,
                title = "مباراة الدومينو الذهبية - تكفيل حاسم 🎴",
                opponentName = "سلطان النرد",
                isWin = true,
                finalScore = "105 - 40",
                dateString = "2026/09/18 19:15",
                steps = dominoSteps
            )
        )
    }
}
