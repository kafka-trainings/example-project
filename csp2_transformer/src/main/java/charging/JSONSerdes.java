package charging;

import charging.common.JSONDeserializer;
import charging.common.JSONSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JSONSerdes {
    public static Serde<CSP2Transaction> CSP2TransactionSerde() {
        return Serdes.serdeFrom(new JSONSerializer<>(), new JSONDeserializer<>(CSP2Transaction.class));
    }

    public static Serde<Transaction> TransactionSerde() {
        return Serdes.serdeFrom(new JSONSerializer<>(), new JSONDeserializer<>(Transaction.class));
    }
}
