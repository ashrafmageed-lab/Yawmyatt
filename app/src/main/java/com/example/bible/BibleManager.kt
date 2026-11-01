package com.example.bible

import android.content.Context
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

enum class BibleScope(val label: String) {
    ALL("كل الكتاب (٦٦ سفر)"),
    NEW_TESTAMENT("العهد الجديد (٢٧)"),
    OLD_TESTAMENT("العهد القديم (٣٩)"),
    PSALMS_PROVERBS("المزامير والأمثال"),
    GOSPELS("الأناجيل الأربعة")
}

data class BibleVerseHit(
    val bookIndex: Int,
    val chapterIndex: Int,
    val verseIndex: Int,
    val bookName: String,
    val reference: String,
    val text: String,
    val isExactMatch: Boolean = false
) {
    fun toJournalQuote(): String = "«${text.trim()}»\n— $reference"
}

object BibleManager {

    val BOOKS: List<String> = listOf(
        "التكوين", "الخروج", "اللاويين", "العدد", "التثنية", "يشوع", "القضاة", "راعوث",
        "صموئيل الأول", "صموئيل الثاني", "ملوك الأول", "ملوك الثاني", "أخبار الأيام الأول", "أخبار الأيام الثاني",
        "عزرا", "نحميا", "أستير", "أيوب", "المزامير", "الأمثال", "الجامعة", "نشيد الأنشاد",
        "إشعياء", "إرميا", "مراثي إرميا", "حزقيال", "دانيال", "هوشع", "يوئيل", "عاموس", "عوبديا",
        "يونان", "ميخا", "ناحوم", "حبقوق", "صفنيا", "حجي", "زكريا", "ملاخي",
        "متى", "مرقس", "لوقا", "يوحنا", "أعمال الرسل", "رومية", "كورنثوس الأولى", "كورنثوس الثانية",
        "غلاطية", "أفسس", "فيلبي", "كولوسي", "تسالونيكي الأولى", "تسالونيكي الثانية", "تيموثاوس الأولى",
        "تيموثاوس الثانية", "تيطس", "فليمون", "العبرانيين", "يعقوب", "بطرس الأولى", "بطرس الثانية",
        "يوحنا الأولى", "يوحنا الثانية", "يوحنا الثالثة", "يهوذا", "رؤيا يوحنا"
    )

    private var cachedBible: List<List<List<String>>>? = null

