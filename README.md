# Custom Linked HashMap

An implementation of a Java TreeMap using a Red-Black Tree.

All methods implemented are identical to those found in the Java NavigableMap interface.

# Build and Test

To build and test the project run command `./gradlew clean build`

To test the project run command `./gradlew test`

### Time Complexity

| Operation / Method                 |  Custom Time  |   JDK Time    | Winner  |
|:-----------------------------------|:-------------:|:-------------:|:-------:|
| **CustomTreeMap()**                |    $O(1)$     |    $O(1)$     |   Tie   |
| **CustomTreeMap(Comparator)**      |    $O(1)$     |    $O(1)$     |   Tie   |
| **CustomTreeMap(Map)**             | $O(m \log n)$ | $O(m \log n)$ |   Tie   |
| **CustomTreeMap(SortedMap)**       | $O(m \log n)$ |    $O(m)$     | **JDK** |
| **ceilingEntry(K)**                |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **ceilingKey(K)**                  |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **clear()**                        |    $O(1)$     |    $O(1)$     |   Tie   |
| **comparator()**                   |    $O(1)$     |    $O(1)$     |   Tie   |
| **containsKey(Object)**            |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **descendingKeySet()**             |    $O(1)$     |    $O(1)$     |   Tie   |
| **descendingMap()**                |    $O(1)$     |    $O(1)$     |   Tie   |
| **entrySet()**                     |    $O(1)$     |    $O(1)$     |   Tie   |
| **firstEntry()**                   |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **firstKey()**                     |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **floorEntry(K)**                  |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **floorKey(K)**                    |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **get(Object)**                    |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **headMap(K)**                     |    $O(1)$     |    $O(1)$     |   Tie   |
| **headMap(K, boolean)**            |    $O(1)$     |    $O(1)$     |   Tie   |
| **higherEntry(K)**                 |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **higherKey(K)**                   |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **lastEntry()**                    |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **lastKey()**                      |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **lowerEntry(K)**                  |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **lowerKey(K)**                    |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **navigableKeySet()**              |    $O(1)$     |    $O(1)$     |   Tie   |
| **pollFirstEntry()**               |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **pollLastEntry()**                |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **put(K, V)**                      |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **remove(Object)**                 |  $O(\log n)$  |  $O(\log n)$  |   Tie   |
| **size()**                         |    $O(1)$     |    $O(1)$     |   Tie   |
| **subMap(K, K)**                   |    $O(1)$     |    $O(1)$     |   Tie   |
| **subMap(K, boolean, K, boolean)** |    $O(1)$     |    $O(1)$     |   Tie   |
| **tailMap(K)**                     |    $O(1)$     |    $O(1)$     |   Tie   |
| **tailMap(K, boolean)**            |    $O(1)$     |    $O(1)$     |   Tie   |

### Space Complexity (Auxiliary)

