package br.edu.utfpr.helloworldcup.presentation

import android.app.RemoteInput
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.input.RemoteInputIntentHelper
import br.edu.utfpr.helloworldcup.data.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Modelos ---
data class Country(
    val name: String,
    val portugueseName: String,
    val emoji: String,
    val group: String,
    val points: Int,
    val region: String = "Outros",
    val isFavorite: Boolean = false,
    val status: String? = null
)

data class TeamStanding(
    val name: String,
    val emoji: String,
    val pontos: Int,
    val jogos: Int,
    val vitorias: Int
)

data class Match(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val stadium: String,
    val status: String,
    val time: String,
    val score: String?,
    val date: String,
    val group: String
)

// --- Design System ---
val WearGreenAccent = Color(0xFF00FF87)
val WearBackground = Color(0xFF000000)
val WearCardBackground = Color(0xFF141416)
val WearTextPrimary = Color(0xFFFFFFFF)
val WearTextSecondary = Color(0xFF94A3B8)

val WearColorPalette = Colors(
    primary = WearGreenAccent,
    secondary = WearCardBackground,
    background = WearBackground,
    onPrimary = Color.Black,
    onSecondary = WearTextPrimary,
    onBackground = WearTextPrimary
)

// --- Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

// --- ViewModel ---
class MatchViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance

    var countries by mutableStateOf<List<Country>>(emptyList())
        private set
    var matches by mutableStateOf<List<Match>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")
    var selectedRegion by mutableStateOf("Todos")
    var selectedGroupTab by mutableStateOf("A")

    val regions = listOf("Todos", "Sul-Am.", "Europa", "Am. Norte", "África", "Ásia")
    val groupTabs = listOf("A", "B", "C", "D", "E", "F", "G", "H")

    val filteredCountries: List<Country>
        get() = countries.filter { country ->
            (selectedRegion == "Todos" || country.region == selectedRegion) &&
            (searchQuery.isEmpty() || country.portugueseName.contains(searchQuery, ignoreCase = true))
        }

    init { fetchData() }

    fun fetchData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val matchesDto = apiService.getMatches()
                val selectionsResponse = apiService.getSelections()

                matches = matchesDto.map {
                    Match(it.id, it.home_team, it.away_team, it.stadium, it.status, it.time, it.score, it.date, it.group)
                }

                val teamGroups = mutableMapOf<String, String>()
                matchesDto.forEach {
                    teamGroups[it.home_team] = it.group
                    teamGroups[it.away_team] = it.group
                }

                countries = selectionsResponse.selecoes.map { name ->
                    Country(
                        name = name,
                        portugueseName = name,
                        emoji = getFlagEmoji(name),
                        group = teamGroups[name] ?: "Grupo ?",
                        points = calculatePoints(name, matches),
                        region = getRegionForTeam(name),
                        status = if (name == "Brasil") "Ao Vivo" else if (calculatePoints(name, matches) > 4) "Classificado" else null
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Erro de conexão"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite(countryName: String) {
        countries = countries.map { if (it.name == countryName) it.copy(isFavorite = !it.isFavorite) else it }
    }

    private fun getRegionForTeam(name: String): String = when (name.lowercase()) {
        "brasil", "argentina", "uruguai" -> "Sul-Am."
        "frança", "alemanha", "portugal", "espanha", "itália", "inglaterra", "escócia" -> "Europa"
        "eua", "méxico", "canadá" -> "Am. Norte"
        "senegal", "marrocos" -> "África"
        "japão" -> "Ásia"
        else -> "Outros"
    }

    private fun getFlagEmoji(name: String): String = when (name.lowercase()) {
        "brasil" -> "🇧🇷"
        "argentina" -> "🇦🇷"
        "frança" -> "🇫🇷"
        "alemanha" -> "🇩🇪"
        "portugal" -> "🇵🇹"
        "espanha" -> "🇪🇸"
        "eua" -> "🇺🇸"
        "méxico" -> "🇲🇽"
        "itália" -> "🇮🇹"
        "inglaterra" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿"
        "escócia" -> "🏴󠁧󠁢󠁳󠁣󠁴󠁿"
        else -> "🏳️"
    }

    private fun calculatePoints(teamName: String, matches: List<Match>): Int {
        var p = 0
        matches.filter { it.score != null && it.score.contains("-") }.forEach { m ->
            val s = m.score!!.split("-")
            if (s.size == 2) {
                val h = s[0].trim().toIntOrNull() ?: 0
                val a = s[1].trim().toIntOrNull() ?: 0
                if (m.homeTeam == teamName) { if (h > a) p += 3 else if (h == a) p += 1 }
                else if (m.awayTeam == teamName) { if (a > h) p += 3 else if (a == h) p += 1 }
            }
        }
        return p
    }
}

