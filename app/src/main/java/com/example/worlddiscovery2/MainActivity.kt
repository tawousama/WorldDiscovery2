package com.example.worlddiscovery2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worlddiscovery2.ui.theme.WorldDiscovery2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldDiscovery2Theme {
                val url = "http://localhost:8081/countries.json"
                Surface {
                    CountryListScreen(url)
                }
            }
        }
    }
}

@Composable
fun CountryListScreen(url: String) {
    val coroutineScope = rememberCoroutineScope()
    var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        coroutineScope.launch {
            try {
                countries = loadCountries(url)
                errorMessage = null // Réinitialiser le message d'erreur en cas de succès
            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement des pays : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (errorMessage != null) {
        Text(text = errorMessage ?: "Erreur inconnue")
    } else {

        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            // content padding
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 16.dp,
                end = 12.dp,
                bottom = 16.dp
            ),
            content = {
                items(countries.size) { index ->
                    val country = countries[index]
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(color = Color(0xFFE7E0EC))
                            .fillMaxSize()
                            .height(100.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = country.name,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )

                    }
                }
            }
        )
    }
}

suspend fun loadCountries(url: String): List<Country> {
    // Exécute la requête réseau dans le contexte IO (entrée/sortie)
    val content = withContext(Dispatchers.IO) {
        URL(url).openConnection().getInputStream().bufferedReader().use { it.readText() }
    }

    // Analyse le JSON
    val jsonArray = JSONArray(content)
    val countries = mutableListOf<Country>()
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val name = jsonObject.getString("name")
        val code = jsonObject.getString("code")
        countries.add(Country(name = name, code = code, latitude = 0.0F, longitude = 0.0F))
    }
    return countries
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorldDiscovery2Theme {
    }
}