| Operation / Method                 | Custom Space | JDK Space | Winner |
|:-----------------------------------|:------------:|:---------:|:------:|
| **CustomTreeMap()**                |    $O(1)$    |  $O(1)$   |  Tie   |
| **CustomTreeMap(Comparator)**      |    $O(1)$    |  $O(1)$   |  Tie   |
| **CustomTreeMap(Map)**             |    $O(1)$    |  $O(1)$   |  Tie   |
| **CustomTreeMap(SortedMap)**       |    $O(1)$    |  $O(1)$   |  Tie   |
| **ceilingEntry(K)**                |    $O(1)$    |  $O(1)$   |  Tie   |
| **ceilingKey(K)**                  |    $O(1)$    |  $O(1)$   |  Tie   |
| **clear()**                        |    $O(1)$    |  $O(1)$   |  Tie   |
| **comparator()**                   |    $O(1)$    |  $O(1)$   |  Tie   |
| **containsKey(Object)**            |    $O(1)$    |  $O(1)$   |  Tie   |
| **descendingKeySet()**             |    $O(1)$    |  $O(1)$   |  Tie   |
| **descendingMap()**                |    $O(1)$    |  $O(1)$   |  Tie   |
| **entrySet()**                     |    $O(1)$    |  $O(1)$   |  Tie   |
| **firstEntry()**                   |    $O(1)$    |  $O(1)$   |  Tie   |
| **firstKey()**                     |    $O(1)$    |  $O(1)$   |  Tie   |
| **floorEntry(K)**                  |    $O(1)$    |  $O(1)$   |  Tie   |
| **floorKey(K)**                    |    $O(1)$    |  $O(1)$   |  Tie   |
| **get(Object)**                    |    $O(1)$    |  $O(1)$   |  Tie   |
| **headMap(K)**                     |    $O(1)$    |  $O(1)$   |  Tie   |
| **headMap(K, boolean)**            |    $O(1)$    |  $O(1)$   |  Tie   |
| **higherEntry(K)**                 |    $O(1)$    |  $O(1)$   |  Tie   |
| **higherKey(K)**                   |    $O(1)$    |  $O(1)$   |  Tie   |
| **lastEntry()**                    |    $O(1)$    |  $O(1)$   |  Tie   |
| **lastKey()**                      |    $O(1)$    |  $O(1)$   |  Tie   |
| **lowerEntry(K)**                  |    $O(1)$    |  $O(1)$   |  Tie   |
| **lowerKey(K)**                    |    $O(1)$    |  $O(1)$   |  Tie   |
| **navigableKeySet()**              |    $O(1)$    |  $O(1)$   |  Tie   |
| **pollFirstEntry()**               |    $O(1)$    |  $O(1)$   |  Tie   |
| **pollLastEntry()**                |    $O(1)$    |  $O(1)$   |  Tie   |
| **put(K, V)**                      |    $O(1)$    |  $O(1)$   |  Tie   |
| **remove(Object)**                 |    $O(1)$    |  $O(1)$   |  Tie   |
| **size()**                         |    $O(1)$    |  $O(1)$   |  Tie   |
| **subMap(K, K)**                   |    $O(1)$    |  $O(1)$   |  Tie   |
| **subMap(K, boolean, K, boolean)** |    $O(1)$    |  $O(1)$   |  Tie   |
| **tailMap(K)**                     |    $O(1)$    |  $O(1)$   |  Tie   |
| **tailMap(K, boolean)**            |    $O(1)$    |  $O(1)$   |  Tie   |

# CustomTreeMap vs JDK TreeMap: Performance Comparison

Geometric mean (ns/op) across all tested collection sizes (10,000-100,000 elements). Margins under 1.10x are treated as noise-level ties since the source data has no per-run error/variance to test true statistical significance._

