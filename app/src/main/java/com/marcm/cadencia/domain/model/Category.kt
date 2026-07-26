package com.marcm.cadencia.domain.model

/**
 * Categoría: agrupa tareas dentro de un ámbito. La UI selecciona ámbito, no categoría,
 * así que cada ámbito tiene una categoría "General" a la que van a parar las tareas
 * que el usuario crea a mano.
 *
 * @param key identificador estable dentro de su ámbito ("dental", "general"...).
 * @param iconKey icono propio; si es null se usa el del ámbito.
 */
data class Category(
    val id: Long = 0L,
    val domainId: Long,
    val key: String,
    val name: String,
    val iconKey: String? = null,
    val sortOrder: Int = 0
) {
    companion object {
        /** Clave de la categoría por defecto que tiene todo ámbito. */
        const val GENERAL_KEY = "general"
        const val GENERAL_NAME = "General"
    }
}

/**
 * Plantilla de categoría de fábrica, con el ámbito al que pertenece.
 * Se siembra junto a los ámbitos al crear la base de datos.
 */
data class CategoryTemplate(
    val domain: BuiltInDomain,
    val key: String,
    val name: String,
    val iconKey: String? = null
)

/** Categorías de fábrica de cada ámbito, en orden de aparición. */
object CategoryCatalog {

    val all: List<CategoryTemplate> = listOf(
        CategoryTemplate(BuiltInDomain.HIGIENE, "dental", "Higiene dental", "dentistry"),
        CategoryTemplate(BuiltInDomain.HIGIENE, "afeitado", "Afeitado y barba", "content_cut"),
        CategoryTemplate(BuiltInDomain.HIGIENE, "vello_facial", "Vello facial", "face"),
        CategoryTemplate(BuiltInDomain.HIGIENE, "piel", "Cuidado de la piel", "water_drop"),

        CategoryTemplate(BuiltInDomain.HOGAR, "ropa", "Ropa", "local_laundry_service"),
        CategoryTemplate(BuiltInDomain.HOGAR, "dormitorio", "Dormitorio", "bed"),
        CategoryTemplate(BuiltInDomain.HOGAR, "limpieza", "Limpieza", "cleaning_services"),
        CategoryTemplate(BuiltInDomain.HOGAR, "plantas", "Plantas", "potted_plant"),

        CategoryTemplate(BuiltInDomain.EJERCICIO, "fuerza", "Fuerza", "fitness_center"),
        CategoryTemplate(BuiltInDomain.EJERCICIO, "cardio", "Cardio", "directions_run"),
        CategoryTemplate(BuiltInDomain.EJERCICIO, "movilidad", "Movilidad", "self_improvement"),

        CategoryTemplate(BuiltInDomain.MASCOTAS, "salud", "Salud", "vaccines"),
        CategoryTemplate(BuiltInDomain.MASCOTAS, "cuidado", "Cuidado", "pets"),
        CategoryTemplate(BuiltInDomain.MASCOTAS, "entorno", "Entorno", "mop")
    )

    fun forDomain(domain: BuiltInDomain): List<CategoryTemplate> = all.filter { it.domain == domain }
}