    suspend fun loadBible(context: Context): List<List<List<String>>> = withContext(Dispatchers.IO) {
        cachedBible?.let { return@withContext it }

        val result = mutableListOf<List<List<String>>>()
        try {
            context.assets.open("bible-svd.json").use { stream ->
                JsonReader(InputStreamReader(stream, "UTF-8")).use { reader ->
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val book = mutableListOf<List<String>>()
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val chapter = mutableListOf<String>()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                chapter.add(reader.nextString())
                            }
                            reader.endArray()
                            book.add(chapter)
                        }
                        reader.endArray()
                        result.add(book)
                    }
                    reader.endArray()
                }
            }
            cachedBible = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun isLoaded(): Boolean = cachedBible != null

    fun getBible(): List<List<List<String>>>? = cachedBible

    fun stripArabic(input: String): String {
        return input
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "") // tashkeel
            .replace("ـ", "") // tatweel
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
    }

    private fun matchesScope(bookIndex: Int, scope: BibleScope): Boolean {
        return when (scope) {
            BibleScope.ALL -> true
            BibleScope.OLD_TESTAMENT -> bookIndex in 0..38
            BibleScope.NEW_TESTAMENT -> bookIndex in 39..65
            BibleScope.PSALMS_PROVERBS -> bookIndex == 18 || bookIndex == 19
            BibleScope.GOSPELS -> bookIndex in 39..42
        }
    }

    /**
     * Resolves references such as "مزمور 23", "يوحنا 3:16", "رو 8:28", "متى 6".
     */
    private fun tryResolveReference(cleanQuery: String, bible: List<List<List<String>>>): List<BibleVerseHit>? {
        val parts = cleanQuery.split(Regex("[\\s:]+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null

        // Last or second-to-last part might be chapter and verse numbers
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        if (numbers.isEmpty()) return null

        val chapterNum = numbers[0]
        val verseNum = if (numbers.size > 1) numbers[1] else null

        // Non-number tokens represent book name
        val bookWords = parts.filter { it.toIntOrNull() == null }
        if (bookWords.isEmpty()) return null
        val searchedBook = bookWords.joinToString(" ")

        var matchedBookIdx = -1
        for (i in BOOKS.indices) {
            val cleanBook = stripArabic(BOOKS[i])
            val altBook = cleanBook.removePrefix("ال")
            if (cleanBook == searchedBook || altBook == searchedBook ||
                cleanBook.startsWith(searchedBook) || altBook.startsWith(searchedBook) ||
                (searchedBook == "مزمور" && i == 18) ||
                (searchedBook == "انجيل يوحنا" && i == 42) ||
                (searchedBook == "انجيل متي" && i == 39) ||
                (searchedBook == "انجيل مرقس" && i == 40) ||
                (searchedBook == "انجيل لوقا" && i == 41) ||
                (searchedBook == "رو" && i == 44) ||
                (searchedBook == "مت" && i == 39) ||
                (searchedBook == "مر" && i == 40) ||
                (searchedBook == "لو" && i == 41) ||
                (searchedBook == "يو" && i == 42) ||
                (searchedBook == "تك" && i == 0) ||
                (searchedBook == "خر" && i == 1) ||
                (searchedBook == "مز" && i == 18)
            ) {
                matchedBookIdx = i
                break
            }
        }

        if (matchedBookIdx == -1) return null

        val book = bible.getOrNull(matchedBookIdx) ?: return null
        val cIdx = chapterNum - 1
        if (cIdx !in book.indices) return null

        val chapter = book[cIdx]
        val bName = BOOKS.getOrElse(matchedBookIdx) { "" }

        if (verseNum != null) {
            val vIdx = verseNum - 1
            if (vIdx in chapter.indices) {
                val verseText = chapter[vIdx]
                return listOf(
                    BibleVerseHit(
                        bookIndex = matchedBookIdx,
                        chapterIndex = cIdx,
                        verseIndex = vIdx,
                        bookName = bName,
                        reference = "$bName ${chapterNum}:${verseNum}",
                        text = verseText,
                        isExactMatch = true
                    )
                )
            }
        } else {
            // Return all verses of that chapter
            return chapter.mapIndexed { vIdx, verseText ->
                BibleVerseHit(
                    bookIndex = matchedBookIdx,
                    chapterIndex = cIdx,
                    verseIndex = vIdx,
                    bookName = bName,
                    reference = "$bName ${chapterNum}:${vIdx + 1}",
                    text = verseText,
                    isExactMatch = true
                )
            }
        }
        return null
    }

    suspend fun search(
        query: String,
        scope: BibleScope = BibleScope.ALL,
        maxHits: Int = 60
    ): List<BibleVerseHit> = withContext(Dispatchers.Default) {
        val bible = cachedBible ?: return@withContext emptyList()
        val rawQuery = query.trim()
        val cleanQuery = stripArabic(rawQuery)
        if (cleanQuery.length < 2) return@withContext emptyList()

        // 1. Check if user typed a specific reference (e.g. "مزمور 23" or "يوحنا 3:16")
        val refHits = tryResolveReference(cleanQuery, bible)
        if (refHits != null && refHits.isNotEmpty()) {
            return@withContext refHits
        }

        // 2. Full text search across books matching scope
        val queryWords = cleanQuery.split(Regex("\\s+")).filter { it.length >= 2 }
        val exactHits = mutableListOf<BibleVerseHit>()
        val multiWordHits = mutableListOf<BibleVerseHit>()

        for (b in bible.indices) {
            if (!matchesScope(b, scope)) continue
            val book = bible[b]
            val bName = BOOKS.getOrElse(b) { "سفر ${b + 1}" }

            for (c in book.indices) {
                val chapter = book[c]
                for (v in chapter.indices) {
                    val verseText = chapter[v]
                    val cleanVerse = stripArabic(verseText)

                    // Check exact phrase match
                    if (cleanVerse.contains(cleanQuery, ignoreCase = true)) {
                        exactHits.add(
                            BibleVerseHit(
                                bookIndex = b,
                                chapterIndex = c,
                                verseIndex = v,
                                bookName = bName,
                                reference = "$bName ${c + 1}:${v + 1}",
                                text = verseText,
                                isExactMatch = true
                            )
                        )
                        if (exactHits.size >= maxHits) {
                            return@withContext exactHits
                        }
                    } else if (queryWords.size > 1 && queryWords.all { cleanVerse.contains(it, ignoreCase = true) }) {
                        // All words match in the verse
                        multiWordHits.add(
                            BibleVerseHit(
                                bookIndex = b,
                                chapterIndex = c,
                                verseIndex = v,
                                bookName = bName,
                                reference = "$bName ${c + 1}:${v + 1}",
                                text = verseText,
                                isExactMatch = false
                            )
                        )
                    }
                }
            }
        }

        val combined = (exactHits + multiWordHits).take(maxHits)
        combined
    }
}
