package com.sd.laborator


import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TeacherMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket
    private lateinit var teacherResponseServerSocket: ServerSocket
    private var subscribers: HashMap<Int, Socket>

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val TEACHER_PORT = 1600
    }

    init {
        subscribers = hashMapOf()
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            messageManagerSocket.soTimeout = 3000
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun getActiveList():Boolean {
        println("Trimit catre MessageManager: ${"get ${messageManagerSocket.localPort}\n"}")
        messageManagerSocket.getOutputStream().write(("get ${messageManagerSocket.localPort}\n").toByteArray())
        // se asteapta raspuns de la MessageManager
        val messageManagerBufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
        try {
            val activeList = messageManagerBufferReader.readLine()
            println(activeList)
            if(activeList=="-")
            {
                subscribers= hashMapOf()
                return false
            }
            val activeMembers = activeList.split(" ")
            //synchronized(subscribers) {
            if (subscribers.isNotEmpty())
                subscribers = hashMapOf()
            activeMembers.forEach {
                val (port, address) = it.split("-")
                subscribers[port.toInt()] = Socket(address, port.toInt() + 1)

                //  }
            }
            println(subscribers)
            return true
        } catch (e: SocketTimeoutException) {
            println("Nu a venit niciun raspuns in timp util. 2")
        }
        return false
    }


    private fun broadcastMessage(message: String, except: List<Int>) {
        subscribers.forEach {
            it.takeIf { !except.contains(it.key) }
                ?.value?.getOutputStream()?.write((message + "\n").toByteArray())
            println("msg sent")
        }
    }

    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        // se porneste un socket server TCP pe portul 1600 care asculta pentru conexiuni
        teacherMicroserviceServerSocket = ServerSocket(TEACHER_PORT)
        teacherResponseServerSocket = ServerSocket(messageManagerSocket.localPort + 1)

        println("TeacherMicroservice se executa pe portul: ${teacherMicroserviceServerSocket.localPort}")
        println("Se asteapta cereri (intrebari)...")

        println("Se executa la ${messageManagerSocket.localPort + 1}")
        thread {
            while (true) {
                // se asteapta conexiuni din partea clientilor ce doresc sa puna o intrebare
                // (in acest caz, din partea aplicatiei client GUI)
                val clientConnection = teacherMicroserviceServerSocket.accept()

                // se foloseste un thread separat pentru tratarea fiecarei conexiuni client
                thread {
                    println("S-a primit o cerere de la: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")

                    // se citeste intrebarea dorita
                    val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                    val receivedQuestion = clientBufferReader.readLine()

                    // intrebarea este redirectionata catre microserviciul MessageManager
                    /*    println("Trimit catre MessageManager: ${"intrebare ${messageManagerSocket.localPort} $receivedQuestion\n"}")
                messageManagerSocket.getOutputStream().write(("intrebare ${messageManagerSocket.localPort} $receivedQuestion\n").toByteArray())*/

                    //cer lista de microservicii active




                   val s= getActiveList()

                    if(s) {
                        //intreb studentii activi
                        println("Broadcast ")
                        broadcastMessage(
                            "intrebare ${clientConnection.port} $receivedQuestion",
                            except = listOf(clientConnection.port, messageManagerSocket.port)
                        )


                        thread {

                            //astept raspunsuri
                            // while (true) {
                            println("Astept raspuns")


                            try {

                                val teacherResponseConnection = teacherResponseServerSocket.accept()

                                teacherResponseConnection.soTimeout = 3000

                                // val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))

                                /*  // se asteapta intrebari trimise prin intermediarul "MessageManager"
                                  val response = bufferReader.readLine()

                                  println("Response: $response")*/
                                // se asteapta raspuns de la MessageManager
                                val messageManagerBufferReader =
                                    BufferedReader(InputStreamReader(teacherResponseConnection.inputStream))// BufferedReader(InputStreamReader(clientConnection.inputStream))

                                // se asteapta intrebari trimise prin intermediarul "MessageManager"
                                val receivedResponse = messageManagerBufferReader.readLine()

                                println("Response: $receivedResponse")
                                // se trimite raspunsul inapoi clientului apelant
                                println("Am primit raspunsul: \"$receivedResponse\"")
                                clientConnection.getOutputStream().write((receivedResponse + "\n").toByteArray())
                            } catch (e: SocketTimeoutException) {
                                println("Nu a venit niciun raspuns in timp util.")
                                clientConnection.getOutputStream()
                                    .write("Nu a raspuns nimeni la intrebare\n".toByteArray())
                            } finally {
                                // se inchide conexiunea cu clientul
                                // teacherResponseConnection.close()
                                clientConnection.close()
                            }
                            //   }
                        }
                    }

             
                }
            }
        }

    }
}

fun main() {
    val teacherMicroservice = TeacherMicroservice()
    teacherMicroservice.run()
}