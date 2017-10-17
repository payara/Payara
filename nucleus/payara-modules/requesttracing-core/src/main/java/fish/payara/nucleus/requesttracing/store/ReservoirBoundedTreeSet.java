package fish.payara.nucleus.requesttracing.store;

import fish.payara.nucleus.notification.domain.BoundedTreeSet;
import java.util.Iterator;
import java.util.Random;

/**
 * Set which adds objects according to a reservoir sampling algorithm.
 * https://en.wikipedia.org/wiki/Reservoir_sampling.
 *
 * @param <N> the class of the items to be stored in the set
 */
public class ReservoirBoundedTreeSet<N extends Comparable> extends BoundedTreeSet<N> {

    private final Random random;
    private int counter;

    /**
     * Constructor which accepts a maximum size. This set requires a
     *
     * @param maxSize the maximum size of the set
     */
    public ReservoirBoundedTreeSet(int maxSize) {
        super(maxSize);
        random = new Random();
        counter = 0;
    }

    /**
     * Performs a reservoir sampling pass in order to add an object to the set.
     *
     * @param n The item to add
     * @return true if the item was added, or false otherwise.
     * @throws IndexOutOfBoundsException this exception shouldn't be thrown, but
     * will be if a problem happens in the reservoir sampling algorithm.
     */
    public boolean add(N n) {
        counter++;
        // If the list isn't full, add the item with p = 1
        if (size() < maxSize) {
            return super.add(n);
        }

        // p is the probability of keeping the new item
        double p = (double) maxSize / counter;
        boolean keepItem = random.nextFloat() < p;

        if (!keepItem) {
            return false;
        }

        // replace a random item in the list
        int itemToReplace = random.nextInt(size());
        for (Iterator<N> iterator = super.iterator(); iterator.hasNext(); itemToReplace--) {
            N item = iterator.next();
            if (itemToReplace == 0) {
                super.remove(item);
                return super.add(n);
            }
        }
        throw new IndexOutOfBoundsException("Error in adding to " + getClass().getName() + " in Request Tracing historical store. "
                + "A problem occurred in the reservoir sampling algorithm.");
    }
}
