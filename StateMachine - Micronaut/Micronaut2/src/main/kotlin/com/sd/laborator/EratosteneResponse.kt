package com.sd.laborator

import io.micronaut.core.annotation.Introspected

@Introspected
class EratosteneResponse {
	private var primes: List<Int>? = null

	fun getPrimes(): List<Int>? {
		return primes
	}

	fun setPrimes(primes: List<Int>?) {
		this.primes = primes
	}

}


