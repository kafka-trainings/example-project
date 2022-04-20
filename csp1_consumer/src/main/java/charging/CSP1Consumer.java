package charging;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class CSP1Consumer {
    public static void main(final String[] args) throws IOException {
        final Properties props = new Properties();
        props.load(new FileReader("csp1_consumer.properties"));
        // What do you need to configure here?
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CSP1TransactionDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final String TOPIC = props.getProperty("topic");

        final Consumer<String, CSP1Transaction> consumer = new KafkaConsumer<>(props);

        try (consumer) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            System.out.println("Started…");
            while (true) {
                ConsumerRecords<String, CSP1Transaction> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, CSP1Transaction> record : records) {
                    String key = record.key();
                    CSP1Transaction value = record.value();
                    System.out.println(key + ": " + value);
                }
            }
        }
    }
}