| Method                                                                   | Custom (ns) | JDK (ns)    | Margin |            Winner            |
|:-------------------------------------------------------------------------|:------------|:------------|:------:|:----------------------------:|
| `Constructor()`                                                          | 4.9         | 5           | 1.02x  | **Statistically Equivalent** |
| `Constructor(Comparator<? super K> comparator)`                          | 1.5         | 1.5         | 1.03x  | **Statistically Equivalent** |
| `Constructor(Map<? extends K, ? extends V> m)`                           | 6,805,107.7 | 6,749,178.3 | 1.01x  | **Statistically Equivalent** |
| `Constructor(SortedMap<K, ? extends V> m)`                               | 3,148,818.4 | 3,430,996.9 | 1.09x  | **Statistically Equivalent** |
| `ceilingEntry(K key)`                                                    | 218.4       | 211.2       | 1.03x  | **Statistically Equivalent** |
| `ceilingKey(K key)`                                                      | 129.8       | 122.8       | 1.06x  | **Statistically Equivalent** |
| `clear()`                                                                | 0.556       | 0.531       | 1.05x  | **Statistically Equivalent** |
| `comparator()`                                                           | 0.553       | 0.531       | 1.04x  | **Statistically Equivalent** |
| `containsKey(Object key)`                                                | 126.4       | 124.6       | 1.01x  | **Statistically Equivalent** |
| `containsValue(Object value)`                                            | 1,376,460.4 | 1,368,072.3 | 1.01x  | **Statistically Equivalent** |
| `descendingKeySet()`                                                     | 2.7         | 2.6         | 1.02x  | **Statistically Equivalent** |
| `descendingKeySet().iterator()`                                          | 2,803,580.2 | 2,804,631.7 | 1.00x  | **Statistically Equivalent** |
| `descendingMap()`                                                        | 1.5         | 1.5         | 1.00x  | **Statistically Equivalent** |
| `descendingMap().entrySet().iterator()`                                  | 2,803,122.1 | 2,693,871.7 | 1.04x  | **Statistically Equivalent** |
| `entrySet()`                                                             | 1.3         | 1.3         | 1.00x  | **Statistically Equivalent** |
| `entrySet().iterator()`                                                  | 2,665,182   | 2,670,758.9 | 1.00x  | **Statistically Equivalent** |
| `firstEntry()`                                                           | 27          | 26.8        | 1.01x  | **Statistically Equivalent** |
| `firstKey()`                                                             | 9.8         | 9.8         | 1.00x  | **Statistically Equivalent** |
| `floorEntry(K key)`                                                      | 212.5       | 212.2       | 1.00x  | **Statistically Equivalent** |
| `floorKey(K key)`                                                        | 125.1       | 125         | 1.00x  | **Statistically Equivalent** |
| `get(Object key)`                                                        | 125.1       | 124.3       | 1.01x  | **Statistically Equivalent** |
| `headMap(K toKey)`                                                       | 1.8         | 1.8         | 1.00x  | **Statistically Equivalent** |
| `headMap(K toKey).entrySet().iterator()`                                 | 1,401,393.3 | 1,398,458.7 | 1.00x  | **Statistically Equivalent** |
| `headMap(K toKey, boolean inclusive)`                                    | 2           | 2           |  1.0x  | **Statistically Equivalent** |
| `higherEntry(K key)`                                                     | 229.1       | 226.6       | 1.01x  | **Statistically Equivalent** |
| `higherKey(K key)`                                                       | 101.9       | 97.9        | 1.04x  | **Statistically Equivalent** |
| `isEmpty()`                                                              | 0.544       | 0.528       | 1.03x  | **Statistically Equivalent** |
| `keySet()`                                                               | 0.548       | 0.532       | 1.03x  | **Statistically Equivalent** |
| `keySet().iterator()`                                                    | 2,785,238.2 | 2,762,890   | 1.01x  | **Statistically Equivalent** |
| `lastEntry()`                                                            | 28.2        | 33.4        | 1.18x  |          **Custom**          |
| `lastKey()`                                                              | 10.3        | 10.1        | 1.02x  | **Statistically Equivalent** |
| `lowerEntry(K key)`                                                      | 236.2       | 225.1       | 1.05x  | **Statistically Equivalent** |
| `lowerKey(K key)`                                                        | 140.3       | 135.9       | 1.03x  | **Statistically Equivalent** |
| `navigableKeySet()`                                                      | 1.3         | 1.4         | 1.01x  | **Statistically Equivalent** |
| `navigableKeySet().descendingIterator()`                                 | 2,976,257.3 | 2,984,993.5 | 1.00x  | **Statistically Equivalent** |
| `navigableKeySet().iterator()`                                           | 2,847,756.4 | 2,839,952.3 | 1.00x  | **Statistically Equivalent** |
| `pollFirstEntry()`                                                       | 0.556       | 0.546       | 1.02x  | **Statistically Equivalent** |
| `pollLastEntry()`                                                        | 0.538       | 0.623       | 1.16x  |          **Custom**          |
| `put(K key, V value)`                                                    | 129.5       | 132.6       | 1.02x  | **Statistically Equivalent** |
| `putAll(Map<? extends K, ? extends V> m)`                                | 7,323,422.1 | 7,055,239.7 | 1.04x  | **Statistically Equivalent** |
| `remove(Object key)`                                                     | 2.2         | 2.9         | 1.32x  |          **Custom**          |
| `size()`                                                                 | 0.555       | 0.554       | 1.00x  | **Statistically Equivalent** |
| `subMap(K fromKey, K toKey)`                                             | 2           | 2.1         | 1.02x  | **Statistically Equivalent** |
| `subMap(K fromKey, K toKey).entrySet().iterator()`                       | 1,462,128.2 | 1,521,866.6 | 1.04x  | **Statistically Equivalent** |
| `subMap(K fromKey, K toKey).keySet().iterator()`                         | 1,485,870.0 | 1,554,680.6 | 1.05x  | **Statistically Equivalent** |
| `subMap(K fromKey, K toKey).values().iterator()`                         | 1,502,503.0 | 1,528,133.5 | 1.02x  | **Statistically Equivalent** |
| `subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive)` | 2.1         | 2           | 1.03x  | **Statistically Equivalent** |
| `tailMap(K fromKey)`                                                     | 1.7         | 1.8         | 1.06x  | **Statistically Equivalent** |
| `tailMap(K fromKey).entrySet().iterator()`                               | 1,406,087.2 | 1,439,058.8 | 1.02x  | **Statistically Equivalent** |
| `tailMap(K fromKey, boolean inclusive)`                                  | 1.7         | 1.7         | 1.02x  | **Statistically Equivalent** |
| `values()`                                                               | 0.548       | 0.579       | 1.06x  | **Statistically Equivalent** |
| `values().iterator()`                                                    | 2,815,112.4 | 2,871,449.8 | 1.02x  | **Statistically Equivalent** |

