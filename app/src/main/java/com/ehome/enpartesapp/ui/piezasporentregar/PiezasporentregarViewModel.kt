package com.ehome.enpartesapp.ui.piezasporentregar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PiezasporentregarViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Piezasporentregar Fragment"
    }
    val text: LiveData<String> = _text
}