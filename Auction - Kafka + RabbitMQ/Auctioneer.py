from kafka import KafkaConsumer, KafkaProducer

from mq_communication import RabbitMq


class Auctioneer:
    def __init__(self, bids_topic, notify_message_processor_topic, journal_topic):
        super().__init__()

        self.rabbit_mq = RabbitMq()

        self.bids_topic = bids_topic
        self.notify_processor_topic = notify_message_processor_topic
        self.journal_topic = journal_topic

        # consumatorul pentru ofertele de la licitatie
        self.bids_consumer = KafkaConsumer(
            self.bids_topic,
            auto_offset_reset="earliest",  # mesajele se preiau de la cel mai vechi la cel mai recent
            group_id="auctioneers",
            consumer_timeout_ms=15_000  # timeout de 15 secunde
        )

        # producatorul pentru notificarea procesorului de mesaje
        self.notify_processor_producer = KafkaProducer()
        self.journal_topic_producer = KafkaProducer()

    def send_request(self, request):
        self.rabbit_mq.send_message(message=request)
        self.rabbit_mq.receive_message()

    def receive_bids(self):
        # se preiau toate ofertele din topicul bids_topic
        print("Astept oferte pentru licitatie...")
        for msg in self.bids_consumer:
            for header in msg.headers:
                if header[0] == "identity":
                    identity = str(header[1], encoding="utf-8")
                elif header[0] == "amount":
                    bid_amount = int.from_bytes(header[1], 'big')

            print("{} a licitat {}".format(identity, bid_amount))

        message = bytearray("Am primit licitatii", encoding="utf-8")
        self.journal_topic_producer.send(topic=self.journal_topic, value=message)
        self.journal_topic_producer.flush()
        self.send_request(message)

        # bids_consumer genereaza exceptia StopIteration atunci cand se atinge timeout-ul de 10 secunde
        # => licitatia se incheie dupa ce timp de 15 secunde nu s-a primit nicio oferta
        self.finish_auction()

    def finish_auction(self):
        print("Licitatia s-a incheiat!")
        self.bids_consumer.close()

        # se notifica MessageProcessor ca poate incepe procesarea mesajelor
        auction_finished_message = bytearray("incheiat", encoding="utf-8")
        self.notify_processor_producer.send(topic=self.notify_processor_topic, value=auction_finished_message)
        self.notify_processor_producer.flush()
        self.notify_processor_producer.close()

        message = bytearray("Licitatia s-a incheiat!", encoding="utf-8")
        self.journal_topic_producer.send(topic=self.journal_topic, value=message)
        self.journal_topic_producer.flush()
        self.journal_topic_producer.close()
        self.send_request(message)

    def run(self):
        self.receive_bids()


if __name__ == '__main__':
    auctioneer = Auctioneer(
        bids_topic="topic_oferte",
        notify_message_processor_topic="topic_notificare_procesor_mesaje",
        journal_topic="topic_jurnal"
    )
    auctioneer.run()
