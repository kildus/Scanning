package com.ildus.scanning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PackViewModel: ViewModel(){

    var packs = mutableListOf<Pack>()
    var myData: MutableLiveData<String> = MutableLiveData()

}