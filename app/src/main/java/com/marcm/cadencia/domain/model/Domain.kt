package com.marcm.cadencia.domain.model

/**
 * Ámbito: el nivel superior del árbol (ámbito → categoría → tarea). Es lo que el
 * usuario activa o desactiva; al activarlo se siembran sus tareas sugeridas.
 *
 * El color viaja como hex porque el dominio no conoce la capa de UI; quien pinta lo
 * traduce en ui/theme/DomainColors.kt.
 *
 * @param key identificador estable para los ámbitos de fábrica ("higiene", "hogar"...).
 *   Los ámbitos creados por el usuario llevan una clave generada.
 * @param isBuiltIn false para los ámbitos que crea el usuario, que sí se pueden borrar.
 */
data class Domain(
    val id: Long = 0L,
    val key: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = false,
    val isBuiltIn: Boolean = true
)

/**
 * Catálogo de ámbitos de fábrica, con el color e icono del sistema visual.
 * Se siembran (inactivos) al crear la base de datos; el onboarding los activa.
 */
enum class BuiltInDomain(
    val key: String,
    val label: String,
    val iconKey: String,
    val colorHex: String,
    /** Resumen que muestra la tarjeta del onboarding, sin el recuento de sugeridas. */
    val summary: String
) {
    HIGIENE("higiene", "Higiene y grooming", "self_care", "#6FD8C0", "Dental, afeitado, piel"),
    HOGAR("hogar", "Hogar", "home", "#F0B95E", "Ropa, sábanas, limpieza"),
    EJERCICIO("ejercicio", "Ejercicio", "fitness_center", "#8FB6FF", "Fuerza, cardio, movilidad"),
    MASCOTAS("mascotas", "Mascotas", "pets", "#C9A6FF", "Antiparasitario, baño, arenero");

    fun toDomain(): Domain = Domain(
        key = key,
        name = label,
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = ordinal,
        isActive = false,
        isBuiltIn = true
    )

    companion object {
        fun fromKey(key: String): BuiltInDomain? = entries.firstOrNull { it.key == key }
    }
}