// --- UI Components ---
@Composable
fun WearApp() {
    MaterialTheme(colors = WearColorPalette) {
        val viewModel: MatchViewModel = viewModel()
        val navController = rememberSwipeDismissableNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize().background(WearBackground),
            timeText = { TimeText() }
        ) {
            SwipeDismissableNavHost(navController, "main") {
                composable("main") { MainCarouselScreen(viewModel, { navController.navigate("match_detail/$it") }, { navController.navigate("countries") }, { navController.navigate("matches") }, { navController.navigate("groups") }) }
                composable("countries") { CountriesScreen(viewModel) { navController.navigate("match_detail/$it") } }
                composable("groups") { GroupsScreen(viewModel) }
                composable("matches") { MatchDayScreen(viewModel) }
                composable("match_detail/{name}") { MatchDetailScreen(it.arguments?.getString("name"), viewModel) { navController.popBackStack() } }
            }
        }
    }
}

@Composable
fun MainCarouselScreen(vm: MatchViewModel, onDet: (String) -> Unit, onPa: () -> Unit, onJo: () -> Unit, onGr: () -> Unit) {
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WORLD CUP 2026", style = MaterialTheme.typography.caption2, color = WearGreenAccent, fontWeight = FontWeight.Bold)
            Text(hora, style = MaterialTheme.typography.display3, color = WearTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Button(onJo, colors = ButtonDefaults.buttonColors(WearCardBackground), modifier = Modifier.fillMaxWidth().height(36.dp)) { Text("Jogos do Dia", color = WearGreenAccent, fontSize = 12.sp) }
            Row(Modifier.padding(top = 4.dp), Arrangement.spacedBy(4.dp)) {
                Button(onPa, colors = ButtonDefaults.buttonColors(WearCardBackground), modifier = Modifier.weight(1f).height(36.dp)) { Text("Países", fontSize = 12.sp) }
                Button(onGr, colors = ButtonDefaults.buttonColors(WearCardBackground), modifier = Modifier.weight(1f).height(36.dp)) { Text("Grupos", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun FilterRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items) { item ->
            CompactChip(
                onClick = { onSelect(item) },
                label = { Text(item, fontSize = 10.sp) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (item == selected) WearGreenAccent else WearCardBackground,
                    contentColor = if (item == selected) Color.Black else WearTextPrimary
                )
            )
        }
    }
}

@Composable
fun CountriesScreen(vm: MatchViewModel, onCountryClick: (String) -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("Países", color = WearGreenAccent, style = MaterialTheme.typography.caption1) }
        item { SearchChip(vm.searchQuery, { vm.searchQuery = it }, "Buscar país...") }
        item { FilterRow(vm.regions, vm.selectedRegion) { vm.selectedRegion = it } }
        items(vm.filteredCountries) { c ->
            Card(
                onClick = { onCountryClick(c.name) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                backgroundPainter = CardDefaults.cardBackgroundPainter(WearCardBackground, WearCardBackground)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(c.portugueseName, style = MaterialTheme.typography.body2)
                            Text("${c.group} • ${c.points} pts", style = MaterialTheme.typography.caption2, color = WearTextSecondary)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        c.status?.let { StatusTag(it, if (it == "Ao Vivo") WearGreenAccent else Color(0xFF4ADE80)) }
                        Icon(if (c.isFavorite) Icons.Default.Star else Icons.Outlined.Star, null, tint = if (c.isFavorite) WearGreenAccent else WearTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsScreen(vm: MatchViewModel) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("Grupos", color = WearGreenAccent, style = MaterialTheme.typography.caption1) }
        item { FilterRow(vm.groupTabs, vm.selectedGroupTab) { vm.selectedGroupTab = it } }
        item {
            val groupTeams = vm.countries.filter { it.group.contains(vm.selectedGroupTab, ignoreCase = true) }.sortedByDescending { it.points }
            Card(onClick = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), backgroundPainter = CardDefaults.cardBackgroundPainter(WearCardBackground, WearCardBackground)) {
                Column {
                    Text("GRUPO ${vm.selectedGroupTab}", color = WearGreenAccent, style = MaterialTheme.typography.caption2)
                    Spacer(Modifier.height(4.dp))
                    groupTeams.forEachIndexed { i, t ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row { Text("${i+1} ${t.emoji} ${t.portugueseName}", fontSize = 11.sp) }
                            Row { Text("3  ${t.points/3}  ", color = Color.Gray, fontSize = 11.sp); Text("${t.points}", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchDayScreen(vm: MatchViewModel) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("Jogos do Dia", color = WearGreenAccent, style = MaterialTheme.typography.caption1) }
        val live = vm.matches.filter { it.status == "live" }
        val later = vm.matches.filter { it.status != "live" }
        if (live.isNotEmpty()) {
            item { Text("AO VIVO", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp)) }
            items(live) { m -> MatchCard(m) }
        }
        item { Text("MAIS TARDE", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp)) }
        items(later) { m -> MatchCard(m) }
    }
}

@Composable
fun MatchCard(m: Match) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), backgroundPainter = CardDefaults.cardBackgroundPainter(WearCardBackground, WearCardBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (m.status == "live") StatusTag("42'", WearGreenAccent)
                    else Text(m.time, fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(m.group, fontSize = 10.sp, color = Color.Gray)
                }
                Text("${m.homeTeam} × ${m.awayTeam}", style = MaterialTheme.typography.body2)
            }
            Text(m.score ?: "vs", color = WearGreenAccent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusTag(t: String, c: Color) {
    Box(Modifier.background(c.copy(0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
        Text(t, color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchChip(v: String, onV: (String) -> Unit, p: String) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        RemoteInput.getResultsFromIntent(r.data)?.getCharSequence("q")?.toString()?.let { onV(it) }
    }
    Chip(onClick = { launcher.launch(RemoteInputIntentHelper.createActionRemoteInputIntent().apply { RemoteInputIntentHelper.putRemoteInputsExtra(this, listOf(RemoteInput.Builder("q").setLabel(p).build())) }) }, label = { Text(if (v.isEmpty()) p else v, color = if (v.isEmpty()) WearTextSecondary else WearGreenAccent) }, colors = ChipDefaults.primaryChipColors(WearCardBackground), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
fun MatchDetailScreen(n: String?, vm: MatchViewModel, onBack: () -> Unit) {
    val c = vm.countries.find { it.name == n || it.portugueseName == n }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Card(onClick = {}, modifier = Modifier.fillMaxWidth().padding(8.dp), backgroundPainter = CardDefaults.cardBackgroundPainter(WearCardBackground, WearCardBackground)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StatusTag("42' AO VIVO", WearGreenAccent)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🇧🇷", fontSize = 24.sp); Text("BRA", fontSize = 10.sp) }
                        Text("2 - 1", style = MaterialTheme.typography.display3, color = WearGreenAccent)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🇦🇷", fontSize = 24.sp); Text("ARG", fontSize = 10.sp) }
                    }
                }
            }
        }
        item { Text("EVENTOS", style = MaterialTheme.typography.caption2, color = WearTextSecondary) }
        item { EventItem("32'", "⚽ BRA - Gol", "Rodrigo 2-1") }
        item { EventItem("25'", "⚽ ARG - Gol", "Di Maria 1-1") }
        item { Button(onClick = onBack, colors = ButtonDefaults.buttonColors(WearCardBackground), modifier = Modifier.padding(top = 16.dp)) { Text("Voltar") } }
    }
}

@Composable
fun EventItem(t: String, e: String, d: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, color = WearGreenAccent, fontSize = 10.sp, modifier = Modifier.width(24.dp))
        Column { Text(e, fontSize = 11.sp); Text(d, fontSize = 9.sp, color = Color.Gray) }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() { WearApp() }
