package com.marcm.cadencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marcm.cadencia.domain.model.BuiltInDomain
import com.marcm.cadencia.domain.model.Category
import com.marcm.cadencia.domain.model.CategoryCatalog

@Database(
    entities = [
        DomainEntity::class,
        CategoryEntity::class,
        TaskEntity::class,
        CompletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KuseDatabase : RoomDatabase() {
    abstract fun domainDao(): DomainDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun completionDao(): CompletionDao

    companion object {
        private const val NAME = "kuse.db"

        /** Base de la app cuando se llamaba "Yo"; se borra al arrancar Kuse. */
        private const val LEGACY_NAME = "cadencia.db"

        @Volatile private var INSTANCE: KuseDatabase? = null

        fun get(context: Context): KuseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): KuseDatabase {
            context.deleteDatabase(LEGACY_NAME)
            return Room.databaseBuilder(context, KuseDatabase::class.java, NAME)
                .addCallback(SeedCallback)
                .build()
        }

        /**
         * Siembra los ámbitos de fábrica (inactivos) y sus categorías al crear la base.
         * El onboarding se limita a activar los que elija el usuario y a insertar las
         * tareas sugeridas; así las claves de ámbito y categoría existen desde el minuto
         * cero y nadie tiene que crearlas a mano.
         */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    BuiltInDomain.entries.forEach { d ->
                        db.execSQL(
                            "INSERT INTO domains (key, name, iconKey, colorHex, sortOrder, isActive, isBuiltIn) " +
                                "VALUES (?, ?, ?, ?, ?, 0, 1)",
                            arrayOf(d.key, d.label, d.iconKey, d.colorHex, d.ordinal)
                        )
                        // La categoría "General" recoge las tareas que el usuario crea a mano.
                        db.execSQL(
                            "INSERT INTO categories (domainId, key, name, iconKey, sortOrder) " +
                                "SELECT id, ?, ?, NULL, 0 FROM domains WHERE key = ?",
                            arrayOf(Category.GENERAL_KEY, Category.GENERAL_NAME, d.key)
                        )
                        CategoryCatalog.forDomain(d).forEachIndexed { index, c ->
                            db.execSQL(
                                "INSERT INTO categories (domainId, key, name, iconKey, sortOrder) " +
                                    "SELECT id, ?, ?, ?, ? FROM domains WHERE key = ?",
                                arrayOf(c.key, c.name, c.iconKey, index + 1, d.key)
                            )
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}
