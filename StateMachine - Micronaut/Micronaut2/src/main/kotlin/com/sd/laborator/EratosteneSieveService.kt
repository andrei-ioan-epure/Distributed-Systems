package com.sd.laborator

import jakarta.inject.Singleton
import java.util.*

@Singleton
class EratosteneSieveService {

    fun checkSum(a:List<Int>):Boolean{
            return (a.last() < 14)
    }
}