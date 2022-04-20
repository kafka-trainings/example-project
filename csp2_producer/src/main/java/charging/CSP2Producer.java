package charging;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Stream;

public class CSP2Producer {
    public static void main(final String[] args) throws IOException {
        final Properties props = new Properties();
        props.load(new FileReader("csp2_producer.properties"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CSP2TransactionSerializer.class);

        final String TOPIC = props.getProperty("topic");
        int batchInterval = Integer.parseInt(props.getProperty("producer.batch_interval.s"));
        int msgsPerBatch = Integer.parseInt(props.getProperty("producer.msgs_per_batch"));

        final Stream<CSP2Transaction> toGreet = Stream.generate(new CSP2TransactionsSupplier(batchInterval, msgsPerBatch));

        try (Producer<String, CSP2Transaction> producer = new KafkaProducer<>(props)) {
            toGreet.forEach(greeting -> {
                ProducerRecord<String, CSP2Transaction> producerRecord = new ProducerRecord<>(TOPIC, greeting);
                producer.send(producerRecord);
            });
        }
    }
}
