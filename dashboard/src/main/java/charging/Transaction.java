package charging;

public class Transaction {
    String customerId;
    String chargingStationId;
    float kwhCharged;
    long timestamp;

    public Transaction(String customerId, String chargingStationId, float kwhCharged, long timestamp) {
        this.customerId = customerId;
        this.chargingStationId = chargingStationId;
        this.kwhCharged = kwhCharged;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Customer " + customerId + " charged " + kwhCharged + " kWh at the charging station " + chargingStationId + " at timestamp " + timestamp;
    }
}
