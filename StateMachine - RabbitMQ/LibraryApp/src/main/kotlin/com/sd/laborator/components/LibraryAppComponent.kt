package com.sd.laborator.components

import com.sd.laborator.interfaces.LibraryDAO
import com.sd.laborator.interfaces.LibraryPrinter
import com.sd.laborator.model.Book
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class LibraryAppComponent {
    @Autowired
    private lateinit var libraryDAO: LibraryDAO

    @Autowired
    private lateinit var libraryPrinter: LibraryPrinter

    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate() {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessage2(msg: String) {
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
                                         connectionFactory.getRoutingKey2(),
                                         msg)
    }

    fun sendMessage3(msg: String) {
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey3(),
            msg)
    }
    @RabbitListener(queues = ["\${libraryapp.rabbitmq.queue}"])
    fun recieveMessage(msg: String) {
        // the result needs processing
        println("Init msg : $msg")
        var list = (msg.split(",").map { it.toInt().toChar()  }).joinToString(separator="")
        list=list.split("=")[1]
        val data=list.split(",").map { it.toInt() }
        println("Proc msg $list")
        try {
            if(data.size<5)
            {
                println("Trimit catre 2")
                sendMessage2("1:$data")
            }
            else {
                println("Trimit catre 3")
                sendMessage3("1:$data")
            }
        } catch (e: Exception) {
            println(e)
        }
    }

}