package com.sd.laborator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StudentMicroservice {
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket
    private var subscribers: HashMap<Int, Socket>
    private lateinit var studentSocket: ServerSocket


    init {
        subscribers = hashMapOf()

        val databaseLines: List<String> = File("questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0 until databaseLines.size step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }
    }

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
    }




    private fun getActiveList(){
        println("Trimit catre MessageManager: ${"get ${messageManagerSocket.localPort}\n"}")
        messageManagerSocket.getOutputStream().write(("get ${messageManagerSocket.localPort}\n").toByteArray())
        // se asteapta raspuns de la MessageManager
        val messageManagerBufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
        try {
            val activeList = messageManagerBufferReader.readLine()
            println(activeList)
            val activeMembers=activeList.split(" ")
            //synchronized(subscribers) {
            if (subscribers.isNotEmpty())
                subscribers = hashMapOf()
            activeMembers.forEach {
                val (port, address) = it.split("-")
                subscribers[port.toInt()] = Socket(address, port.toInt()+1)

                //  }
            }
            println(subscribers)
            return
        } catch (e: SocketTimeoutException) {
            println("Nu a venit niciun raspuns in timp util. 2")
        }
    }


    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question) {
                return it.second
            }
        }
        return null
    }

    private fun broadcastMessage( message: String, only: Int) {
        subscribers.forEach {
            it.takeIf { it.key != only }
                ?.value?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }


    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        studentSocket = ServerSocket(messageManagerSocket.localPort+1)

        println("StudentMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta mesaje...")


        thread {

            while (true) {
                println("Astept conexiune")
                val clientConnection = studentSocket.accept()
                println("am prins ceva")

                getActiveList()

                val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))

                // se asteapta intrebari trimise prin intermediarul "MessageManager"
                val response = bufferReader.readLine()

                if (response == null) {
                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    println("Microserviciul MessageService (${clientConnection.port}) a fost oprit.")
                    bufferReader.close()
                    clientConnection.close()
                    break
                }


                // se foloseste un thread separat pentru tratarea intrebarii primite
                thread {
                    val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)

                    when (messageType) {
                        // tipul mesajului cunoscut de acest microserviciu este de forma:
                        // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                        "intrebare" -> {
                            println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                            var responseToQuestion = respondToQuestion(messageBody)
                            responseToQuestion?.let {
                                responseToQuestion = "raspuns ${messageManagerSocket.localPort+1} $it"
                                println("Trimit raspunsul: \"${responseToQuestion}\"")
                                broadcastMessage(responseToQuestion!!,only=messageDestination.toInt())


                                //messageManagerSocket.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}