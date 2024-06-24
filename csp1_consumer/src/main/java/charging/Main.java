package charging;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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
        // What do you need to configure here?
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CSP1TransactionDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        long processingTimeMs = Long.parseLong(props.getProperty("processing.time.ms", "1"));
        boolean logInfos = props.getProperty("app.log.infos", "true").equals("true");

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
                    // "Processing" Message
                    Thread.sleep(processingTimeMs);
                    if (logInfos) {
                        System.out.println(key + ": " + value);
                    }
                }
            }
        }
    }
}
