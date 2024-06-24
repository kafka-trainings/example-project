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
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {

        final Properties props = new Properties();
        if (args.length == 1) {
            props.load(new FileReader(args[0]));
        }
        // Load from environment variables
        props.putAll(System.getenv()
                .entrySet()
                .stream()
                .filter(mapEntry -> mapEntry.getKey().startsWith("KAFKA_"))
                .collect(Collectors.toMap(
                        mapEntry -> {
                            String envVar = mapEntry.getKey();
                            return envVar.substring(envVar.indexOf("_") + 1).toLowerCase(Locale.ENGLISH).replace("_", ".");
                        },
                        Map.Entry::getValue)));

        System.out.println("Properties: " + props);

        // copy from Props
        final Properties consumerProps = new Properties();
        consumerProps.putAll(props);
        final Properties producerProps = new Properties();
        producerProps.putAll(props);


        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CSP1TransactionDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TransactionSerializer.class);

        long processingTimeMs = Long.parseLong(producerProps.getProperty("processing.time.ms", "1"));
        boolean logInfos = producerProps.getProperty("app.log.infos", "true").equals("true");


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

                    for (ConsumerRecord<String, CSP1Transaction> record : records) {
                        producer.beginTransaction();
                        HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                        CSP1Transaction csp1Transaction = record.value();
                        offsets.put(new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset() + 1));
                        Transaction transaction = new Transaction();
                        transaction.chargingStationId = csp1Transaction.chargingStationId;
                        transaction.customerId = "CSP1:" + csp1Transaction.customerId;
                        transaction.kwhCharged = csp1Transaction.kwhCharged;
                        transaction.timestamp = record.timestamp();

                        // "Processing" Message
                        Thread.sleep(processingTimeMs);

                        ProducerRecord<String, Transaction> transactionRecord =
                                new ProducerRecord<>(OUTPUT_TOPIC, transaction.customerId, transaction);
                        producer.send(transactionRecord);
                        if (logInfos) {
                            System.out.println("Processed message for customer " + transaction.customerId);
                        }
                        producer.sendOffsetsToTransaction(offsets, consumerGroupMetadata);
                        producer.commitTransaction();
                    }
                }
            }
        }
    }
}
