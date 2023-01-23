package charging;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class CSP2TransactionsSupplier implements Supplier<CSP2Transaction> {
    private final static List<Integer> RANDOM_CUSTOMERS = List.of(373451, 670990, 681601, 813366, 697992, 980555);
    private final static List<Integer> RANDOM_CST = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    private final Random rand = new Random();

    private final int batchInterval;
    private final int msgsPerBatch;
    private boolean logInfos;
    private int numMsgs = 0;

    public CSP2TransactionsSupplier(int batchInterval, int msgsPerBatch, boolean logInfos) {
        this.batchInterval = batchInterval;
        this.msgsPerBatch = msgsPerBatch;
        this.logInfos = logInfos;
    }

    @Override
    public CSP2Transaction get() {

        CSP2Transaction transaction = new CSP2Transaction();
        transaction.customerId = RANDOM_CUSTOMERS.get(rand.nextInt(RANDOM_CUSTOMERS.size()));
        transaction.chargingStationId = RANDOM_CST.get(rand.nextInt(RANDOM_CST.size()));
        transaction.whCharged = rand.nextFloat() * 100000;
        numMsgs++;
        if (numMsgs >= msgsPerBatch) {
            if (logInfos) {
                System.out.println("Produced " + numMsgs + " Messages. Waiting now for the next Batch to 'arrive'");
            }
            try {
                Thread.sleep(batchInterval * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            numMsgs = 0;
        }

        return transaction;
    }
}
