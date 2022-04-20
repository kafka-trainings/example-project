package charging;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static charging.Server.*;

public class Dashboard {
    public static void main(final String[] args) throws IOException {

        final Properties props = new Properties();
        props.load(new FileReader("dashboard.properties"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        int port = Integer.parseInt(props.getProperty("web.port"));


        final Topology topology = getTopology();
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
            Server server = new Server(port, streams);
            streams.start();
            latch.await();
            server.close();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }


    private static Topology getTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Transaction> transactions = builder.stream("transactions", Consumed.with(Serdes.String(), JSONSerdes.TransactionSerde()));

        KGroupedStream<String, Transaction> byCustomer = transactions.groupByKey();
        KGroupedStream<String, Transaction> byChargingStation = transactions.groupBy((key, value) -> value.chargingStationId);

        byChargingStation.count(Materialized.as(TRANSACTIONS_COUNT_BY_CHARGING_STATION_STORE));
        byCustomer.count(Materialized.as(TRANSACTIONS_COUNT_BY_CUSTOMERS_STORE));

        Initializer<Double> sumInitializer = () -> 0.0;
        Aggregator<String, Transaction, Double> sumAggregator = (key, value, agg) -> agg + value.kwhCharged;

        byCustomer.aggregate(sumInitializer, sumAggregator, Materialized
                .<String, Double, KeyValueStore<Bytes, byte[]>>as(TRANSACTIONS_CHARGED_BY_CUSTOMERS_STORE)
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Double()));
        byChargingStation.aggregate(sumInitializer, sumAggregator,
                Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as(TRANSACTIONS_CHARGED_BY_CHARGING_STATION_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Double()));

        return builder.build();
    }
}
