package cl.app.dina.ui.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.ModoTema
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import cl.app.dina.ui.components.FotoPerfil
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.MenuViewModel
import java.io.File

class MenuAcciones(
    val onFotoClick: () -> Unit = {},
    val onAgregarEspacio: () -> Unit = {},
    val onModoTemaChange: (ModoTema) -> Unit = {},
    val onCerrarSesion: () -> Unit = {}
)

/**
 * Menú: perfil (tocar la foto permite cambiarla desde la Galería o la Cámara),
 * "Administración" (solo admin), "General" (modo oscuro) y "Cerrar sesión".
 */
@Composable
fun MenuScreen(
    onAgregarEspacio: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val modoTema by viewModel.modoTema.collectAsStateWithLifecycle()
    val subiendoFoto by viewModel.subiendoFoto.collectAsStateWithLifecycle()
    val confirmandoCierre by viewModel.confirmandoCierre.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val foto = rememberSelectorDeFoto(onFotoElegida = viewModel::cambiarFoto, onBorrar = viewModel::borrarFoto)

    MenuContenido(
        usuario = usuario,
        modoTema = modoTema,
        subiendoFoto = subiendoFoto,
        acciones = MenuAcciones(
            onFotoClick = foto::elegirOrigen,
            onAgregarEspacio = onAgregarEspacio,
            onModoTemaChange = viewModel::cambiarModoTema,
            onCerrarSesion = viewModel::pedirCerrarSesion
        ),
        modifier = modifier
    )

    foto.Dialogos(tieneFoto = usuario?.fotoUrl != null)

    if (confirmandoCierre) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarCerrarSesion,
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar de nuevo con tu cuenta institucional.") },
            // Acción destructiva en rojo; "Cancelar" usa el color del tema
            confirmButton = {
                TextButton(
                    onClick = viewModel::cerrarSesion,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarCerrarSesion) { Text("Cancelar") }
            }
        )
    }
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@Composable
private fun MenuContenido(
    usuario: Usuario?,
    modoTema: ModoTema,
    subiendoFoto: Boolean,
    acciones: MenuAcciones,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (usuario != null) Perfil(usuario, subiendoFoto, acciones.onFotoClick)

        if (usuario?.esAdmin == true) {
            TituloSeccion("Administración")
            OpcionMenu(Icons.Outlined.Add, "Agregar espacio", onClick = acciones.onAgregarEspacio)
        }

        TituloSeccion("General")
        OpcionModoOscuro(modoTema, acciones.onModoTemaChange)

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = acciones.onCerrarSesion,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
}

// ── Perfil: rol, foto (tocable), nombre y correo ────────────────────────────

@Composable
private fun Perfil(usuario: Usuario, subiendoFoto: Boolean, onFotoClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            usuario.descripcionRol,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // "Pop" al entrar al Menú: la foto crece con un pequeño rebote
        val escala = remember { Animatable(ESCALA_INICIAL_POP) }
        LaunchedEffect(Unit) {
            escala.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = escala.value
                    scaleY = escala.value
                }
                .semantics { contentDescription = "Foto de perfil. Toca para cambiarla" }
                .clickable(enabled = !subiendoFoto, role = Role.Button, onClick = onFotoClick)
        ) {
            FotoPerfil(
                nombre = usuario.nombre,
                fotoUrl = usuario.fotoUrl,
                tamano = 104.dp,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            if (subiendoFoto) {
                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(104.dp))
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
        }
        Text(
            usuario.nombre,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            usuario.correo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tamaño de la foto al empezar el "pop" (1 = tamaño normal). */
private const val ESCALA_INICIAL_POP = 0.6f

// ── Opciones ────────────────────────────────────────────────────────────────

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp)
    )
}

/** Fila con el ícono suelto a la izquierda: "+  Agregar espacio". */
@Composable
private fun OpcionMenu(
    icono: ImageVector,
    texto: String,
    onClick: () -> Unit,
    detalle: @Composable () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(texto, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
        trailingContent = detalle,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    )
}

/** "☾  Modo oscuro   Sistema ▾": abre un menú con Sistema, Claro y Oscuro. */
@Composable
private fun OpcionModoOscuro(modoTema: ModoTema, onCambio: (ModoTema) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OpcionMenu(Icons.Outlined.DarkMode, "Modo oscuro", onClick = { abierto = true }) {
        // El menú se ancla aquí, bajo "Sistema ▾"
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modoTema.etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                ModoTema.entries.forEach { modo ->
                    val elegido = modo == modoTema
                    DropdownMenuItem(
                        text = { Text(modo.etiqueta) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = if (elegido) "Elegido" else null,
                                tint = if (elegido) MaterialTheme.colorScheme.onSurface else Color.Transparent
                            )
                        },
                        onClick = {
                            onCambio(modo)
                            abierto = false
                        }
                    )
                }
            }
        }
    }
}

