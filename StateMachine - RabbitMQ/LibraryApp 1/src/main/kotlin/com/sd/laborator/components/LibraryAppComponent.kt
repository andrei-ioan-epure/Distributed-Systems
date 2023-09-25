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

        val(from,message)=msg.split(":")
        println("Am primit de la $from")
        val processedMsg = ( message.substring(1,message.length-1).split(", ").map { it.toInt()}).toMutableList()
        try {
            if (processedMsg.last() >14)
            {
                println("Trimit catre 3")
                sendMessage3("2:$processedMsg")
            }
            else {
                println("Trimit catre 2")
                processedMsg.last().plus(2)
                processedMsg[processedMsg.size-1]+=2
                sendMessage2("2:$processedMsg")

            }
        } catch (e: Exception) {
            println(e)
        }
    }


}