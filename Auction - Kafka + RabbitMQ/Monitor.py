from kafka import KafkaConsumer
from mq_communication2 import RabbitMq

class Monitor:
    def __init__(self, journal_topic):
        super().__init__()
        self.journal_topic = journal_topic
        self.rabbit_mq = RabbitMq()


        # consumatorul
        self.journal_consumer = KafkaConsumer(
            self.journal_topic,
            auto_offset_reset="earliest"  # mesajele se preiau de la cel mai vechi la cel mai recent
        )


    def write(self):



        print("Astept mesaj..")
        self.rabbit_mq.receive_message()




        w=open('result.txt','w')
        print("Scriu in fisier...")
        for msg in self.journal_consumer:
            msg= str(msg.value, encoding="utf-8")
            w.write(msg+"\n")




        self.journal_topic.close()


    def run(self):
        self.write()


if __name__ == '__main__':
    monitor = Monitor(
        journal_topic="topic_jurnal"
    )
    monitor.run()
