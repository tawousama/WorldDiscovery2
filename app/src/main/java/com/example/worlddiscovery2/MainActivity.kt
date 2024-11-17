package com.example.worlddiscovery2

import android.content.res.Configuration
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldDiscovery2Theme {
                Surface {
                    val url = "http://localhost:8081/countries.json"
                    NavigationComponent(url = url)
                }
            }
        }
    }
}
@Composable
fun FlagAndDrawingScreen(country: Country) {
    // Variable d'état pour la couleur sélectionnée
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var flagImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Charger le drapeau
    LaunchedEffect(country.code) {
        val flagBitmap = downloadFlagImage(country.code) // Télécharger le drapeau
        flagImageBitmap = flagBitmap?.asImageBitmap() // Convertir en ImageBitmap
    }

    if (flagImageBitmap == null) {
        // Affiche un indicateur de chargement tant que l'image n'est pas prête
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Palette de couleurs
            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                Palette(
                    image = flagImageBitmap!!,
                    modifier = Modifier.fillMaxSize(),
                    onSelectedColor = { color ->
                        selectedColor = color // Mettre à jour la couleur sélectionnée
                    }
                )
            }

            // Zone de dessin
            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                ActiveDrawer(
                    color = selectedColor, // Utilise la couleur sélectionnée pour dessiner
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun NavigationComponent(url: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "country_list") {
        composable("country_list") {
            CountryListScreen(url) { country ->
                navController.navigate("flag_and_drawing/${country.name}/${country.code}")
            }
        }
        composable(
            route = "flag_and_drawing/{name}/{code}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val code = backStackEntry.arguments?.getString("code") ?: ""
            val country = Country(name = name, code = code, latitude = 0f, longitude = 0f)
            FlagAndDrawingScreen(country = country)
        }
    }
}

@Composable
fun CountryListScreen(url: String, onCountryClick: (Country) -> Unit) {
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
            contentPadding = PaddingValues(12.dp),
            content = {
                items(countries.size) { index ->
                    val country = countries[index]
                    Card(
                        modifier = Modifier
                            .padding(10.dp)
                            .background(color = Color(0xFFE7E0EC))
                            .fillMaxSize()
                            .height(130.dp)
                            .clickable { onCountryClick(country) },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlagDisplayer(
                                country = country,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            )
                            Text(
                                text = country.name,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
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


@Composable
fun Drawer(sketch: Sketch, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        sketch.paths.forEach { path ->
            for (i in 0 until path.size - 1) {
                drawLine(
                    color = path.color,
                    start = path[i],
                    end = path[i + 1],
                    strokeWidth = 5f
                )
            }
        }
    }
}
@Composable
fun PointerCapturer(modifier: Modifier = Modifier, onNewPointerPosition: (Offset, Boolean) -> Unit) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        val boundedOffset = Offset(
                            position.x.coerceIn(0f, componentSize.width.toFloat()),
                            position.y.coerceIn(0f, componentSize.height.toFloat())
                        )
                        if (event.changes.first().pressed) {
                            // Continue current path
                            onNewPointerPosition(boundedOffset, false)
                        } else {
                            // Start a new path
                            onNewPointerPosition(boundedOffset, true)
                        }
                    }
                }
            }
    )
}

@Composable
fun ActiveDrawer(color: Color, modifier: Modifier = Modifier) {
    var sketch by remember { mutableStateOf(Sketch.createEmpty() + color) }

    Box(modifier = modifier) {
        Drawer(sketch = sketch, modifier = Modifier.fillMaxSize())
        PointerCapturer(
            modifier = Modifier.fillMaxSize(),
            onNewPointerPosition = { offset, isNewPath ->
                sketch = if (isNewPath) {
                    sketch + color + offset
                } else {
                    sketch + offset
                }
            }
        )
    }
}
@Composable
fun Palette(image: ImageBitmap, modifier: Modifier = Modifier, onSelectedColor: (Color) -> Unit) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position ?: continue
                        if (event.changes.first().pressed) {
                            val scaledX = ((position.x / componentSize.width) * image.width).toInt()
                            val scaledY = ((position.y / componentSize.height) * image.height).toInt()
                            if (scaledX in 0 until image.width && scaledY in 0 until image.height) {
                                val bitmap = image.asAndroidBitmap()
                                val pixelColor = bitmap.getPixel(scaledX, scaledY)
                                onSelectedColor(Color(pixelColor))
                            }
                        }
                    }
                }
            }
    ) {
        Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ActiveDrawerWithPalette(paletteImage: ImageBitmap, modifier: Modifier = Modifier) {
    val orientation = LocalConfiguration.current.orientation
    var selectedColor by remember { mutableStateOf(Color.Black) }

    val layoutModifier = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Modifier.fillMaxWidth().height(300.dp)
    } else {
        Modifier.fillMaxSize()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Palette(
                image = paletteImage,
                modifier = layoutModifier,
                onSelectedColor = { selectedColor = it }
            )
            ActiveDrawer(
                color = selectedColor,
                modifier = layoutModifier
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Palette(
                    image = paletteImage,
                    modifier = layoutModifier,
                    onSelectedColor = { selectedColor = it }
                )
                ActiveDrawer(
                    color = selectedColor,
                    modifier = layoutModifier
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun ActiveDrawerWithPalettePreview() {
    // Replace with an actual ImageBitmap for testing
    val testBitmap = ImageBitmap(100, 100)
    ActiveDrawerWithPalette(paletteImage = testBitmap)
}

