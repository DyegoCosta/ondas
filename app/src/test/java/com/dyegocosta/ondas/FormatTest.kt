package com.dyegocosta.ondas

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class FormatTest {
    private val today = LocalDate.of(2026, 11, 6) // sexta-feira

    @Test fun dayLabels() {
        assertEquals("Hoje", Format.dayLabel(today, today))
        assertEquals("Ontem", Format.dayLabel(today.minusDays(1), today))
        assertEquals("Quarta, 4 de nov", Format.dayLabel(today.minusDays(2), today))
    }

    @Test fun todayLabels() {
        assertEquals("Nenhuma hoje", Format.todayLabel(0))
        assertEquals("1 onda hoje", Format.todayLabel(1))
        assertEquals("3 ondas hoje", Format.todayLabel(3))
    }

    @Test fun lastLabels() {
        assertEquals("Nenhuma registrada ainda", Format.lastLabel(null, today))
        val todayAt = today.atTime(14, 32).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("Última às 14:32", Format.lastLabel(todayAt, today, ZoneOffset.UTC))
        val yesterdayAt = today.minusDays(1).atTime(23, 10).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("Última: ontem, 23:10", Format.lastLabel(yesterdayAt, today, ZoneOffset.UTC))
    }

    @Test fun weekdayLetters() {
        assertEquals("S", Format.weekdayLetter(today))
        assertEquals("D", Format.weekdayLetter(LocalDate.of(2026, 11, 8)))
    }

    @Test fun intensityLabels() {
        assertEquals("Leve", Format.intensityLabel(1))
        assertEquals("Forte", Format.intensityLabel(3))
        assertEquals("Sem intensidade", Format.intensityLabel(null))
    }
}
