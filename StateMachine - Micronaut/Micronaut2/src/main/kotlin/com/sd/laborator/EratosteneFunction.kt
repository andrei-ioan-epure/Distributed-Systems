package com.sd.laborator;

import io.micronaut.function.FunctionBean
import io.micronaut.function.executor.FunctionInitializer
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Function

@FunctionBean("eratostene")
class EratosteneFunction : FunctionInitializer(), Function<EratosteneRequest, EratosteneResponse> {
    @Inject
    private lateinit var eratosteneSieveService: EratosteneSieveService

    private val LOG: Logger = LoggerFactory.getLogger(EratosteneFunction::class.java)

    override fun apply(msg : EratosteneRequest) : EratosteneResponse {
        // preluare numar din parametrul de intrare al functiei
        val number = msg.getNumber().toMutableList()

        val response = EratosteneResponse()
        LOG.info("Se verfica valoarea ultimului element: $number...")
        // se verifica daca numarul nu depaseste maximul
        if(eratosteneSieveService.checkSum(number))
        {
            number[number.size-1] = number.last() + 2
            khttp.post(
                url = "http://localhost:8081/",
                json= mapOf("number" to number)
            )
            LOG.info("Am trimis spre starea 2")
        }
        else{
            khttp.post(
                url = "http://localhost:8082/",
                json= mapOf("number" to number)
            )
            LOG.info("Am trimis spre starea 3")

        }

   
        response.setPrimes(number)
        LOG.info("Am terminat!")
        return response
    }   
}

/**
 * This main method allows running the function as a CLI application using: echo '{}' | java -jar function.jar 
 * where the argument to echo is the JSON to be parsed.
 */
fun main(args : Array<String>) { 
    val function = EratosteneFunction()
    function.run(args, { context -> function.apply(context.get(EratosteneRequest::class.java))})
}