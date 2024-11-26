package com.ehome.enpartesapp.ui.consultasabiertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConsultasabiertasViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Consultasabiertas Fragment"
    }
    val text: LiveData<String> = _text
}