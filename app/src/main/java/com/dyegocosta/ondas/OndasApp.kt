package com.dyegocosta.ondas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun OndasApp(repository: OndasRepository, screen: Screen, navigate: (Screen) -> Unit) {
    val p = LocalPalette.current
    val entries by repository.entries.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun deleteWithUndo(entry: Entry) {
        repository.delete(entry.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Onda excluída",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) repository.restore(entry)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = p.ground, contentColor = p.ink) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Home -> Tabs(selected = screen, navigate = navigate) {
                    HomeScreen(entries = entries, onRegister = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        navigate(Screen.Note(id = repository.add(), isNew = true, animate = true))
                    })
                }

                Screen.History -> {
                    BackHandler { navigate(Screen.Home) }
                    Tabs(selected = screen, navigate = navigate) {
                        HistoryScreen(
                            entries = entries,
                            onOpen = { navigate(Screen.Note(id = it.id, isNew = false)) },
                            onDelete = { deleteWithUndo(it) },
                        )
                    }
                }

                is Screen.Note -> {
                    val entry = entries.firstOrNull { it.id == screen.id }
                    val back = if (screen.isNew) Screen.Home else Screen.History
                    BackHandler { navigate(back) }
                    if (entry == null) {
                        LaunchedEffect(Unit) { navigate(back) }
                    } else {
                        NoteScreen(
                            entry = entry,
                            screen = screen,
                            repository = repository,
                            onDone = { navigate(back) },
                            onUndo = {
                                repository.delete(entry.id)
                                navigate(Screen.Home)
                            },
                            onDelete = {
                                navigate(Screen.History)
                                deleteWithUndo(entry)
                            },
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun Tabs(selected: Screen, navigate: (Screen) -> Unit, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TabButton("Registrar", R.drawable.ic_tab_register, selected == Screen.Home, Modifier.weight(1f)) { navigate(Screen.Home) }
            TabButton("Histórico", R.drawable.ic_tab_history, selected == Screen.History, Modifier.weight(1f)) { navigate(Screen.History) }
        }
    }
}

@Composable
private fun TabButton(label: String, icon: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (active) p.tab else p.ground,
        contentColor = p.ink,
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeScreen(entries: List<Entry>, onRegister: () -> Unit) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    val count = entries.count { Format.date(it.at) == today }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
        Text(Format.todayLabel(count), fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
        Text(Format.lastLabel(entries.firstOrNull()?.at), fontSize = 18.sp, color = p.muted)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(312.dp).background(p.hotRing2, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(272.dp).background(p.hotRing1, CircleShape), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = onRegister,
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        shape = CircleShape,
                        color = p.hot,
                        contentColor = p.onHot,
                        interactionSource = interaction,
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_waves), contentDescription = null, modifier = Modifier.size(width = 48.dp, height = 34.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Registrar onda de calor",
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteScreen(
    entry: Entry,
    screen: Screen.Note,
    repository: OndasRepository,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var text by rememberSaveable(entry.id) { mutableStateOf(entry.note) }
    val focus = remember { FocusRequester() }
    val cooling = remember { Animatable(if (screen.animate) 0.92f else 0f) }
    val moment = Format.dateTime(entry.at)
    val zone = ZoneId.systemDefault()

    fun save(dateTime: LocalDateTime) {
        val at = dateTime.atZone(zone).toInstant().toEpochMilli()
        repository.updateAt(entry.id, minOf(at, System.currentTimeMillis()))
    }

    LaunchedEffect(Unit) {
        if (screen.focus) focus.requestFocus()
        cooling.animateTo(0f, tween(800))
    }

    Box(Modifier.fillMaxSize().background(if (screen.isNew) p.coolGround else p.ground)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (screen.isNew) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = p.cool, modifier = Modifier.size(44.dp))
                            Text(
                                "Registrada às ${Format.clock(entry.at)}",
                                fontSize = 32.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Desfazer", color = p.cool, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onDone, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_back), contentDescription = "Voltar ao histórico", tint = p.ink)
                        }
                        Text(
                            "Onda das ${Format.clock(entry.at)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Excluir", color = p.danger, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                IntensityPicker(entry.intensity) { repository.updateIntensity(entry.id, it) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anotação (opcional)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            repository.updateNote(entry.id, it)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 168.dp).focusRequester(focus),
                        textStyle = LocalTextStyleBody(),
                        placeholder = { Text("O que você estava fazendo, comendo ou sentindo?", fontSize = 18.sp) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = p.surface,
                            unfocusedContainerColor = p.surface,
                            focusedBorderColor = p.cool,
                            unfocusedBorderColor = p.field,
                            focusedTextColor = p.ink,
                            unfocusedTextColor = p.ink,
                            focusedPlaceholderColor = p.muted,
                            unfocusedPlaceholderColor = p.muted,
                        ),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quando", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        WhenButton(Format.dayLabel(moment.toLocalDate()), "Alterar data", Modifier.weight(3f)) {
                            DatePickerDialog(context, { _, year, month, day ->
                                save(moment.withYear(year).withMonth(month + 1).withDayOfMonth(day))
                            }, moment.year, moment.monthValue - 1, moment.dayOfMonth).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        }
                        WhenButton(Format.clock(entry.at), "Alterar hora", Modifier.weight(2f)) {
                            TimePickerDialog(context, { _, hour, minute ->
                                save(moment.withHour(hour).withMinute(minute))
                            }, moment.hour, moment.minute, true).show()
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.cool, contentColor = p.onCool),
                shape = RoundedCornerShape(30.dp),
            ) {
                Text("Concluir", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (screen.animate) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = cooling.value }
                    .background(p.hot),
            )
        }
    }
}

@Composable
private fun IntensityPicker(value: Int?, onChange: (Int?) -> Unit) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Intensidade ", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("(opcional)", fontSize = 18.sp, color = p.muted, modifier = Modifier.weight(1f))
            if (value != null) {
                TextButton(onClick = { onChange(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Limpar", color = p.cool, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Format.INTENSITIES.forEachIndexed { index, name ->
                val level = index + 1
                val on = value == level
                Surface(
                    onClick = { onChange(level) },
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp).semantics { selected = on },
                    shape = RoundedCornerShape(16.dp),
                    color = p.surface,
                    contentColor = p.ink,
                    border = if (on) BorderStroke(2.5.dp, p.ink) else BorderStroke(1.5.dp, p.line),
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        Box(Modifier.size(22.dp).background(p.intensity[index], CircleShape))
                        Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTextStyleBody() = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 25.sp)

@Composable
private fun WhenButton(label: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).semantics { contentDescription = "$description: $label" },
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = p.surface, contentColor = p.ink),
        border = BorderStroke(1.5.dp, p.field),
    ) {
        Text(label, fontSize = 17.sp, maxLines = 1)
    }
}

private sealed interface HistoryRow {
    data class Header(val label: String) : HistoryRow
    data class Item(val entry: Entry) : HistoryRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(entries: List<Entry>, onOpen: (Entry) -> Unit, onDelete: (Entry) -> Unit) {
    val p = LocalPalette.current
    val rows = buildList {
        var last: LocalDate? = null
        entries.forEach { entry ->
            val day = Format.date(entry.at)
            if (day != last) {
                add(HistoryRow.Header(Format.dayLabel(day)))
                last = day
            }
            add(HistoryRow.Item(entry))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
    ) {
        item(key = "title") {
            Text(
                "Histórico",
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(24.dp))
            Chart(entries)
            Spacer(Modifier.height(16.dp))
        }
        if (entries.isEmpty()) {
            item(key = "empty") { Text("Nenhuma onda registrada ainda.", fontSize = 18.sp, color = p.muted) }
        }
        items(rows, key = {
            when (it) {
                is HistoryRow.Header -> "h-${it.label}"
                is HistoryRow.Item -> it.entry.id
            }
        }) { row ->
            when (row) {
                is HistoryRow.Header -> Text(
                    row.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.muted,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
                )

                is HistoryRow.Item -> {
                    val state = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(row.entry)
                            true
                        } else {
                            false
                        }
                    })
                    SwipeToDismissBox(
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().background(p.danger).padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text("Excluir", color = p.onDanger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                    ) {
                        EntryRow(row.entry) { onOpen(row.entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(onClick = onClick, color = p.ground, contentColor = p.ink, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.heightIn(min = 64.dp).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val dot = Modifier
                    .padding(top = 5.dp)
                    .size(16.dp)
                    .semantics { contentDescription = Format.intensityLabel(entry.intensity) }
                Box(
                    entry.intensity?.let { dot.background(p.intensity[it - 1], CircleShape) }
                        ?: dot.border(2.dp, p.field, CircleShape),
                )
                Text(Format.clock(entry.at), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                Text(
                    entry.note.ifBlank { "Sem anotação" },
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    color = if (entry.note.isBlank()) p.muted else p.ink,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        }
    }
}

@Composable
private fun Chart(entries: List<Entry>) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    val days = (13L downTo 0L).map { today.minusDays(it) }
    val byDay = entries.groupingBy { Format.date(it.at) }.eachCount()
    val counts = days.map { byDay[it] ?: 0 }
    val max = maxOf(1, counts.max())
    val total = counts.sum()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(Format.periodLabel(total), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Gráfico de barras: ${Format.periodLabel(total)}, por dia." },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, day ->
                val count = counts[index]
                val isToday = day == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (count > 0) "$count" else "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (count > 0) (72 * count / max).dp else 3.dp)
                            .background(if (count > 0) p.hot else p.line, RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Format.weekdayLetter(day),
                        fontSize = 14.sp,
                        color = if (isToday) p.ink else p.muted,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
