package charging;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class CSP2TransactionSerializer implements Serializer<CSP2Transaction> {
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Override
    public byte[] serialize(String topic, CSP2Transaction data) {
        return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
    }
}