// ── Foto de perfil: Galería, Cámara (con permiso) o Borrar ──────────────────

/**
 * Flujo para cambiar la foto de perfil:
 * 1. Diálogo "Galería, Cámara o Borrar" ("Borrar" solo si hay foto; pide confirmación).
 * 2. Galería: selector de fotos del sistema (no necesita permisos).
 * 3. Cámara: si falta el permiso, primero un diálogo que explica para qué se usa y luego
 *    el permiso del sistema; con permiso, se abre la cámara y la foto queda en un archivo temporal.
 */
private class SelectorDeFoto(
    private val context: Context,
    private val abrirGaleria: () -> Unit,
    private val abrirCamara: () -> Unit,
    private val pedirPermiso: () -> Unit,
    private val borrar: () -> Unit
) {
    var eligiendoOrigen by mutableStateOf(false)
    var explicandoPermiso by mutableStateOf(false)
    var confirmandoBorrado by mutableStateOf(false)

    fun elegirOrigen() { eligiendoOrigen = true }

    fun usarCamara() {
        eligiendoOrigen = false
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (tienePermiso) abrirCamara() else explicandoPermiso = true
    }

    fun usarGaleria() {
        eligiendoOrigen = false
        abrirGaleria()
    }

    @Composable
    fun Dialogos(tieneFoto: Boolean) {
        if (eligiendoOrigen) {
            AlertDialog(
                onDismissRequest = { eligiendoOrigen = false },
                title = { Text("Foto de perfil") },
                text = {
                    Column {
                        OrigenFoto(Icons.Outlined.PhotoLibrary, "Galería", onClick = ::usarGaleria)
                        OrigenFoto(Icons.Outlined.PhotoCamera, "Cámara", onClick = ::usarCamara)
                        if (tieneFoto) {
                            OrigenFoto(Icons.Outlined.Delete, "Borrar", color = MaterialTheme.colorScheme.error) {
                                eligiendoOrigen = false
                                confirmandoBorrado = true
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { eligiendoOrigen = false }) { Text("Cancelar") }
                }
            )
        }
        if (explicandoPermiso) {
            AlertDialog(
                onDismissRequest = { explicandoPermiso = false },
                icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                title = { Text("¿Permitir usar la cámara?") },
                text = { Text("DINA usará la cámara solo para tomar tu foto de perfil.") },
                confirmButton = {
                    TextButton(onClick = {
                        explicandoPermiso = false
                        pedirPermiso()
                    }) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(onClick = { explicandoPermiso = false }) { Text("Ahora no") }
                }
            )
        }
        if (confirmandoBorrado) {
            AlertDialog(
                onDismissRequest = { confirmandoBorrado = false },
                title = { Text("¿Borrar tu foto de perfil?") },
                text = { Text("En su lugar se mostrarán tus iniciales.") },
                // Acción destructiva en rojo, como "Cerrar sesión"
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmandoBorrado = false
                            borrar()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Borrar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun OrigenFoto(
    icono: ImageVector,
    texto: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(texto) },
        leadingContent = { Icon(icono, contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = color,
            leadingIconColor = color
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun rememberSelectorDeFoto(onFotoElegida: (Uri) -> Unit, onBorrar: () -> Unit): SelectorDeFoto {
    val context = LocalContext.current
    // El archivo temporal de la cámara sobrevive a la rotación (la cámara puede tardar)
    var uriCamara by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onFotoElegida)
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        if (guardada) uriCamara?.let(onFotoElegida)
    }
    val abrirCamara = {
        val uri = nuevoArchivoDeFoto(context)
        uriCamara = uri
        camara.launch(uri)
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara()
        else Toast.makeText(
            context,
            "Sin permiso no se puede usar la cámara. Puedes activarlo en Ajustes > Aplicaciones > DINA",
            Toast.LENGTH_LONG
        ).show()
    }

    return remember(context) {
        SelectorDeFoto(
            context = context,
            abrirGaleria = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            abrirCamara = abrirCamara,
            pedirPermiso = { permiso.launch(Manifest.permission.CAMERA) },
            borrar = onBorrar
        )
    }
}

/** Archivo temporal (en la caché de la app) donde la cámara guarda la foto; se reemplaza cada vez. */
private fun nuevoArchivoDeFoto(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "perfil_camara.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    DINATheme {
        MenuContenido(
            usuario = Usuario("admin.prueba@duocuc.cl", "Admin de Prueba", Rol.ESTUDIANTE, esAdmin = true),
            modoTema = ModoTema.SISTEMA,
            subiendoFoto = false,
            acciones = MenuAcciones()
        )
    }
}
