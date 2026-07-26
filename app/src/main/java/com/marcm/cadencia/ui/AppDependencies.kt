package com.marcm.cadencia.ui

import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.marcm.cadencia.AppContainer
import com.marcm.cadencia.KuseApp

/** Obtiene el contenedor de dependencias desde la Application en un Factory de ViewModel. */
fun CreationExtras.appContainer(): AppContainer =
    (this[APPLICATION_KEY] as KuseApp).container
