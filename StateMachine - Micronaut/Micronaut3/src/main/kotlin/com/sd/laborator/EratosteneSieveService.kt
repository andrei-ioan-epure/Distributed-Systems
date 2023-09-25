package com.sd.laborator

import jakarta.inject.Singleton
import java.util.*

@Singleton
class EratosteneSieveService {

    fun checkParity(a:Int):Boolean{
        return a % 2 == 0
    }
}