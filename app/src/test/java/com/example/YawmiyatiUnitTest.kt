package com.example

import com.example.bible.BibleManager
import com.example.data.model.JournalItem
import com.example.data.repository.YawmiyatiRepository
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class YawmiyatiUnitTest {

    @Test
    fun testArabicTashkeelStripping() {
        val original = "فِي ٱلْبَدْءِ خَلَقَ ٱللهُ ٱلسَّمَاوَاتِ وَٱلْأَرْضَ."
        val stripped = BibleManager.stripArabic(original)
        assertTrue(!stripped.contains("ْ"))
        assertTrue(!stripped.contains("َ"))
        assertTrue(!stripped.contains("ِ"))
        assertTrue(stripped.contains("في"))
        assertTrue(stripped.contains("البدء"))
    }

    @Test
    fun testJournalItemSerializationRoundTrip() {
        val items = listOf(
            JournalItem(t = "الصلاة الصباحية", d = true),
            JournalItem(t = "قراءة مزمور ٢٣", d = false)
        )
        val json = YawmiyatiRepository.serializeItems(items)
        val parsed = YawmiyatiRepository.parseItems(json)

        assertEquals(2, parsed.size)
        assertEquals("الصلاة الصباحية", parsed[0].t)
        assertTrue(parsed[0].d)
        assertEquals("قراءة مزمور ٢٣", parsed[1].t)
        assertTrue(!parsed[1].d)
    }



    @Test
    fun testAesGcmEncryptionDecryptionRoundTrip() {
        val originalData = """{"v":1,"note":"تأمل في وعود الرب"}"""
        val password = "mySecretPassword123"

        val encryptedJson = SecurityManager.encryptText(originalData, password)
        assertTrue(encryptedJson.contains("\"enc\":true"))

        val decryptedData = SecurityManager.decryptText(encryptedJson, password)
        assertEquals(originalData, decryptedData)
    }

    @Test
    fun testReminderSettingsSaveAndRetrieve() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = com.example.reminder.ReminderSettings(
            enabled = true,
            dailyHour = 21,
            dailyMinute = 15,
            mode = "daily"
        )
        com.example.reminder.ReminderManager.saveSettings(context, settings)

        val retrieved = com.example.reminder.ReminderManager.getSettings(context)
        assertTrue(retrieved.enabled)
        assertEquals(21, retrieved.dailyHour)
        assertEquals(15, retrieved.dailyMinute)
        assertEquals("daily", retrieved.mode)
    }

    @Test
    fun testOfflineBibleSearchAndQuoteFormatting() = kotlinx.coroutines.runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val bible = BibleManager.loadBible(context)
        assertTrue(bible.isNotEmpty())
        assertEquals(66, bible.size)

        // Test searching by keyword "سلام"
        val hits = BibleManager.search("سلام")
        assertTrue(hits.isNotEmpty())
        val firstHit = hits.first()
        assertTrue(firstHit.reference.isNotBlank())
        assertTrue(firstHit.text.isNotBlank())

        // Test quote formatting
        val quote = firstHit.toJournalQuote()
        assertTrue(quote.startsWith("«"))
        assertTrue(quote.contains(firstHit.reference))

        // Test searching by reference "مزمور 23"
        val psalmHits = BibleManager.search("مزمور 23")
        assertTrue(psalmHits.isNotEmpty())
        assertEquals("المزامير", psalmHits[0].bookName)
        assertEquals(22, psalmHits[0].chapterIndex) // Chapter 23 is 0-indexed 22
    }

    @Test
    fun testEntryEntityFavoriteAndMoodTag() {
        val favEntry = com.example.data.model.EntryEntity(
            kind = "journal",
            text = "تأمل هادئ ومميز",
            ansNote = "fav",
            ansDate = "سلام عميق 🕊️"
        )
        assertTrue(favEntry.isFavorite)
        assertEquals("سلام عميق 🕊️", favEntry.moodTag)

        val nonFavEntry = com.example.data.model.EntryEntity(
            kind = "journal",
            text = "تأمل عادي",
            ansNote = null,
            ptext = "شكر وامتنان 🌾"
        )
        assertTrue(!nonFavEntry.isFavorite)
        assertEquals("شكر وامتنان 🌾", nonFavEntry.moodTag)
    }

    @Test
    fun testStreakCalculation() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)

        val days = listOf(
            com.example.data.model.DayEntity(date = today, note = "صلاة اليوم"),
            com.example.data.model.DayEntity(date = yesterday, note = "شكر الأمس")
        )

        val streak = com.example.ui.viewmodel.YawmiyatiViewModel.calculateStreakDays(
            days,
            emptyList<com.example.data.model.EntryEntity>()
        )
        assertEquals(2, streak)
    }

    @Test
    fun testJournalSearchFilteringByContent() {
        val entries = listOf(
            com.example.data.model.EntryEntity(
                id = 1,
                kind = "journal",
                text = "جلسة هادئة في المخدع، شعرت بسلام عميق وشكرت الرب على نعمته العظيمة.",
                step = "تأمل المساء",
                ref = "2026-11-01 21:00",
                verse = "«سلاما أترك لكم سلامي أعطيكم»",
                ptext = "يوحنا 14:27"
            ),
            com.example.data.model.EntryEntity(
                id = 2,
                kind = "journal",
                text = "اليوم واجهت ضيقاً في العمل لكن تذكرت أن الرب راعيّ فلا يعوزني شيء.",
                step = "رجاء وأمل",
                ref = "2026-11-02 20:30",
                verse = "«الرب راعي فلا يعوزني شيء»",
                ptext = "مزمور 23:1"
            )
        )

        // Query matching first entry text
        val query1 = "المخدع"
        val results1 = entries.filter { it.text.contains(query1, ignoreCase = true) }
        assertEquals(1, results1.size)
        assertEquals(1L, results1[0].id)

        // Query matching verse reference in second entry
        val query2 = "مزمور"
        val results2 = entries.filter {
            it.text.contains(query2, ignoreCase = true) ||
            it.ptext.contains(query2, ignoreCase = true) ||
            it.verse.contains(query2, ignoreCase = true)
        }
        assertEquals(1, results2.size)
        assertEquals(2L, results2[0].id)

        // Query matching title/step
        val query3 = "تأمل المساء"
        val results3 = entries.filter { it.step.contains(query3, ignoreCase = true) }
        assertEquals(1, results3.size)
        assertEquals(1L, results3[0].id)

        // Query with no matches
        val query4 = "كلمةغيرموجودة"
        val results4 = entries.filter { it.text.contains(query4, ignoreCase = true) }
        assertTrue(results4.isEmpty())
    }

    @Test
    fun testEveningReminderDefaultSettingsAndNotification() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        // Save explicit evening settings
        val eveningSettings = com.example.reminder.ReminderSettings(
            enabled = true,
            dailyHour = 21, // 9:00 PM evening
            dailyMinute = 0,
            mode = "daily"
        )
        com.example.reminder.ReminderManager.saveSettings(context, eveningSettings)

        val retrieved = com.example.reminder.ReminderManager.getSettings(context)
        assertTrue(retrieved.enabled)
        assertEquals(21, retrieved.dailyHour)
        assertEquals(0, retrieved.dailyMinute)
        assertEquals("daily", retrieved.mode)

        // Trigger immediate test notification to verify channel creation and posting
        com.example.reminder.ReminderManager.sendEveningTestReminder(context)
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(com.example.reminder.ReminderReceiver.CHANNEL_ID)
            assertTrue(channel != null)
        }
    }
}
