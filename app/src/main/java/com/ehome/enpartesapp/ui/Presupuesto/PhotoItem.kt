package com.ehome.enpartesapp.ui.Presupuesto

import android.net.Uri

data class PhotoItem(
    var photoUri: Uri? = null,
    var photoType: String = "",
    var photoKey: String = ""
)