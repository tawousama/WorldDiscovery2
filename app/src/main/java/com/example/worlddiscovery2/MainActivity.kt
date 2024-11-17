package com.example.worlddiscovery2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worlddiscovery2.ui.theme.WorldDiscovery2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.URL
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldDiscovery2Theme {
                val url = "http://localhost:8081/countries.json"
                Surface {
                    ActiveDrawer(
                        color = Color.Blue, modifier = Modifier
                            .size(500.dp)
                            .padding(20.dp)
                            .border(color = Color.Red, width = 1.dp)
                    )
                    //CountryListScreen(url)
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
                            .padding(10.dp)
                            .background(color = Color(0xFFE7E0EC))
                            .fillMaxSize()
                            .height(130.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        FlagDisplayer(
                            country,
                            modifier = Modifier
                                .align(alignment = Alignment.CenterHorizontally)
                                .padding(10.dp)
                        )
                        Text(
                            text = country.name,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(14.dp)
                                .align(alignment = Alignment.CenterHorizontally)
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

@Composable
fun FlagDisplayer(country: Country, modifier: Modifier = Modifier) {
    // Variable d'état pour stocker l'image et le message de statut
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf("Téléchargement en cours...") }

    // Lancer la tâche de téléchargement dans LaunchedEffect
    LaunchedEffect(country.code) {
        try {
            // Initialiser le message de statut pour le téléchargement en cours
            statusMessage = "Téléchargement en cours..."

            // Télécharger l'image dans une coroutine
            val downloadedBitmap = downloadFlagImage(country.code)

            // Mettre à jour le bitmap téléchargé et le statut
            if (downloadedBitmap != null) {
                bitmap = downloadedBitmap
                statusMessage = "Téléchargement réussi !" // Succès
            } else {
                statusMessage = "Échec du téléchargement" // Échec
            }
        } catch (e: Exception) {
            // En cas d'erreur imprévue
            statusMessage = "Erreur : ${e.localizedMessage}"
        }
    }

    // Afficher l'image ou le message de statut
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(), // Convertir le Bitmap en ImageBitmap
            contentDescription = "Flag of ${country.name}",
            modifier = modifier
        )
    } else {
        // Si l'image n'est pas encore téléchargée, afficher le message de statut
        Text(
            text = statusMessage,
            modifier = modifier.padding(16.dp),
            color = if (statusMessage.contains("Erreur") || statusMessage.contains("Échec")) Color.Red else Color.Green,
            fontSize = 18.sp
        )
    }
}


suspend fun downloadFlagImage(countryCode: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val url =
                URL("http://localhost:8081/flags/$countryCode.png") // Assurez-vous de remplacer localhost si nécessaire
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: IOException) {
            null // En cas d'erreur, retourner null
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawerPreview() {
    // Création d'un sketch de test avec 2 chemins de dessin (un noir et un rouge)
    val sketch = Sketch.createEmpty()
        .plus(Color.Black)
        .plus(Offset(100f, 100f))
        .plus(Offset(200f, 200f))
        .plus(Color.Red)
        .plus(Offset(300f, 100f))
        .plus(Offset(400f, 200f))

    Drawer(sketch = sketch, modifier = Modifier.fillMaxSize())
}
@Composable
fun ActiveDrawer(
    color: Color,
    modifier: Modifier = Modifier
) {
    var sketch by remember { mutableStateOf(Sketch.createEmpty()) }
    var isFirstTouch by remember { mutableStateOf(true) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var currentPosition by remember { mutableStateOf(Offset(0f, 0f)) } // Pour stocker la position actuelle

    // Crée un composant Box pour dessiner
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                size = it // On capture la taille du composant
            }
    ) {
        // Drawer qui dessine le sketch
        Drawer(sketch, modifier = Modifier.fillMaxSize())

        // PointerCapturer pour détecter les gestes
        PointerCapturer(
            modifier = Modifier.fillMaxSize(),
            onNewPointerPosition = { position, firstTouch ->
                // Adapter les coordonnées pour les mettre dans le contexte du Canvas
                val adjustedPosition = Offset(
                    x = position.x.coerceIn(0f, size.width.toFloat()),  // Assurer que x est dans les bornes
                    y = position.y.coerceIn(0f, size.height.toFloat())  // Assurer que y est dans les bornes
                )
                isFirstTouch = firstTouch
                // Mettre à jour le sketch avec la nouvelle position
                if (isFirstTouch) {
                    sketch += color // On commence un nouveau tracé avec la couleur donnée
                }
                sketch += adjustedPosition // Ajouter le point au tracé

                // Mettre à jour la position actuelle
                currentPosition = adjustedPosition
                // Mettre à jour l'état du premier contact
            }
        )

        // Affichage de la position actuelle et de l'état du premier contact
        Text(
            text = "Position: ${currentPosition.x}, ${currentPosition.y}\nFirst Touch: $isFirstTouch",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
    }
}

@Composable
fun Drawer(sketch: Sketch, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Dessiner les chemins de sketch
        sketch.paths.forEach { path ->
            // Chaque path représente un tracé, dessine chaque segment
            for (i in 1 until path.size) {
                val start = path[i - 1]
                val end = path[i]
                drawLine(
                    start = start,
                    end = end,
                    color = path.color,
                    strokeWidth = 5f
                )
            }
        }
    }
}

@Composable
fun PointerCapturer(
    modifier: Modifier = Modifier,
    onNewPointerPosition: (Offset, Boolean) -> Unit
) {
    var isFirstTouch by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { _, pan ->
                // Position du geste, en pixels absolus
                val position = Offset(pan.x, pan.y)

                // Appeler la fonction onNewPointerPosition avec la position et si c'est le premier contact
                onNewPointerPosition(position, isFirstTouch)

                // Si c'est le premier contact, on commence un nouveau tracé
                if (isFirstTouch) {
                    isFirstTouch = false
                }
            }
        }
    )
}

@Preview
@Composable
fun ActiveDrawerPreview() {
    ActiveDrawer(color = Color.Red)
}
