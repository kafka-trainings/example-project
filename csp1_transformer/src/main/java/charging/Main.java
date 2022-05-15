package charging;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Main {
    public static void main(final String[] args) throws IOException {

        final Properties consumerProps = new Properties();
        final Properties producerProps = new Properties();
        String configFile = "csp1_transformer.properties";
        if(args.length == 1) {
            configFile = args[0];
        }
        consumerProps.load(new FileReader(configFile));
        producerProps.load(new FileReader(configFile));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CSP1TransactionDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TransactionSerializer.class);


        final String CSP1_TOPIC = consumerProps.getProperty("csp1.topic");
        final String OUTPUT_TOPIC = consumerProps.getProperty("output.topic");

        final Consumer<String, CSP1Transaction> consumer = new KafkaConsumer<>(consumerProps);
        final Producer<String, Transaction> producer = new KafkaProducer<>(producerProps);

        try (consumer) {
            try (producer) {
                producer.initTransactions();
                consumer.subscribe(List.of(CSP1_TOPIC));
                ConsumerGroupMetadata consumerGroupMetadata = consumer.groupMetadata();
                while (true) {
                    ConsumerRecords<String, CSP1Transaction> records = consumer.poll(Duration.ofMillis(100));

                    producer.beginTransaction();
                    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

                    for (ConsumerRecord<String, CSP1Transaction> record : records) {
                        CSP1Transaction csp1Transaction = record.value();
                        offsets.put(new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset() + 1));
                        Transaction transaction = new Transaction();
                        transaction.chargingStationId = csp1Transaction.chargingStationId;
                        transaction.customerId = "CSP1:" + csp1Transaction.customerId;
                        transaction.kwhCharged = csp1Transaction.kwhCharged;
                        transaction.timestamp = record.timestamp();

                        ProducerRecord<String, Transaction> transactionRecord =
                                new ProducerRecord<>(OUTPUT_TOPIC, transaction.customerId, transaction);
                        producer.send(transactionRecord);
                    }
                    producer.sendOffsetsToTransaction(offsets, consumerGroupMetadata);
                    producer.commitTransaction();
                }
            }
        }
    }
}
