package com.example.pantryfa

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// کد خالص (بدون وابستگی به اندروید) برای پیدا کردن تاریخ در متن خوانده‌شده از روی بسته

enum class DateKind { EXPIRY, PRODUCTION, UNKNOWN }

data class DateCandidate(
    val date: LocalDate,            // تاریخ میلادی
    val kind: DateKind,
    val raw: String,                // متنی که این تاریخ از آن خوانده شد
    val calendar: String,           // «شمسی» یا «میلادی» (فرمتی که روی بسته چاپ شده)
    val alternatives: List<LocalDate>,  // برداشت‌های دیگرِ ممکن از همان متن
    val guess: Boolean = false      // نوع تاریخ حدسی است
) {
    val key: String get() = date.toString()
}

private class Rd(val date: LocalDate, val cal: Char, val pref: Int)

private class Pat(
    val re: Regex,
    val needsKw: Boolean,
    val build: (MatchResult, Boolean) -> List<Rd>
)

private class Kw(val start: Int, val end: Int, val kind: DateKind)

object DateOcr {

    private val SEP = """(?:\s?[/\\.\-,:]\s?|\s)"""
    private const val MONNAMES = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEPT|SEP|OCT|NOV|DEC"
    private val MONRE = "($MONNAMES)[A-Z]*"

    private val monthNum = mapOf(
        "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
        "JUL" to 7, "AUG" to 8, "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
    )

    private val EXP_RE =
        Regex("""\b(BEST BEFORE|USE BY|EXPIRATION|EXPIRES|EXPIRY|EXPIRE|EXP|BBE|BBD|BB)\b""")
    private val PRD_RE =
        Regex("""\b(MANUFACTURED|MANUFACTURE|PRODUCTION|PRODUCED|MFG|MFD|PKD|PACKED|PROD|DOM|PD)\b""")

    private val pats: List<Pat> = listOf(
        Pat(Regex("""(?<!\d)(\d{1,2})$SEP?$MONRE$SEP?(\d{4}|\d{2})(?!\d)"""), false, ::bDmony),
        Pat(Regex("""(?<![A-Z])$MONRE\s?(\d{1,2})\s?[,.]?\s?(\d{4})(?!\d)"""), false, ::bMondy),
        Pat(Regex("""(?<![A-Z])$MONRE$SEP?(\d{4}|\d{2})(?!\d)"""), false, ::bMony),
        Pat(Regex("""(?<!\d)(\d{4})$SEP(\d{1,2})$SEP(\d{1,2})(?!\d)"""), false, ::bYmd),
        Pat(Regex("""(?<!\d)(\d{1,2})$SEP(\d{1,2})$SEP(\d{4})(?!\d)"""), false, ::bDmy),
        Pat(Regex("""(?<!\d)(\d{8})(?!\d)"""), false, ::b8),
        Pat(Regex("""(?<!\d)(\d{1,2})$SEP(\d{4})(?!\d)"""), false, ::bMy),
        Pat(Regex("""(?<!\d)(\d{4})$SEP(\d{1,2})(?!\d)"""), false, ::bYm),
        Pat(Regex("""(?<!\d)(\d{2})$SEP(\d{2})$SEP(\d{2})(?!\d)"""), false, ::b3x2),
        Pat(Regex("""(?<!\d)(\d{1,2})$SEP(\d{2})(?!\d)"""), true, ::bMy2),
        Pat(Regex("""(?<!\d)(\d{6})(?!\d)"""), true, ::b6)
    )

    // ---------- تمیزکاری متن ----------

    private val CONF = "OQDILZSGB"

