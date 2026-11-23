package cl.app.dina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.app.dina.data.model.ModoTema
import cl.app.dina.navigation.NavegacionRaiz
import cl.app.dina.ui.theme.DINATheme

// Solo muestra la UI: el tema (el que se eligió en el Menú) y la navegación raíz (Login o app)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = (application as DinaApplication).container.preferenciasRepository
        setContent {
            val modoTema by preferencias.modoTema.collectAsStateWithLifecycle(initialValue = ModoTema.SISTEMA)
            DINATheme(modoTema = modoTema) {
                NavegacionRaiz()
            }
        }
    }
}
