package cl.app.dina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.app.dina.navigation.NavegacionRaiz
import cl.app.dina.ui.theme.DINATheme

// Solo muestra la UI: el tema y la navegación raíz (Login o app)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DINATheme {
                NavegacionRaiz()
            }
        }
    }
}