package com.sd.laborator

import io.micronaut.core.annotation.Introspected

@Introspected
class EratosteneRequest {
    private lateinit var number: List<Int>

    fun getNumber(): List<Int> {
        return number.toList()
    }
}