package cl.app.dina

import cl.app.dina.data.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RolTest {

    @Test
    fun detectaRolPorDominio() {
        assertEquals(Rol.ESTUDIANTE, Rol.desdeCorreo("ana@duocuc.cl"))
        assertEquals(Rol.PROFESOR, Rol.desdeCorreo("juan@profesor.duoc.cl"))
    }

    @Test
    fun ignoraMayusculasYEspacios() {
        assertEquals(Rol.PROFESOR, Rol.desdeCorreo("  Juan@PROFESOR.Duoc.cl "))
    }

    @Test
    fun rechazaDominiosNoInstitucionales() {
        assertNull(Rol.desdeCorreo("ana@gmail.com"))
        assertNull(Rol.desdeCorreo("ana@falso.duocuc.cl"))   // subdominio inventado
        assertNull(Rol.desdeCorreo("ana@duocuc.cl.evil.com"))
        assertNull(Rol.desdeCorreo("sin-arroba"))
        assertNull(Rol.desdeCorreo(""))
    }
}
