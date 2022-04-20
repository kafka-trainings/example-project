package charging;

public class CSP1Transaction {
    String customerId;
    String chargingStationId;
    float kwhCharged;

    @Override
    public String toString() {
        return "Customer " + customerId + " charged " + kwhCharged + " kWh at the charging station " + chargingStationId;
    }
}
