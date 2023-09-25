package com.sd.laborator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MessageManagerMicroservice {

    private lateinit var messageManagerSocket: ServerSocket

    companion object Constants {
        const val MESSAGE_MANAGER_PORT = 2500
//        val writer = PrintWriter("result.txt")  // java.io.PrintWriter
      //  val writer=File("result.txt").bufferedWriter()
        val writer=File("result.txt")
        val bufferedReader: BufferedReader = File("/ProcessorMicroservice/src/main/resources/dictionar.txt").bufferedReader()
        val dictionar = bufferedReader.use { it.readText() }.split("\n")
    }


    public fun run() {
        // se porneste un socket server TCP pe portul 1500 care asculta pentru conexiuni
        messageManagerSocket = ServerSocket(MESSAGE_MANAGER_PORT)
        println("ProcessorManagerMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")

        while (true) {
            // se asteapta conexiuni din partea clientilor subscriberi
            val clientConnection = messageManagerSocket.accept()

            // se porneste un thread separat pentru tratarea conexiunii cu clientul
            thread {
                println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")



                val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))

                while (true) {
                    // se citeste raspunsul de pe socketul TCP
                    val receivedMessage = bufferReader.readLine()

                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedMessage == null) {
                        // deci subscriber-ul respectiv a fost deconectat
                        println("Subscriber-ul ${clientConnection.port} a fost deconectat.")

                        bufferReader.close()
                        clientConnection.close()
                        break
                    }

                    println("Primit mesaj: $receivedMessage")
                    val (messageType, messageDestination, messageBody) = receivedMessage.split(" ", limit = 3)

                    var cnt:Int = 0
                    // tipul mesajului de tip intrebare este de forma:
                    // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                    messageBody.split(" ").forEach {
                        if(dictionar.contains(it))
                            cnt++

                    }
                    println("Fraza contine $cnt din dictionarul de ${dictionar.size}")
                    if(cnt >= dictionar.size/2)
                    {
                        println("Scriu in file")
                      //  writer.append("$messageType : $messageBody\n")
                       // writer.use { out->out.println("$messageType : $messageBody\n") }
                        writer.writeText("$messageType : $messageBody\n")
                    }

                }
            }
        }
    }
}

fun main() {
    val messageManagerMicroservice = MessageManagerMicroservice()
    messageManagerMicroservice.run()
}
