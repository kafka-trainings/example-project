package charging;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(final String[] args) throws IOException {
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
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CSP1TransactionSerializer.class);

        final String TOPIC = props.getProperty("topic");
        double msgsPerSec = Double.parseDouble(props.getProperty("producer.msgs.per.sec", "1"));
        boolean logInfos = props.getProperty("app.log.infos", "true").equals("true");

        final Stream<CSP1Transaction> toGreet = Stream.generate(new CSP1TransactionsSupplier(msgsPerSec));

        try (Producer<String, CSP1Transaction> producer = new KafkaProducer<>(props)) {
            toGreet.forEach(greeting -> {
                ProducerRecord<String, CSP1Transaction> producerRecord = new ProducerRecord<>(TOPIC, greeting);
                producer.send(producerRecord);
                if (logInfos) {
                    System.out.println("Produced transaction for customer " + greeting.customerId);
                }
            });
        }
    }
}
