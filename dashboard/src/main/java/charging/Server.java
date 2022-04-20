package charging;

import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Server implements Closeable {
    public static final String TRANSACTIONS_COUNT_BY_CUSTOMERS_STORE = "transactions_count_by_customer";
    public static final String TRANSACTIONS_COUNT_BY_CHARGING_STATION_STORE = "transactions_count_by_charging_station";
    public static final String TRANSACTIONS_CHARGED_BY_CUSTOMERS_STORE = "transactions_charged_by_customer";
    public static final String TRANSACTIONS_CHARGED_BY_CHARGING_STATION_STORE = "transactions_charged_by_charging_station";
    private final KafkaStreams streams;
    private final HttpServer server;

    public Server(int port, KafkaStreams streams) throws IOException {
        this.streams = streams;

        System.out.println(port);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", httpExchange -> {
            try {
                byte[] responseBytes = getMetrics().getBytes();
                httpExchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                byte[] responseBytes="Waiting for Kafka Streams to Start".getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(500, responseBytes.length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }

        });
        new Thread(server::start).start();
    }

    private String getMetrics() {
        StringBuilder response = new StringBuilder();
        response.append("""
                # HELP transaction_count_per_chargingstation Transactions per Charging Station
                # TYPE transaction_count_per_chargingstation counter
                """);
        Map<String, Long> countPerCST = getStoreValues(TRANSACTIONS_COUNT_BY_CHARGING_STATION_STORE);
        countPerCST.forEach((key, value) -> response.append(String.format("transaction_count_per_chargingstation{charging_station = \"%s\"} %d\n", key, value)));


        response.append("""
                # HELP transaction_count_per_customer Transactions per Charging Station
                # TYPE transaction_count_per_customer counter
                """);
        Map<String, Long> countPerCustomer = getStoreValues(TRANSACTIONS_COUNT_BY_CUSTOMERS_STORE);
        countPerCustomer.forEach((key, value) -> response.append(String.format("transaction_count_per_customer{customer = \"%s\"} %d\n", key, value)));


        response.append("""
                # HELP transaction_charged_per_chargingstation Transactions per Charging Station
                # TYPE transaction_charged_per_chargingstation counter
                """);
        Map<String, Double> sumPerCST = getStoreValues(TRANSACTIONS_CHARGED_BY_CHARGING_STATION_STORE);
        sumPerCST.forEach((key, value) -> response.append(String.format("transaction_charged_per_chargingstation{charging_station = \"%s\"} %f\n", key, value)));


        response.append("""
                # HELP transaction_charged_per_customer Transactions per Customer
                # TYPE transaction_charged_per_customer counter
                """);
        Map<String, Double> sumPerCustomer = getStoreValues(TRANSACTIONS_CHARGED_BY_CUSTOMERS_STORE);
        sumPerCustomer.forEach((key, value) -> response.append(String.format("transaction_charged_per_customer{customer = \"%s\"} %f\n", key, value)));

        return response.toString();
    }

    <K, V> Map<K, V> getStoreValues(String storeName) {
        HashMap<K, V> values = new HashMap<>();
        ReadOnlyKeyValueStore<K, V> store = getStoreByName(storeName);
        KeyValueIterator<K, V> iterator = store.all();
        while (iterator.hasNext()) {
            KeyValue<K, V> kv = iterator.next();
            values.put(kv.key, kv.value);
        }
        return values;
    }

    <K, V> ReadOnlyKeyValueStore<K, V> getStoreByName(String storeName) {
        return streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()));
    }

    @Override
    public void close() {
        server.stop(1);
    }
}
