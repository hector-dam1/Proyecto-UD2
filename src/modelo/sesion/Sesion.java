package modelo.sesion;

import modelo.entidades.Cliente;

/**
 * Mantiene el cliente identificado durante la ejecucion de la aplicacion.
 * La informacion se conserva al cambiar de pantalla y se pierde al cerrar
 * la aplicacion.
 */
public final class Sesion {

    private static Cliente clienteActual;

    private Sesion() {
    }

    public static Cliente getClienteActual() {
        return clienteActual;
    }

    public static void setClienteActual(Cliente cliente) {
        clienteActual = cliente;
    }

    public static void limpiar() {
        clienteActual = null;
    }
}
