package com.ehome.enpartesapp.ui.piezasabiertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PiezasabiertasViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Piezasabiertas Fragment"
    }
    val text: LiveData<String> = _text
}