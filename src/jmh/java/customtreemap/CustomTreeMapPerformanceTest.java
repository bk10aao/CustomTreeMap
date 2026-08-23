package customtreemap;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class CustomTreeMapPerformanceTest {

    @Param({ "10000", "20000", "30000", "40000", "50000", "70000", "80000", "90000", "100000"})
    private int size;

    private Integer[] keys;
    private CustomTreeMap<Integer, Integer> map;
    private Map<Integer, Integer> populatedHashMap;
    private SortedMap<Integer, Integer> populatedSortedMap;
    private Comparator<Integer> reverseComp;

    private int index;

    private Integer minKey;
    private Integer maxKey;
    private Integer midKey;

    @Setup(Level.Trial)
    public void setupTrial() {
        keys = new Integer[size];
        Random random = new Random(42);
        for (int i = 0; i < size; i++) {
            keys[i] = random.nextInt();
        }

        populatedHashMap = new HashMap<>();
        populatedSortedMap = new TreeMap<>();
        reverseComp = Comparator.reverseOrder();

        for (Integer key : keys) {
            populatedHashMap.put(key, key);
            populatedSortedMap.put(key, key);
        }

        minKey = populatedSortedMap.firstKey();
        maxKey = populatedSortedMap.lastKey();

        List<Integer> sortedList = new ArrayList<>(populatedSortedMap.keySet());
        midKey = sortedList.get(size / 2);

        index = 0;
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        map = new CustomTreeMap<>();
        for (Integer key : keys) {
            map.put(key, key);
        }
    }

    private int getNextKey() {
        return keys[Math.abs(index++ % size)];
    }

    @Benchmark
    public void testDefaultConstructor(Blackhole bh) {
        bh.consume(new CustomTreeMap<Integer, Integer>());
    }

    @Benchmark
    public void testComparatorConstructor(Blackhole bh) {
        bh.consume(new CustomTreeMap<Integer, Integer>(reverseComp));
    }

    @Benchmark
    public void testMapConstructor(Blackhole bh) {
        bh.consume(new CustomTreeMap<>(populatedHashMap));
    }

    @Benchmark
    public void testSortedMapConstructor(Blackhole bh) {
        bh.consume(new CustomTreeMap<>(populatedSortedMap));
    }

    @Benchmark
    public void testSize(Blackhole bh) {
        bh.consume(map.size());
    }

    @Benchmark
    public void testIsEmpty(Blackhole bh) {
        bh.consume(map.isEmpty());
    }

    @Benchmark
    public void testContainsKey(Blackhole bh) {
        bh.consume(map.containsKey(getNextKey()));
    }

    @Benchmark
    public void testContainsValue(Blackhole bh) {
        bh.consume(map.containsValue(getNextKey()));
    }

    @Benchmark
    public void testGet(Blackhole bh) {
        bh.consume(map.get(getNextKey()));
    }

    @Benchmark
    public void testComparator(Blackhole bh) {
        bh.consume(map.comparator());
    }

    @Benchmark
    public void testPut(Blackhole bh) {
        int k = getNextKey();
        bh.consume(map.put(k, k));
    }

    @Benchmark
    public void testPutAll(Blackhole bh) {
        CustomTreeMap<Integer, Integer> target = new CustomTreeMap<>();
        target.putAll(populatedHashMap);
        bh.consume(target);
    }

    @Benchmark
    public void testRemove(Blackhole bh) {
        bh.consume(map.remove(getNextKey()));
    }

    @Benchmark
    public void testClear(Blackhole bh) {
        map.clear();
        bh.consume(map);
    }

    @Benchmark
    public void testFirstKey(Blackhole bh) {
        bh.consume(map.firstKey());
    }

    @Benchmark
    public void testLastKey(Blackhole bh) {
        bh.consume(map.lastKey());
    }

    @Benchmark
    public void testFirstEntry(Blackhole bh) {
        bh.consume(map.firstEntry());
    }

    @Benchmark
    public void testLastEntry(Blackhole bh) {
        bh.consume(map.lastEntry());
    }

    @Benchmark
    public void testPollFirstEntry(Blackhole bh) {
        bh.consume(map.pollFirstEntry());
    }

    @Benchmark
    public void testPollLastEntry(Blackhole bh) {
        bh.consume(map.pollLastEntry());
    }

    @Benchmark
    public void testLowerKey(Blackhole bh) {
        bh.consume(map.lowerKey(getNextKey()));
    }

    @Benchmark
    public void testLowerEntry(Blackhole bh) {
        bh.consume(map.lowerEntry(getNextKey()));
    }

    @Benchmark
    public void testFloorKey(Blackhole bh) {
        bh.consume(map.floorKey(getNextKey()));
    }

    @Benchmark
    public void testFloorEntry(Blackhole bh) {
        bh.consume(map.floorEntry(getNextKey()));
    }

    @Benchmark
    public void testCeilingKey(Blackhole bh) {
        bh.consume(map.ceilingKey(getNextKey()));
    }

    @Benchmark
    public void testCeilingEntry(Blackhole bh) {
        bh.consume(map.ceilingEntry(getNextKey()));
    }

    @Benchmark
    public void testHigherKey(Blackhole bh) {
        bh.consume(map.higherKey(getNextKey()));
    }

    @Benchmark
    public void testHigherEntry(Blackhole bh) {
        bh.consume(map.higherEntry(getNextKey()));
    }

    @Benchmark
    public void testKeySet(Blackhole bh) {
        bh.consume(map.keySet());
    }

    @Benchmark
    public void testValues(Blackhole bh) {
        bh.consume(map.values());
    }

    @Benchmark
    public void testEntrySet(Blackhole bh) {
        bh.consume(map.entrySet());
    }

    @Benchmark
    public void testNavigableKeySet(Blackhole bh) {
        bh.consume(map.navigableKeySet());
    }

    @Benchmark
    public void testDescendingKeySet(Blackhole bh) {
        bh.consume(map.descendingKeySet());
    }

    @Benchmark
    public void testEntrySetIterator(Blackhole bh) {
        Iterator<Map.Entry<Integer, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testValuesIterator(Blackhole bh) {
        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testNavigableKeySetIterator(Blackhole bh) {
        Iterator<Integer> it = map.navigableKeySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testDescendingKeySetIterator(Blackhole bh) {
        Iterator<Integer> it = map.descendingKeySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testDescendingMapEntrySetIterator(Blackhole bh) {
        Iterator<Map.Entry<Integer, Integer>> it = map.descendingMap().entrySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testSubMapTwoArgs(Blackhole bh) {
        bh.consume(map.subMap(minKey, midKey));
    }

    @Benchmark
    public void testSubMapFourArgs(Blackhole bh) {
        bh.consume(map.subMap(minKey, true, midKey, false));
    }

    @Benchmark
    public void testHeadMapOneArg(Blackhole bh) {
        bh.consume(map.headMap(midKey));
    }

    @Benchmark
    public void testHeadMapTwoArgs(Blackhole bh) {
        bh.consume(map.headMap(midKey, true));
    }

    @Benchmark
    public void testTailMapOneArg(Blackhole bh) {
        bh.consume(map.tailMap(midKey));
    }

    @Benchmark
    public void testTailMapTwoArgs(Blackhole bh) {
        bh.consume(map.tailMap(midKey, true));
    }

    @Benchmark
    public void testSubMapIterator(Blackhole bh) {
        Iterator<Map.Entry<Integer, Integer>> it = map.subMap(minKey, true, midKey, false).entrySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testHeadMapIterator(Blackhole bh) {
        Iterator<Map.Entry<Integer, Integer>> it = map.headMap(midKey).entrySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testTailMapIterator(Blackhole bh) {
        Iterator<Map.Entry<Integer, Integer>> it = map.tailMap(midKey).entrySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testSubMapKeySetIterator(Blackhole bh) {
        Iterator<Integer> it = map.subMap(minKey, true, midKey, false).keySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testSubMapValuesIterator(Blackhole bh) {
        Iterator<Integer> it = map.subMap(minKey, true, midKey, false).values().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testDescendingMap(Blackhole bh) {
        bh.consume(map.descendingMap());
    }

    @Benchmark
    public void testKeySetIterator(Blackhole bh) {
        Iterator<Integer> it = map.keySet().iterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }

    @Benchmark
    public void testNavigableKeySetDescendingIterator(Blackhole bh) {
        Iterator<Integer> it = map.navigableKeySet().descendingIterator();
        while (it.hasNext()) {
            bh.consume(it.next());
        }
    }




    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CustomTreeMapPerformanceTest.class.getSimpleName())
                .measurementIterations(3)
                .warmupIterations(2)
                .forks(1)
                .result("CustomTreeMap_performance_results.csv")
                .resultFormat(ResultFormatType.CSV)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        writeCustomCsv(results);
    }

    private static void writeCustomCsv(Collection<RunResult> results) {
        try (FileWriter writer = new FileWriter("CustomTreeMap_jmh_performance.csv")) {
            writer.write("Benchmark;Size;Score (ns/op)\n");
            for (RunResult result : results) {
                String benchmarkName = result.getParams().getBenchmark();
                String shortName = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);

                double score = result.getPrimaryResult().getScore();
                String sizeVal = result.getParams().getParam("size");

                writer.write("\"" + shortName + "\";" + (sizeVal != null ? sizeVal : "N/A") + ";" + score + "\n");
            }
            System.out.println("JMH Performance report saved: CustomTreeMap_jmh_performance.csv");
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }
}