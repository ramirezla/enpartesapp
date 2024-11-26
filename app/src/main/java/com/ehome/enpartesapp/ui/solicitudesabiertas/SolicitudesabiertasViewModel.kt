package com.ehome.enpartesapp.ui.solicitudesabiertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SolicitudesabiertasViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Solicitudesabiertas Fragment"
    }
    val text: LiveData<String> = _text
}