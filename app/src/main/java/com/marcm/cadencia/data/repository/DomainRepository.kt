package com.marcm.cadencia.data.repository

import com.marcm.cadencia.data.local.CategoryDao
import com.marcm.cadencia.data.local.CategoryEntity
import com.marcm.cadencia.data.local.DomainDao
import com.marcm.cadencia.data.local.DomainEntity
import com.marcm.cadencia.data.local.TaskDao
import com.marcm.cadencia.data.local.toDomain
import com.marcm.cadencia.data.local.toEntity
import com.marcm.cadencia.domain.model.Category
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Ámbitos y sus categorías. Activar un ámbito es lo que lo hace aparecer en Hoy, en el
 * Plan y en los chips de filtro; desactivarlo lo esconde sin borrar sus tareas.
 */
class DomainRepository(
    private val domainDao: DomainDao,
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao
) {

    fun observeAll(): Flow<List<Domain>> =
        domainDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Domain>> =
        domainDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Domain> = domainDao.getAll().map { it.toDomain() }

    suspend fun getActive(): List<Domain> = domainDao.getActive().map { it.toDomain() }

    suspend fun getByKey(key: String): Domain? = domainDao.getByKey(key)?.toDomain()

    suspend fun setActive(domainId: Long, active: Boolean) = domainDao.setActive(domainId, active)

    suspend fun update(domain: Domain) = domainDao.update(domain.toEntity())

    /** Borra un ámbito propio (con sus categorías y tareas en cascada). Los de fábrica no se borran. */
    suspend fun deleteCustom(domain: Domain) {
        if (!domain.isBuiltIn) domainDao.delete(domain.toEntity())
    }

    /**
     * Crea un ámbito propio con su categoría "General" y lo deja activo.
     * La clave se genera para no chocar con las de fábrica.
     */
    suspend fun createCustomDomain(name: String, colorHex: String, iconKey: String): Long {
        val sortOrder = (domainDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = domainDao.insert(
            DomainEntity(
                key = "custom_${System.currentTimeMillis()}",
                name = name.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                sortOrder = sortOrder,
                isActive = true,
                isBuiltIn = false
            )
        )
        categoryDao.insert(
            CategoryEntity(
                domainId = id,
                key = Category.GENERAL_KEY,
                name = Category.GENERAL_NAME,
                iconKey = null,
                sortOrder = 0
            )
        )
        return id
    }

    /** Cuántas tareas tiene el ámbito, esté activo o no. */
    suspend fun taskCount(domainId: Long): Int = taskDao.countForDomain(domainId)

    /**
     * Activa el ámbito y siembra las tareas indicadas. Cada preset va a su categoría por
     * clave; si esa categoría no existiera, cae en la "General" del ámbito.
     *
     * Si el ámbito ya tiene tareas, no se siembra nada: reactivarlo desde Ajustes lo
     * vuelve a mostrar tal como estaba, sin duplicar las sugerencias.
     */
    suspend fun activateWithPresets(
        domainId: Long,
        presets: List<Preset>,
        today: LocalDate = LocalDate.now()
    ) {
        domainDao.setActive(domainId, true)
        if (presets.isEmpty() || taskDao.countForDomain(domainId) > 0) return

        val general = categoryDao.getByKey(domainId, Category.GENERAL_KEY)
        val tasks = presets.mapNotNull { preset ->
            val categoryId = categoryDao.getByKey(domainId, preset.categoryKey)?.id ?: general?.id
            categoryId?.let { preset.toTask(it, today).toEntity() }
        }
        taskDao.insertAll(tasks)
    }

    /** Categoría por defecto de un ámbito, destino de las tareas creadas a mano. */
    suspend fun generalCategoryId(domainId: Long): Long? =
        categoryDao.getByKey(domainId, Category.GENERAL_KEY)?.id

    suspend fun categoriesOf(domainId: Long): List<Category> =
        categoryDao.getForDomain(domainId).map { it.toDomain() }
}