# Performance Charts

#### Note: The following performance charts are designed to be viewed in dark mode.

![GeometricPerformance](PerformanceCharts/geometric.png)
![Heatmap](PerformanceCharts/heatmap.png)
![Chart](PerformanceCharts/TreeMap.png)
![Chart](PerformanceCharts/TreeMap_Comparator.png)
![Chart](PerformanceCharts/TreeMap_Map.png)
![Chart](PerformanceCharts/ceilingEntry_K.png)
![Chart](PerformanceCharts/ceilingKey_K.png)
![Chart](PerformanceCharts/clear.png)
![Chart](PerformanceCharts/comparator.png)
![Chart](PerformanceCharts/containsKey_Object.png)
![Chart](PerformanceCharts/containsValue_Object.png)
![Chart](PerformanceCharts/descendingKeySet.png)
![Chart](PerformanceCharts/descendingKeySet_iterator.png)
![Chart](PerformanceCharts/descendingMap.png)
![Chart](PerformanceCharts/descendingMap_entrySet_iterator.png)
![Chart](PerformanceCharts/entrySet.png)
![Chart](PerformanceCharts/entrySet_iterator.png)
![Chart](PerformanceCharts/firstEntry.png)
![Chart](PerformanceCharts/firstKey.png)
![Chart](PerformanceCharts/floorEntry_K.png)
![Chart](PerformanceCharts/floorKey_K.png)
![Chart](PerformanceCharts/get_Object.png)
![Chart](PerformanceCharts/headMap_K.png)
![Chart](PerformanceCharts/headMap_K_boolean.png)
![Chart](PerformanceCharts/headMap_K_iterator.png)
![Chart](PerformanceCharts/higherEntry_K.png)
![Chart](PerformanceCharts/higherKey_K.png)
![Chart](PerformanceCharts/isEmpty.png)
![Chart](PerformanceCharts/keySet.png)
![Chart](PerformanceCharts/keySet_iterator.png)
![Chart](PerformanceCharts/lastEntry.png)
![Chart](PerformanceCharts/lastKey.png)
![Chart](PerformanceCharts/lowerEntry_K.png)
![Chart](PerformanceCharts/lowerKey_K.png)
![Chart](PerformanceCharts/navigableKeySet.png)
![Chart](PerformanceCharts/navigableKeySet_descendingIterator.png)
![Chart](PerformanceCharts/navigableKeySet_iterator.png)
![Chart](PerformanceCharts/pollFirstEntry.png)
![Chart](PerformanceCharts/pollLastEntry.png)
![Chart](PerformanceCharts/put_K_V.png)
![Chart](PerformanceCharts/putAll_Map.png)
![Chart](PerformanceCharts/remove_Object.png)
![Chart](PerformanceCharts/size.png)
![Chart](PerformanceCharts/subMap_K_K.png)
![Chart](PerformanceCharts/subMap_K_K_entrySet_iterator.png)
![Chart](PerformanceCharts/subMap_K_K_keySet_iterator.png)
![Chart](PerformanceCharts/subMap_K_K_values_iterator.png)
![Chart](PerformanceCharts/subMap_K_boolean_K_boolean.png)
![Chart](PerformanceCharts/tailMap_K.png)
![Chart](PerformanceCharts/tailMap_K_boolean.png)
![Chart](PerformanceCharts/tailMap_K_iterator.png)
![Chart](PerformanceCharts/values.png)
![Chart](PerformanceCharts/values_iterator.png)
