package charging;

public class Transaction {
    String customerId;
    String chargingStationId;
    float kwhCharged;
    long timestamp;

    @Override
    public String toString() {
        return "Customer " + customerId + " charged " + kwhCharged + " kWh at the charging station " + chargingStationId + " at timestamp " + timestamp;
    }
}
