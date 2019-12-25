package Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Slightly edited version of this comment's code https://gamedev.stackexchange.com/a/162987
 * @param <T>
 */
public class WeightedRandomBag<T> {

    private class Entry {
        double accumulatedWeight;
        T object;
        private Entry(double accumulatedWeight, T object){
            this.accumulatedWeight = accumulatedWeight;
            this.object = object;
        }
    }

    private List<Entry> entries = new ArrayList<>();
    private double accumulatedWeight;
    private Random rand = new Random();

    public void addEntry(T object, double weight) {
        accumulatedWeight += weight;
        entries.add(new Entry(accumulatedWeight, object));
    }

    public T getRandom() {
        double r = rand.nextDouble() * accumulatedWeight;
        for (Entry entry : entries) {
            if (entry.accumulatedWeight >= r) return entry.object;
        }
        return null;
    }
}