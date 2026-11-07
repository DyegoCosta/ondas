package com.dyegocosta.ondas

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Textos em português, sem depender do idioma configurado no aparelho. */
object Format {
    private val WEEKDAYS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    private val LETTERS = listOf("S", "T", "Q", "Q", "S", "S", "D")

    fun dateTime(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(at).atZone(zone).toLocalDateTime()

    fun date(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = dateTime(at, zone).toLocalDate()

    fun clock(at: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = dateTime(at, zone)
        return "%02d:%02d".format(t.hour, t.minute)
    }

    fun dayLabel(day: LocalDate, today: LocalDate = LocalDate.now()): String =
        when (ChronoUnit.DAYS.between(day, today)) {
            0L -> "Hoje"
            1L -> "Ontem"
            else -> "${WEEKDAYS[day.dayOfWeek.value - 1]}, ${day.dayOfMonth} de ${MONTHS[day.monthValue - 1]}"
        }

    fun weekdayLetter(day: LocalDate): String = LETTERS[day.dayOfWeek.value - 1]

    fun todayLabel(count: Int): String = when (count) {
        0 -> "Nenhuma hoje"
        1 -> "1 onda hoje"
        else -> "$count ondas hoje"
    }

    fun lastLabel(lastAt: Long?, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        if (lastAt == null) return "Nenhuma registrada ainda"
        val label = dayLabel(date(lastAt, zone), today)
        val time = clock(lastAt, zone)
        return if (label == "Hoje") "Última às $time" else "Última: ${label.lowercase()}, $time"
    }

    val INTENSITIES = listOf("Leve", "Moderada", "Forte")

    fun intensityLabel(intensity: Int?): String =
        intensity?.let { INTENSITIES.getOrNull(it - 1) } ?: "Sem intensidade"

    fun periodLabel(total: Int): String =
        if (total == 1) "1 onda nos últimos 14 dias" else "$total ondas nos últimos 14 dias"
}
