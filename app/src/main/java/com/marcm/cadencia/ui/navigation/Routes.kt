package com.marcm.cadencia.ui.navigation

/** Rutas de navegación. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val ALL = "all"
    const val PLAN = "plan"
    const val STREAKS = "streaks"
    const val SETTINGS = "settings"
    const val PIN_SETUP = "pin_setup"

    const val DETAIL = "detail/{taskId}"
    fun detail(taskId: Long) = "detail/$taskId"

    // taskId = -1 para "nueva tarea".
    const val EDIT = "edit/{taskId}"
    fun edit(taskId: Long = -1L) = "edit/$taskId"

    const val ARG_TASK_ID = "taskId"
}