    private fun fixDigits(t: String): String {
        if (t.none { it.isDigit() }) return t
        if (!t.all { it.isDigit() || it in CONF }) return t
        val sb = StringBuilder()
        for (ch in t) {
            when (ch) {
                'O', 'Q', 'D' -> sb.append('0')
                'I', 'L' -> sb.append('1')
                'Z' -> sb.append('2')
                'S' -> sb.append('5')
                'G' -> sb.append('6')
                'B' -> sb.append('8')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val GLUED =
        Regex("^(EXPIRY|EXPIRES|EXPIRE|EXP|BBE|BBD|BB|MFG|MFD|PKD|PROD|DOM)(.+)$")

    private fun fixToken(t: String): String {
        val m = GLUED.find(t)
        if (m != null && m.groupValues[2].any { it.isDigit() }) {
            return m.groupValues[1] + " " + fixDigits(m.groupValues[2])
        }
        return fixDigits(t)
    }

    fun normalizeLine(raw: String): String {
        var s = normDigits(raw).uppercase()
        s = s.replace(Regex("(\\d)($MONNAMES)"), "$1 $2")
        s = s.replace(Regex("($MONNAMES)(\\d)"), "$1 $2")
        s = Regex("[A-Z0-9]+").replace(s) { fixToken(it.value) }
        return s
    }

    // ---------- ساخت تاریخ ----------

    private fun years(y: Int, two: Boolean): List<Pair<Char, Int>> {
        if (two) return listOf('G' to 2000 + y, 'J' to 1400 + y)
        if (y in 1300..1499) return listOf('J' to y)
        if (y in 1900..2199) return listOf('G' to y)
        return emptyList()
    }

    private fun mk(cal: Char, y: Int, m: Int, d: Int): LocalDate? {
        if (m !in 1..12 || d !in 1..31) return null
        if (cal == 'J') return Jalali.parse("$y/$m/$d")
        return try {
            LocalDate.of(y, m, d)
        } catch (e: Exception) {
            null
        }
    }

    private fun mkEnd(cal: Char, y: Int, m: Int, eom: Boolean): LocalDate? {
        if (m !in 1..12) return null
        if (!eom) return mk(cal, y, m, 1)
        if (cal == 'J') return mk('J', y, m, monthLen(y, m))
        return try {
            YearMonth.of(y, m).atEndOfMonth()
        } catch (e: Exception) {
            null
        }
    }

    private fun add(out: MutableList<Rd>, cal: Char, d: LocalDate?, pref: Int) {
        if (d != null) out.add(Rd(d, cal, pref))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun bDmony(m: MatchResult, eom: Boolean): List<Rd> {
        val d = m.groupValues[1].toInt()
        val mon = monthNum[m.groupValues[2].take(3)] ?: return emptyList()
        val ys = m.groupValues[3]
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(ys.toInt(), ys.length == 2)) add(out, cal, mk(cal, yy, mon, d), 5)
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun bMondy(m: MatchResult, eom: Boolean): List<Rd> {
        val mon = monthNum[m.groupValues[1].take(3)] ?: return emptyList()
        val d = m.groupValues[2].toInt()
        val y = m.groupValues[3].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, false)) add(out, cal, mk(cal, yy, mon, d), 4)
        return out
    }

    private fun bMony(m: MatchResult, eom: Boolean): List<Rd> {
        val mon = monthNum[m.groupValues[1].take(3)] ?: return emptyList()
        val ys = m.groupValues[2]
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(ys.toInt(), ys.length == 2)) add(out, cal, mkEnd(cal, yy, mon, eom), 5)
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun bYmd(m: MatchResult, eom: Boolean): List<Rd> {
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, false)) add(out, cal, mk(cal, yy, mo, d), 4)
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun bDmy(m: MatchResult, eom: Boolean): List<Rd> {
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[2].toInt()
        val y = m.groupValues[3].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, false)) {
            add(out, cal, mk(cal, yy, b, a), 4)   // روز/ماه/سال
            add(out, cal, mk(cal, yy, a, b), 3)   // ماه/روز/سال
        }
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun b8(m: MatchResult, eom: Boolean): List<Rd> {
        val s = m.groupValues[1]
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(s.substring(0, 4).toInt(), false)) {
            add(out, cal, mk(cal, yy, s.substring(4, 6).toInt(), s.substring(6, 8).toInt()), 3)
        }
        for ((cal, yy) in years(s.substring(4, 8).toInt(), false)) {
            add(out, cal, mk(cal, yy, s.substring(2, 4).toInt(), s.substring(0, 2).toInt()), 3)
        }
        return out
    }

    private fun bMy(m: MatchResult, eom: Boolean): List<Rd> {
        val mo = m.groupValues[1].toInt()
        val y = m.groupValues[2].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, false)) add(out, cal, mkEnd(cal, yy, mo, eom), 3)
        return out
    }

    private fun bYm(m: MatchResult, eom: Boolean): List<Rd> {
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, false)) add(out, cal, mkEnd(cal, yy, mo, eom), 3)
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun b3x2(m: MatchResult, eom: Boolean): List<Rd> {
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[2].toInt()
        val c = m.groupValues[3].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(c, true)) {
            add(out, cal, mk(cal, yy, b, a), 3)   // روز/ماه/سال
            add(out, cal, mk(cal, yy, a, b), 2)   // ماه/روز/سال
        }
        for ((cal, yy) in years(a, true)) {
            add(out, cal, mk(cal, yy, b, c), 2)   // سال/ماه/روز
        }
        return out
    }

    private fun bMy2(m: MatchResult, eom: Boolean): List<Rd> {
        val mo = m.groupValues[1].toInt()
        val y = m.groupValues[2].toInt()
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(y, true)) add(out, cal, mkEnd(cal, yy, mo, eom), 2)
        return out
    }

    @Suppress("UNUSED_PARAMETER")
    private fun b6(m: MatchResult, eom: Boolean): List<Rd> {
        val s = m.groupValues[1]
        val out = ArrayList<Rd>()
        for ((cal, yy) in years(s.substring(4, 6).toInt(), true)) {
            add(out, cal, mk(cal, yy, s.substring(2, 4).toInt(), s.substring(0, 2).toInt()), 2)
        }
        for ((cal, yy) in years(s.substring(0, 2).toInt(), true)) {
            add(out, cal, mk(cal, yy, s.substring(2, 4).toInt(), s.substring(4, 6).toInt()), 2)
        }
        return out
    }

    // ---------- کلمات کلیدی ----------

    private fun keywords(line: String): List<Kw> {
        val out = ArrayList<Kw>()
        for (m in EXP_RE.findAll(line)) out.add(Kw(m.range.first, m.range.last + 1, DateKind.EXPIRY))
        for (m in PRD_RE.findAll(line)) out.add(Kw(m.range.first, m.range.last + 1, DateKind.PRODUCTION))
        return out
    }

    private fun kindFor(kws: List<Kw>, prev: List<Kw>, prevLine: String?, start: Int, end: Int): DateKind {
        val before = kws.filter { it.end <= start && start - it.end <= 25 }.maxByOrNull { it.end }
        if (before != null) return before.kind
        if (prevLine != null && prev.isNotEmpty() && start <= 6) {
            val last = prev.maxByOrNull { it.end }!!
            if (prevLine.length - last.end <= 4) return last.kind
        }
        val after = kws.filter { it.start >= end && it.start - end <= 15 }.minByOrNull { it.start }
        if (after != null) return after.kind
        return DateKind.UNKNOWN
    }

    // ---------- انتخاب بهترین برداشت ----------

    private fun choose(rds: List<Rd>, today: LocalDate, kind: DateKind, raw: String): DateCandidate? {
        val scored = ArrayList<Pair<Rd, Int>>()
        for (r in rds) {
            val days = ChronoUnit.DAYS.between(today, r.date)
            if (days < -3650 || days > 4380) continue
            val inWin = if (kind == DateKind.PRODUCTION) days in -3650..30 else days in -400..3650
            val typical = kind != DateKind.PRODUCTION && days in -60..1100
            scored.add(Pair(r, r.pref + (if (inWin) 10 else 0) + (if (typical) 3 else 0)))
        }
        if (scored.isEmpty()) return null
        scored.sortByDescending { it.second }
        val best = scored[0].first
        val alts = scored
            .filter { it.first.date != best.date && it.second >= 10 }
            .map { it.first.date }
            .distinct()
            .take(3)
        return DateCandidate(
            date = best.date,
            kind = kind,
            raw = raw,
            calendar = if (best.cal == 'J') "شمسی" else "میلادی",
            alternatives = alts
        )
    }

    // ---------- تابع اصلی ----------

    fun parse(rawLines: List<String>, today: LocalDate): List<DateCandidate> {
        val lines = rawLines.map { normalizeLine(it) }
        val found = ArrayList<DateCandidate>()
        for (li in lines.indices) {
            val line = lines[li]
            val kws = keywords(line)
            val prevLine = if (li > 0) lines[li - 1] else null
            val prevKws = if (prevLine != null) keywords(prevLine) else emptyList()
            val work = StringBuilder(line)
            for (p in pats) {
                val text = work.toString()
                for (m in p.re.findAll(text).toList()) {
                    val start = m.range.first
                    val end = m.range.last + 1
                    if (p.needsKw && kws.none { it.end <= start && start - it.end <= 25 }) continue
                    val kind = kindFor(kws, prevKws, prevLine, start, end)
                    val rds = p.build(m, kind != DateKind.PRODUCTION)
                    val c = choose(rds, today, kind, m.value.trim())
                    if (c != null) found.add(c)
                    for (i in start until end) work.setCharAt(i, '#')
                }
            }
        }
        return finalize(found)
    }

    private fun kindOrder(k: DateKind): Int = when (k) {
        DateKind.EXPIRY -> 0
        DateKind.UNKNOWN -> 1
        DateKind.PRODUCTION -> 2
    }

    fun rank(list: List<DateCandidate>): List<DateCandidate> =
        list.sortedWith(
            compareBy<DateCandidate> { kindOrder(it.kind) }.thenByDescending { it.date }
        )

    private fun finalize(found: List<DateCandidate>): List<DateCandidate> {
        // یک تاریخ تکراری: نوعِ صریح را نگه دار
        val byDate = LinkedHashMap<LocalDate, DateCandidate>()
        for (c in found) {
            val old = byDate[c.date]
            if (old == null || (old.kind == DateKind.UNKNOWN && c.kind != DateKind.UNKNOWN)) {
                byDate[c.date] = c
            }
        }
        var list = byDate.values.toList()
        val hasExpiry = list.any { it.kind == DateKind.EXPIRY }
        val unknown = list.filter { it.kind == DateKind.UNKNOWN }.sortedBy { it.date }
        if (!hasExpiry && unknown.size >= 2) {
            val latest = unknown.last()
            list = list.map {
                if (it.kind != DateKind.UNKNOWN) it
                else if (it.date == latest.date) it.copy(kind = DateKind.EXPIRY, guess = true)
                else it.copy(kind = DateKind.PRODUCTION, guess = true)
            }
        }
        return rank(list)
    }
}
