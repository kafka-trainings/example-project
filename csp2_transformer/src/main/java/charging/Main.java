package charging;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(final String[] args) throws IOException {

        final Properties props = new Properties();
        String configFile = "csp2_transformer.properties";
        if(args.length == 1) {
            configFile = args[0];
        }
        props.load(new FileReader(configFile));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());


        final Topology topology = getTopology(props);
        System.out.println("you can paste the topology into this site for a vizualization: https://zz85.github.io/kafka-streams-viz/");
        System.out.println(topology.describe());
        final KafkaStreams streams = new KafkaStreams(topology, props);




        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("<<< Stopping the streams-app Application");
            streams.close();
            latch.countDown();
        }));

        try {
            streams.start();

            ReadOnlyKeyValueStore<String, String> cimStore = streams.store(
                    StoreQueryParameters.fromNameAndType("cim-store", QueryableStoreTypes.keyValueStore()));
            System.out.println("NUMENTRIES: " + cimStore.approximateNumEntries());
            KeyValueIterator<String, String> it = cimStore.all();
            while(it.hasNext()) {
                KeyValue<String, String> kv = it.next();
                System.out.println(kv);
            }
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static Topology getTopology(Properties props) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, CSP2Transaction> cspTransactions = builder.stream(props.getProperty("csp2.transactions.topic"),
                Consumed.with(Serdes.String(), JSONSerdes.CSP2TransactionSerde()));
        GlobalKTable<Integer, String> customerIdMapping = builder.globalTable(props.getProperty("csp2.customer_id_mapping.topic"),
                Materialized.<Integer, String, KeyValueStore<Bytes, byte[]>> as("cim-store")
                        .withKeySerde(Serdes.Integer())
                        .withValueSerde(Serdes.String()));
        GlobalKTable<Integer, String> chargingstationIdMapping = builder.globalTable(props.getProperty("csp2.chargingstation_id_mapping.topic"),
                Consumed.with(Serdes.Integer(), Serdes.String()));

        KStream<String, CSP2TransactionWithCustomerId> cspTransactionsWithCustomerId = cspTransactions.join(customerIdMapping,
                (key, transactionValue) -> transactionValue.customerId,
                (transaction, id) -> new CSP2TransactionWithCustomerId(id, transaction.chargingStationId, transaction.whCharged));

        KStream<String, Transaction> transactions = cspTransactionsWithCustomerId.join(chargingstationIdMapping,
                (key, transactionValue) -> transactionValue.chargingStationId,
                (transaction, id) -> new Transaction(transaction.customerId, id, transaction.whCharged / 1000, System.currentTimeMillis())
        )
                        .selectKey((key, value) -> value.customerId);
        transactions.to(props.getProperty("output.topic"), Produced.with(Serdes.String(), JSONSerdes.TransactionSerde()));

        return builder.build();
    }

    private static class CSP2TransactionWithCustomerId {
        String customerId;
        Integer chargingStationId;
        float whCharged;

        public CSP2TransactionWithCustomerId(String customerId, Integer chargingStationId, float whCharged) {
            this.customerId = customerId;
            this.chargingStationId = chargingStationId;
            this.whCharged = whCharged;
        }
    }
}
