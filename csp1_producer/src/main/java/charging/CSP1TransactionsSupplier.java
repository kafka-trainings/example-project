package charging;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class CSP1TransactionsSupplier implements Supplier<CSP1Transaction> {
    private final static List<String> RANDOM_NAMES = List.of("Alice", "Bob", "Charlie", "Dave", "Eve", "Francis");
    private final static List<String> RANDOM_CST = List.of("BER", "PRG", "LHR", "LAX", "SYD");

    private final Random rand = new Random();

    private final double msgsPerSec;

    public CSP1TransactionsSupplier(double msgsPerSec) {
        this.msgsPerSec = msgsPerSec;
    }

    @Override
    public CSP1Transaction get() {

        CSP1Transaction transaction = new CSP1Transaction();
        transaction.customerId = RANDOM_NAMES.get(rand.nextInt(RANDOM_NAMES.size()));
        transaction.chargingStationId = RANDOM_CST.get(rand.nextInt(RANDOM_CST.size()));
        transaction.kwhCharged = rand.nextFloat() * 100;

        if (msgsPerSec != -1) {
            try {
                Thread.sleep((long) ((1/msgsPerSec)*1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return transaction;
    }
}
