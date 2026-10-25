package customtreemap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import static java.util.Objects.requireNonNull;

/**
 * A custom implementation of {@link NavigableMap} backed by a self-balancing
 * red-black binary search tree, providing behavior comparable to
 * {@link java.util.TreeMap}.
 *
 * <p>Keys are ordered according to their {@linkplain Comparable natural ordering},
 * or by a {@link Comparator} supplied at map creation time, depending on which
 * constructor is used. This ordering is used for all key-based navigation
 * operations, such as {@link #firstKey()}, {@link #lastKey()},
 * {@link #ceilingKey(Comparable)}, {@link #floorKey(Comparable)}, and range views
 * such as {@link #subMap(Comparable, boolean, Comparable, boolean)}.
 *
 * <p>The tree maintains the standard red-black invariants (root is black, red
 * nodes have only black children, and every path from a node to its descendant
 * leaves contains the same number of black nodes) so that {@code get}, {@code put},
 * and {@code remove} all run in {@code O(log n)} time.
 *
 * <p><b>Implementation notes.</b> Internal navigation is performed entirely in
 * terms of {@link Node} references rather than keys, so a lookup such as
 * {@link #ceilingEntry(Comparable)} costs a single tree descent rather than two.
 * Iterators walk the tree with {@link #successor(Node)}/{@link #predecessor(Node)}
 * and hand back the live {@link Node}, which both avoids an allocation per
 * element and makes {@link Map.Entry#setValue(Object)} write through to the map,
 * as the {@link Map} contract requires. Range views bound their iterators with a
 * precomputed <i>fence</i> and therefore perform no key comparisons at all while
 * iterating.
 *
 * <p>This implementation is not synchronized. If multiple threads access a map
 * concurrently, and at least one of the threads modifies the map structurally,
 * it must be synchronized externally.
 *
 * @param <K> the type of keys maintained by this map; must be {@link Comparable}
 *            unless a {@link Comparator} is supplied
 * @param <V> the type of mapped values
 * @see <a href="https://www.linkedin.com/in/benjamin-kane-81149482/">LinkedIn</a>
 * @see <a href="https://github.com/bk10aao">GitHub account bk10aao</a>
 * @see <a href="https://github.com/bk10aao/CustomTreeMap">Repository</a>
 */
public class CustomTreeMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {

    /**
     * Marker used for the color of a red node.
     */
    private static final boolean RED = true;

    /**
     * Marker used for the color of a black node. Also, the color of a {@code null} (absent) node.
     */
    private static final boolean BLACK = false;

    /**
     * Sentinel used by range-view iterators to represent "no fence", i.e. an
     * unbounded end. It is compared by reference identity only, and can never
     * be equal to a real key.
     */
    private static final Object UNBOUNDED = new Object();

    /**
     * The root node of the tree, or {@code null} if the map is empty.
     */
    private Node<K, V> root;

    /**
     * The number of key-value mappings currently in the map.
     */
    private int size = 0;

    protected transient int modCount = 0;

    /**
     * The comparator used to order keys, or {@code null} if natural ordering is used.
     */
    private final Comparator<? super K> comparator;

    /**
     * Lazily created, cached view returned by {@link #entrySet()}. Views are
     * stateless with respect to the map's contents, so a single instance can be
     * reused for the lifetime of the map.
     */
    private transient Set<Entry<K, V>> entrySetView;

    /**
     * Lazily created, cached view returned by {@link #navigableKeySet()} and {@link #keySet()}.
     */
    private transient KeySet<K> navigableKeySetView;

    /**
     * Lazily created, cached view returned by {@link #descendingMap()}.
     */
    private transient NavigableMap<K, V> descendingMapView;

    /**
     * Constructs a new, empty tree map that orders its keys according to their
     * {@linkplain Comparable natural ordering}.
     */
    public CustomTreeMap() {
        this.comparator = null;
    }

    /**
     * Constructs a new, empty tree map that orders its keys according to the
     * given comparator.
     *
     * @param comparator the comparator used to order keys, or {@code null} to
     *                    use the natural ordering of the keys
     */
    public CustomTreeMap(final Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a new tree map containing the same mappings as the given map,
     * ordered according to the natural ordering of its keys.
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws NullPointerException if {@code m} is {@code null}, or if any key
     *                               in {@code m} is {@code null}
     * @throws ClassCastException   if the keys in {@code m} are not mutually comparable
     */
    public CustomTreeMap(final Map<? extends K, ? extends V> m) {
        this.comparator = null;
        putAll(m);
    }

    /**
     * Constructs a new tree map containing the same mappings and using the same
     * ordering as the given sorted map. Because the source is already sorted,
     * the tree is built bottom-up in {@code O(n)} time with no key comparisons,
     * rather than by {@code n} separate {@code O(log n)} insertions.
     *
     * @param m the sorted map whose mappings and comparator are to be used
     * @throws NullPointerException if {@code m} is {@code null}
     */
    public CustomTreeMap(final SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        int mapSize = m.size();
        if (mapSize != 0) {
            modCount++;
            buildFromSorted(mapSize, m.entrySet().iterator());
        }
    }

    /**
     * Returns the entry associated with the least key greater than or equal to
     * the given key, or {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the entry with the least key greater than or equal to {@code key},
     *         or {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Entry<K, V> ceilingEntry(final K key) {
        requireNonNull(key);
        return exportEntry(getCeilingNode(key, true));
    }

    /**
     * Returns the least key greater than or equal to the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the least key greater than or equal to {@code key}, or
     *         {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public K ceilingKey(final K key) {
        requireNonNull(key);
        return keyOrNull(getCeilingNode(key, true));
    }

    /**
     * Removes all mappings from this map. The map will be empty after this
     * call returns.
     */
    public void clear() {
        if (size > 0)
            modCount++;
        size = 0;
        root = null;
    }

    /**
     * Returns the comparator used to order the keys in this map, or
     * {@code null} if this map uses the {@linkplain Comparable natural
     * ordering} of its keys.
     *
     * @return the comparator used to order this map's keys, or {@code null}
     *         if natural ordering is used
     */
    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the given key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws ClassCastException   if {@code key} cannot be compared with the keys in this map
     */
    public boolean containsKey(final Object key) {
        requireNonNull(key);
        return getNode(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the given
     * value. Runs in {@code O(n)} time via a single in-order traversal,
     * allocating nothing.
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if some key maps to {@code value}
     */
    @Override
    public boolean containsValue(final Object value) {
        for (Node<K, V> n = getFirstNode(); n != null; n = successor(n))
            if (valueEquals(value, n.value))
                return true;
        return false;
    }

    /**
     * Returns a reverse-order {@link NavigableSet} view of the keys in this map.
     * The set is backed by the map, so changes to the map are reflected in the set.
     *
     * @return a reverse-order navigable set view of the keys in this map
     */
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * Returns a reverse-order {@link NavigableMap} view of this map. The
     * returned map is backed by this map, so changes to this map are
     * reflected in the returned map, and vice versa. The view is created once
     * and cached.
     *
     * @return a reverse-order view of this map
     */
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> view = descendingMapView;
        return view != null ? view
                : (descendingMapView = new DescendingSubMap<>(this, true, null,
                true, true, null, true));
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map, ordered
     * by key in ascending order. The set is backed by the map, so changes to
     * the map are reflected in the set, and vice versa. The view is created
     * once and cached.
     *
     * <p>The entries handed out by this set's iterator are the map's live
     * nodes, so {@link Map.Entry#setValue(Object)} writes through to the map.
     * An entry must not be used after the iterator has advanced past it.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> view = entrySetView;
        return view != null ? view : (entrySetView = new EntrySet());
    }

    /**
     * Returns the entry associated with the least key in this map, or
     * {@code null} if the map is empty.
     *
     * @return the entry with the least key, or {@code null} if this map is empty
     */
    public Entry<K, V> firstEntry() {
        return exportEntry(getFirstNode());
    }

    /**
     * Returns the least key currently in this map.
     *
     * @return the least key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    public K firstKey() {
        return key(getFirstNode());
    }

    /**
     * Returns the entry associated with the greatest key less than or equal to
     * the given key, or {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the entry with the greatest key less than or equal to {@code key},
     *         or {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Entry<K, V> floorEntry(final K key) {
        requireNonNull(key);
        return exportEntry(getFloorNode(key, true));
    }

    /**
     * Returns the greatest key less than or equal to the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the greatest key less than or equal to {@code key}, or
     *         {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public K floorKey(final K key) {
        requireNonNull(key);
        return keyOrNull(getFloorNode(key, true));
    }

    /**
     * Returns the value to which the given key is mapped, or {@code null} if
     * this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value mapped to {@code key}, or {@code null} if no such mapping exists
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws ClassCastException   if {@code key} cannot be compared with the keys in this map
     */
    public V get(final Object key) {
        requireNonNull(key);
        Node<K, V> p = getNode(key);
        return p == null ? null : p.value;
    }

    /**
     * Returns a view of the portion of this map whose keys are strictly less
     * than {@code toKey}.
     *
     * @param toKey the exclusive upper bound of the returned map
     * @return a view of the portion of this map whose keys are less than {@code toKey}
     * @throws NullPointerException if {@code toKey} is {@code null}
     */
    public SortedMap<K, V> headMap(final K toKey) {
        return headMap(toKey, false);
    }

    /**
     * Returns a view of the portion of this map whose keys are less than
     * (or equal to, if {@code inclusive} is {@code true}) {@code toKey}. The
     * returned map is backed by this map.
     *
     * @param toKey     the upper bound of the returned map
     * @param inclusive {@code true} if {@code toKey} is to be included in the returned view
     * @return a view of the portion of this map whose keys are less than
     *         (or equal to) {@code toKey}
     * @throws NullPointerException if {@code toKey} is {@code null}
     */
    public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        requireNonNull(toKey);
        return new AscendingSubMap<>(this, true, null, true, false, toKey, inclusive);
    }

    /**
     * Returns the entry associated with the least key strictly greater than
     * the given key, or {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the entry with the least key strictly greater than {@code key},
     *         or {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Entry<K, V> higherEntry(final K key) {
        requireNonNull(key);
        return exportEntry(getCeilingNode(key, false));
    }

    /**
     * Returns the least key strictly greater than the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the least key strictly greater than {@code key}, or
     *         {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public K higherKey(final K key) {
        requireNonNull(key);
        return keyOrNull(getCeilingNode(key, false));
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map is empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map, in
     * ascending order. Equivalent to {@link #navigableKeySet()}.
     *
     * @return a set view of the keys in this map
     */
    @Override
    public Set<K> keySet() {
        return navigableKeySet();
    }

    /**
     * Returns the entry associated with the greatest key in this map, or
     * {@code null} if the map is empty.
     *
     * @return the entry with the greatest key, or {@code null} if this map is empty
     */
    public Entry<K, V> lastEntry() {
        return exportEntry(getLastNode());
    }

    /**
     * Returns the greatest key currently in this map.
     *
     * @return the greatest key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    public K lastKey() {
        return key(getLastNode());
    }

    /**
     * Returns the entry associated with the greatest key strictly less than
     * the given key, or {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the entry with the greatest key strictly less than {@code key},
     *         or {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Entry<K, V> lowerEntry(final K key) {
        requireNonNull(key);
        return exportEntry(getFloorNode(key, false));
    }

    /**
     * Returns the greatest key strictly less than the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key to match
     * @return the greatest key strictly less than {@code key}, or
     *         {@code null} if no such key exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public K lowerKey(final K key) {
        requireNonNull(key);
        return keyOrNull(getFloorNode(key, false));
    }

    /**
     * Returns a {@link NavigableSet} view of the keys contained in this map,
     * in ascending order. The set is backed by the map. The view is created
     * once and cached.
     *
     * @return a navigable set view of the keys in this map
     */
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> view = navigableKeySetView;
        return view != null ? view : (navigableKeySetView = new KeySet<>(this));
    }

    /**
     * Removes and returns the entry associated with the least key in this
     * map, or returns {@code null} if the map is empty.
     *
     * @return the removed first entry, or {@code null} if this map is empty
     */
    public Entry<K, V> pollFirstEntry() {
        Node<K, V> p = getFirstNode();
        Entry<K, V> e = exportEntry(p);
        if (p != null)
            deleteNode(p);
        return e;
    }

    /**
     * Removes and returns the entry associated with the greatest key in this
     * map, or returns {@code null} if the map is empty.
     *
     * @return the removed last entry, or {@code null} if this map is empty
     */
    public Entry<K, V> pollLastEntry() {
        Node<K, V> p = getLastNode();
        Entry<K, V> e = exportEntry(p);
        if (p != null)
            deleteNode(p);
        return e;
    }

    /**
     * Associates the given value with the given key in this map. If the map
     * previously contained a mapping for the key, the old value is replaced.
     *
     * <p>If the key is new, a new red node is inserted at the appropriate leaf
     * position via standard binary-search-tree insertion, and
     * {@link #fixAfterInsertion(Node)} is called to restore the red-black
     * invariants.
     *
     * @param key   the key with which the given value is to be associated
     * @param value the value to be associated with the given key
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping for {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws ClassCastException   if {@code key} cannot be compared with the keys already in this map
     */
    public V put(final K key, final V value) {
        requireNonNull(key);
        Node<K, V> t = root;
        if (t == null) {
            root = new Node<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        Node<K, V> parent;
        int cmp;
        final Comparator<? super K> cpr = comparator;
        if (cpr != null)
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        else
            do {
                parent = t;
                cmp = key.compareTo(t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        Node<K, V> e = new Node<>(key, value, parent);
        e.color = RED;
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * Copies all mappings from the given map into this map.
     *
     * <p>When this map is empty and {@code map} is a {@link SortedMap} using an
     * equivalent ordering, the tree is built bottom-up from the already-sorted
     * input in {@code O(n)} time with no key comparisons at all, instead of
     * performing {@code n} separate {@code O(log n)} insertions.
     *
     * @param map the mappings to be stored in this map
     * @throws NullPointerException if {@code map} is {@code null}
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        if(map.isEmpty())
            return;
        int mapSize = map.size();
        if (size == 0 && mapSize != 0 && map instanceof SortedMap
                && Objects.equals(comparator, ((SortedMap<?, ?>) map).comparator())) {
            modCount++;
            buildFromSorted(mapSize, map.entrySet().iterator());
            return;
        }
        super.putAll(map);
    }

    /**
     * Removes the mapping for the given key from this map, if present.
     *
     * @param key the key whose mapping is to be removed from this map
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping for {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws ClassCastException   if {@code key} cannot be compared with the keys in this map
     */
    public V remove(final Object key) {
        requireNonNull(key);
        Node<K, V> p = getNode(key);
        if (p == null)
            return null;
        V oldValue = p.value;
        deleteNode(p);
        return oldValue;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns a view of the portion of this map whose keys range from
     * {@code fromKey}, inclusive, to {@code toKey}, exclusive.
     *
     * @param fromKey the inclusive lower bound of the returned map
     * @param toKey   the exclusive upper bound of the returned map
     * @return a view of the specified range of this map
     * @throws NullPointerException     if {@code fromKey} or {@code toKey} is {@code null}
     * @throws IllegalArgumentException if {@code fromKey} is greater than {@code toKey}
     */
    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * Returns a view of the portion of this map whose keys range from
     * {@code fromKey} to {@code toKey}, with inclusivity of each bound
     * controlled by the corresponding boolean argument. The returned map is
     * backed by this map.
     *
     * @param fromKey       the lower bound of the returned map
     * @param fromInclusive {@code true} if {@code fromKey} is to be included in the returned view
     * @param toKey         the upper bound of the returned map
     * @param toInclusive   {@code true} if {@code toKey} is to be included in the returned view
     * @return a view of the specified range of this map
     * @throws NullPointerException     if {@code fromKey} or {@code toKey} is {@code null}
     * @throws IllegalArgumentException if {@code fromKey} is greater than {@code toKey}
     *                                   (or equal to it while either bound is exclusive)
     */
    public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        requireNonNull(fromKey);
        requireNonNull(toKey);
        int cmp = compare(fromKey, toKey);
        if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
            throw new IllegalArgumentException();
        return new AscendingSubMap<>(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than
     * or equal to {@code fromKey}.
     *
     * @param fromKey the inclusive lower bound of the returned map
     * @return a view of the portion of this map whose keys are greater than
     *         or equal to {@code fromKey}
     * @throws NullPointerException if {@code fromKey} is {@code null}
     */
    public SortedMap<K, V> tailMap(final K fromKey) {
        return tailMap(fromKey, true);
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than
     * (or equal to, if {@code inclusive} is {@code true}) {@code fromKey}.
     * The returned map is backed by this map.
     *
     * @param fromKey   the lower bound of the returned map
     * @param inclusive {@code true} if {@code fromKey} is to be included in the returned view
     * @return a view of the portion of this map whose keys are greater than
     *         (or equal to) {@code fromKey}
     * @throws NullPointerException if {@code fromKey} is {@code null}
     */
    public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        requireNonNull(fromKey);
        return new AscendingSubMap<>(this, false, fromKey, inclusive, true, null, true);
    }

    /**
     * Returns the color of the given node, treating a {@code null} node
     * (an external/leaf position) as {@link #BLACK}, per red-black tree convention.
     *
     * @param p the node whose color is queried, may be {@code null}
     * @return {@link #RED} or {@link #BLACK}
     */
    private static <K, V> boolean colorOf(final Node<K, V> p) {
        return p == null ? BLACK : p.color;
    }

    /**
     * Compares two keys using this map's comparator, or their natural
     * ordering if no comparator was supplied.
     *
     * <p>Hot descent loops ({@link #put}, {@link #getNode}) do not call this
     * method; they hoist the comparator test out of the loop instead.
     *
     * @param k1 the first key
     * @param k2 the second key
     * @return a negative integer, zero, or a positive integer as {@code k1} is
     *         less than, equal to, or greater than {@code k2}
     */
    private int compare(final Object k1, final Object k2) {
        if (comparator != null)
            return comparator.compare((K) k1, (K) k2);
        return ((Comparable<? super K>) k1).compareTo((K) k2);
    }

    /**
     * Compares two values for equality, treating {@code null} as equal only to
     * {@code null}. Used by {@link #containsValue(Object)} and by the entry-set
     * views.
     *
     * @param a the first value, may be {@code null}
     * @param b the second value, may be {@code null}
     * @return {@code true} if the two values are equal
     */
    private static boolean valueEquals(final Object a, final Object b) {
        return Objects.equals(a, b);
    }

    /**
     * Wraps the given node in an immutable, disconnected {@link Map.Entry}
     * suitable for returning to callers of the navigation methods, so that
     * external code cannot mutate the tree's internal {@link Node} objects.
     *
     * <p>This takes a node rather than a key: the caller has already located
     * the node, so no second tree descent is needed.
     *
     * @param node the node to export, may be {@code null}
     * @return an immutable entry for {@code node}, or {@code null} if {@code node} is {@code null}
     */
    private static <K, V> Map.Entry<K, V> exportEntry(final Node<K, V> node) {
        return node == null ? null : new SimpleImmutableEntry<>(node.key, node.value);
    }

    /**
     * Returns the key of the given node, or {@code null} if the node is {@code null}.
     *
     * @param node the node whose key is wanted, may be {@code null}
     * @return the node's key, or {@code null}
     */
    private static <K, V> K keyOrNull(final Node<K, V> node) {
        return node == null ? null : node.key;
    }

    /**
     * Returns the key of the given node, failing if the node is absent.
     *
     * @param node the node whose key is wanted
     * @return the node's key
     * @throws NoSuchElementException if {@code node} is {@code null}
     */
    static <K, V> K key(final Node<K, V> node) {
        if (node == null)
            throw new NoSuchElementException();
        return node.key;
    }

    /**
     * Builds a balanced red-black tree from an already-sorted sequence of
     * mappings in {@code O(n)} time, performing no key comparisons.
     *
     * @param size the number of mappings to read from {@code it}
     * @param it   an iterator over the mappings, in this map's key order
     */
    private void buildFromSorted(final int size, final Iterator<? extends Map.Entry<? extends K, ? extends V>> it) {
        this.size = size;
        this.root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it);
    }

    /**
     * Recursively builds the subtree spanning positions {@code lo..hi} of the
     * sorted input, colouring the nodes at depth {@code redLevel} red so that
     * the black-height invariant holds for a tree that is not a perfect
     * power-of-two size.
     *
     * @param level    the depth of the subtree root being built
     * @param lo       the first index of this subtree's range
     * @param hi       the last index of this subtree's range
     * @param redLevel the depth at which nodes must be coloured red
     * @param it       the iterator supplying mappings in ascending key order
     * @return the root of the constructed subtree, or {@code null} if the range is empty
     */
    private Node<K, V> buildFromSorted(final int level, final int lo, final int hi, final int redLevel,
                                       final Iterator<? extends Map.Entry<? extends K, ? extends V>> it) {
        if (hi < lo)
            return null;
        int mid = (lo + hi) >>> 1;
        Node<K, V> left = null;
        if (lo < mid)
            left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it);
        Map.Entry<? extends K, ? extends V> entry = it.next();
        Node<K, V> middle = new Node<>(entry.getKey(), entry.getValue(), null);
        if (level == redLevel)
            middle.color = RED;
        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }
        if (mid < hi) {
            Node<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it);
            middle.right = right;
            right.parent = middle;
        }
        return middle;
    }

    /**
     * Returns the depth of the last "full" level of a tree of the given size,
     * which is the level whose nodes must be coloured red by
     * {@link #buildFromSorted}.
     *
     * @param size the number of nodes in the tree
     * @return the level to colour red
     */
    private static int computeRedLevel(final int size) {
        return 31 - Integer.numberOfLeadingZeros(size + 1);
    }

    /**
     * Removes the given node from the tree, rewiring child/parent links and
     * balancing the tree afterwards if a black node was removed.
     *
     * <p>If the node to delete has two children, its position is instead
     * relabeled with its in-order {@link #successor(Node)}'s key/value, and
     * the successor node (which has at most one child) is physically unlinked
     * in its place. Callers holding a reference to that successor (notably
     * iterators) must account for this; see {@code PrivateEntryIterator#remove}.
     *
     * @param p the node to remove; must not be {@code null}
     */
    private void deleteNode(Node<K, V> p) {
        size--;
        modCount++;
        if (p.left != null && p.right != null) {
            Node<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        }
        Node<K, V> replacement = (p.left != null ? p.left : p.right);
        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left = replacement;
            else
                p.parent.right = replacement;
            p.left = p.right = p.parent = null;
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        } else if (p.parent == null)
            root = null;
        else {
            if (p.color == BLACK)
                fixAfterDeletion(p);
            if (p.parent != null) {
                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }

    /**
     * Restores the red-black tree invariants after a node has been physically
     * removed from the tree, in the case where the removed node (or its
     * replacement) was black. {@code x} is the node that now occupies the
     * position of the removed node (possibly a "phantom" double-black node),
     * and rotations/recoloring are applied up the tree until the invariants
     * are satisfied.
     *
     * @param x the node (or replacement) at which to begin balancing
     */
    private void fixAfterDeletion(Node<K, V> x) {
        while (x != root && colorOf(x) == BLACK) {
            Node<K, V> parent = parentOf(x);
            if (x == leftOf(parent)) {
                Node<K, V> sib = rightOf(parent);
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parent, RED);
                    rotateLeft(parent);
                    parent = parentOf(x);
                    sib = rightOf(parent);
                }
                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parent;
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        parent = parentOf(x);
                        sib = rightOf(parent);
                    }
                    setColor(sib, colorOf(parent));
                    setColor(parent, BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parent);
                    x = root;
                }
            } else {
                Node<K, V> sib = leftOf(parent);
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parent, RED);
                    rotateRight(parent);
                    parent = parentOf(x);
                    sib = leftOf(parent);
                }
                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parent;
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        parent = parentOf(x);
                        sib = leftOf(parent);
                    }
                    setColor(sib, colorOf(parent));
                    setColor(parent, BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parent);
                    x = root;
                }
            }
        }
        setColor(x, BLACK);
    }

    /**
     * Restores the red-black tree invariants after a new red node {@code x}
     * has been inserted, by recoloring and rotating up the tree to resolve
     * any red-red parent/child violations, and finally ensuring the root is black.
     *
     * @param x the newly inserted node
     */
    private void fixAfterInsertion(Node<K, V> x) {
        x.color = RED;
        while (x != null && x != root && x.parent.color == RED) {
            Node<K, V> parent = parentOf(x);
            Node<K, V> grand = parentOf(parent);
            if (parent == leftOf(grand)) {
                Node<K, V> y = rightOf(grand);
                if (colorOf(y) == RED) {
                    setColor(parent, BLACK);
                    setColor(y, BLACK);
                    setColor(grand, RED);
                    x = grand;
                } else {
                    if (x == rightOf(parent)) {
                        x = parent;
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<K, V> y = leftOf(grand);
                if (colorOf(y) == RED) {
                    setColor(parent, BLACK);
                    setColor(y, BLACK);
                    setColor(grand, RED);
                    x = grand;
                } else {
                    if (x == leftOf(parent)) {
                        x = parent;
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Finds the node with the least key greater than (or equal to, if
     * {@code inclusive} is {@code true}) the given key, by walking down from
     * the root and remembering the best candidate seen while descending left.
     *
     * @param key       the key to search for
     * @param inclusive whether an exact match should be returned
     * @return the matching node, or {@code null} if none exists
     */
    private Node<K, V> getCeilingNode(final K key, final boolean inclusive) {
        Node<K, V> p = root;
        Node<K, V> candidate = null;
        final Comparator<? super K> cpr = comparator;
        while (p != null) {
            int cmp = cpr != null ? cpr.compare(key, p.key) : key.compareTo(p.key);
            if (cmp < 0) {
                candidate = p;
                p = p.left;
            } else {
                if (cmp == 0 && inclusive)
                    return p;
                p = p.right;
            }
        }
        return candidate;
    }

    /**
     * Returns the node holding the least key in the tree, found by following
     * left-child links from the root.
     *
     * @return the leftmost node in the tree, or {@code null} if the tree is empty
     */
    private Node<K, V> getFirstNode() {
        Node<K, V> p = root;
        if (p != null)
            while (p.left != null)
                p = p.left;
        return p;
    }

    /**
     * Finds the node with the greatest key less than (or equal to, if
     * {@code inclusive} is {@code true}) the given key, by walking down from
     * the root and remembering the best candidate seen while descending right.
     *
     * @param key       the key to search for
     * @param inclusive whether an exact match should be returned
     * @return the matching node, or {@code null} if none exists
     */
    private Node<K, V> getFloorNode(final K key, final boolean inclusive) {
        Node<K, V> p = root;
        Node<K, V> candidate = null;
        final Comparator<? super K> cpr = comparator;
        while (p != null) {
            int cmp = cpr != null ? cpr.compare(key, p.key) : key.compareTo(p.key);
            if (cmp > 0) {
                candidate = p;
                p = p.right;
            } else if (cmp < 0) {
                p = p.left;
            } else {
                if (inclusive)
                    return p;
                p = p.left;
            }
        }
        return candidate;
    }

    /**
     * Returns the node holding the greatest key in the tree, found by
     * following right-child links from the root.
     *
     * @return the rightmost node in the tree, or {@code null} if the tree is empty
     */
    private Node<K, V> getLastNode() {
        Node<K, V> p = root;
        if (p != null)
            while (p.right != null)
                p = p.right;
        return p;
    }

    /**
     * Performs a standard binary-search-tree lookup for the given key. The
     * comparator test is hoisted out of the descent loop.
     *
     * @param key the key to search for, may be {@code null}
     * @return the node whose key equals {@code key}, or {@code null} if
     *         {@code key} is {@code null} or not found
     */
    private Node<K, V> getNode(final Object key) {
        if (key == null)
            return null;
        final Comparator<? super K> cpr = comparator;
        Node<K, V> p = root;
        if (cpr != null) {
            K k = (K) key;
            while (p != null) {
                int cmp = cpr.compare(k, p.key);
                if (cmp < 0)
                    p = p.left;
                else if (cmp > 0)
                    p = p.right;
                else
                    return p;
            }
        } else {
            Comparable<? super K> k = (Comparable<? super K>) key;
            while (p != null) {
                int cmp = k.compareTo(p.key);
                if (cmp < 0)
                    p = p.left;
                else if (cmp > 0)
                    p = p.right;
                else
                    return p;
            }
        }
        return null;
    }

    /**
     * Returns the left child of the given node, treating a {@code null} node
     * as having no children.
     *
     * @param p the node whose left child is queried, may be {@code null}
     * @return {@code p.left}, or {@code null} if {@code p} is {@code null}
     */
    private static <K, V> Node<K, V> leftOf(final Node<K, V> p) {
        return p == null ? null : p.left;
    }

    /**
     * Returns the parent of the given node, treating a {@code null} node as
     * having no parent.
     *
     * @param p the node whose parent is queried, may be {@code null}
     * @return {@code p.parent}, or {@code null} if {@code p} is {@code null}
     */
    private static <K, V> Node<K, V> parentOf(final Node<K, V> p) {
        return p == null ? null : p.parent;
    }

    /**
     * Returns the node whose key immediately precedes the given node's key in
     * this map's ordering (the in-order predecessor).
     *
     * @param t the node whose predecessor is sought, may be {@code null}
     * @return the in-order predecessor of {@code t}, or {@code null} if
     *         {@code t} is {@code null} or has no predecessor
     */
    private static <K, V> Node<K, V> predecessor(final Node<K, V> t) {
        if (t == null)
            return null;
        else if (t.left != null) {
            Node<K, V> p = t.left;
            while (p.right != null)
                p = p.right;
            return p;
        } else {
            Node<K, V> p = t.parent;
            Node<K, V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns the right child of the given node, treating a {@code null} node
     * as having no children.
     *
     * @param p the node whose right child is queried, may be {@code null}
     * @return {@code p.right}, or {@code null} if {@code p} is {@code null}
     */
    private static <K, V> Node<K, V> rightOf(final Node<K, V> p) {
        return p == null ? null : p.right;
    }

    /**
     * Performs a standard left tree rotation around the given node, promoting
     * its right child into its position while preserving in-order key ordering.
     * Does nothing if {@code p} is {@code null}.
     *
     * @param p the node to rotate around
     */
    private void rotateLeft(final Node<K, V> p) {
        if (p != null) {
            Node<K, V> r = p.right;
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    /**
     * Performs a standard right tree rotation around the given node, promoting
     * its left child into its position while preserving in-order key ordering.
     * Does nothing if {@code p} is {@code null}.
     *
     * @param p the node to rotate around
     */
    private void rotateRight(final Node<K, V> p) {
        if (p != null) {
            Node<K, V> l = p.left;
            p.left = l.right;
            if (l.right != null)
                l.right.parent = p;
            l.parent = p.parent;
            if (p.parent == null)
                root = l;
            else if (p.parent.right == p)
                p.parent.right = l;
            else
                p.parent.left = l;
            l.right = p;
            p.parent = l;
        }
    }

    /**
     * Sets the color of the given node, doing nothing if the node is
     * {@code null} (since {@code null} nodes are conventionally black and
     * cannot be recolored).
     *
     * @param p the node to recolor, may be {@code null}
     * @param c the new color, {@link #RED} or {@link #BLACK}
     */
    private static <K, V> void setColor(final Node<K, V> p, final boolean c) {
        if (p != null)
            p.color = c;
    }

    /**
     * Returns the node whose key immediately follows the given node's key in
     * this map's ordering (the in-order successor).
     *
     * @param t the node whose successor is sought, may be {@code null}
     * @return the in-order successor of {@code t}, or {@code null} if
     *         {@code t} is {@code null} or has no successor
     */
    private static <K, V> Node<K, V> successor(final Node<K, V> t) {
        if (t == null)
            return null;
        else if (t.right != null) {
            Node<K, V> p = t.right;
            while (p.left != null)
                p = p.left;
            return p;
        } else {
            Node<K, V> p = t.parent;
            Node<K, V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns an iterator over this map's keys in ascending order, without
     * materialising any entry objects.
     *
     * @return an ascending key iterator
     */
    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstNode());
    }

    /**
     * Returns an iterator over this map's keys in descending order.
     *
     * @return a descending key iterator
     */
    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastNode());
    }

    /**
     * The set view returned by {@link CustomTreeMap#entrySet()}.
     */
    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public void clear() {
            CustomTreeMap.this.clear();
        }

        /**
         * Returns {@code true} only if the given object is an entry that is
         * actually present in this map with an equal value. Runs in
         * {@code O(log n)} rather than scanning the set.
         */
        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> entry))
                return false;
            Object key = entry.getKey();
            if (key == null)
                return false;
            Node<K, V> p = getNode(key);
            return p != null && valueEquals(entry.getValue(), p.value);
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator(getFirstNode());
        }

        /**
         * Removes the given entry if it is present with a matching value.
         * Runs in {@code O(log n)}.
         */
        @Override
        public boolean remove(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> entry))
                return false;
            Object key = entry.getKey();
            if (key == null)
                return false;
            Node<K, V> p = getNode(key);
            if (p == null || !valueEquals(entry.getValue(), p.value))
                return false;
            deleteNode(p);
            return true;
        }

        @Override
        public int size() {
            return CustomTreeMap.this.size;
        }
    }

    /**
     * Base class for this map's iterators. It walks the tree in-order using
     * parent links, holds no auxiliary stack, and allocates nothing per step.
     *
     * @param <T> the type yielded by the iterator
     */
    private abstract class PrivateEntryIterator<T> implements Iterator<T> {

        /**
         * The node that will be returned by the next call to {@code next()}.
         */
        Node<K, V> next;

        /**
         * The node most recently returned, or {@code null} if none is removable.
         */
        Node<K, V> lastReturned;

        /**
         * The map's {@code modCount} as of the last synchronisation point.
         */
        int expectedModCount;

        PrivateEntryIterator(final Node<K, V> first) {
            this.expectedModCount = modCount;
            this.lastReturned = null;
            this.next = first;
        }

        /**
         * {@inheritDoc} A single reference test; no comparator calls.
         */
        @Override
        public final boolean hasNext() {
            return next != null;
        }

        /**
         * Advances forward and returns the node just passed.
         *
         * @return the next node in ascending order
         */
        final Node<K, V> nextNode() {
            Node<K, V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = successor(e);
            lastReturned = e;
            return e;
        }

        /**
         * Advances backward and returns the node just passed.
         *
         * @return the next node in descending order
         */
        final Node<K, V> prevNode() {
            Node<K, V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = predecessor(e);
            lastReturned = e;
            return e;
        }

        /**
         * {@inheritDoc}
         *
         * <p>When the node being removed has two children,
         * {@link CustomTreeMap#deleteNode(Node)} relabels it with its
         * successor's key and value and unlinks the successor node instead.
         * That successor is exactly the node {@link #next} points at, so
         * {@code next} is redirected to the relabeled node to avoid skipping
         * an element or terminating the traversal early.
         */
        @Override
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (lastReturned.left != null && lastReturned.right != null)
                next = lastReturned;
            deleteNode(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    /**
     * Ascending iterator over this map's entries. Yields the live {@link Node},
     * so {@link Map.Entry#setValue(Object)} writes through to the map.
     */
    private final class EntryIterator extends PrivateEntryIterator<Entry<K, V>> {

        EntryIterator(final Node<K, V> first) {
            super(first);
        }

        @Override
        public Entry<K, V> next() {
            return nextNode();
        }
    }

    /**
     * Ascending iterator over this map's keys.
     */
    private final class KeyIterator extends PrivateEntryIterator<K> {

        KeyIterator(final Node<K, V> first) {
            super(first);
        }

        @Override
        public K next() {
            return nextNode().key;
        }
    }

    /**
     * Descending iterator over this map's keys.
     */
    private final class DescendingKeyIterator extends PrivateEntryIterator<K> {

        DescendingKeyIterator(final Node<K, V> first) {
            super(first);
        }

        @Override
        public K next() {
            return prevNode().key;
        }

        /**
         * {@inheritDoc} A descending traversal already holds the relabeled
         * node's predecessor, so no {@code next} fixup is required.
         */
        @Override
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            deleteNode(lastReturned);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    /**
     * Abstract base class for the {@code NavigableMap} range views returned by
     * {@link CustomTreeMap#subMap(Comparable, boolean, Comparable, boolean)},
     * {@link CustomTreeMap#headMap(Comparable, boolean)},
     * {@link CustomTreeMap#tailMap(Comparable, boolean)} and
     * {@link CustomTreeMap#descendingMap()}. All operations are delegated to
     * the backing {@link #m} instance and restricted to this view's range, so
     * the view stays live: modifications made through it, or directly on the
     * backing map, are visible through both.
     *
     * <p><b>Bounds are absolute.</b> {@link #lo} and {@link #hi} are always
     * expressed in the <i>backing map's</i> ordering, regardless of the
     * direction in which this view iterates. A descending view of
     * {@code [2, 6]} therefore stores {@code lo = 2, hi = 6} exactly as the
     * ascending view does, and merely maps its own relative operations
     * ("first", "ceiling", ...) onto the opposite absolute ones. Storing
     * bounds relative to the view's direction requires every derived view to
     * swap them, which is easy to get wrong and yields silently empty views.
     *
     * <p>The {@code abs*} helpers below locate {@link Node}s rather than keys,
     * so a query such as {@code ceilingEntry} costs one descent of the backing
     * tree, not two plus a re-lookup.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static abstract class NavigableSubMap<K extends Comparable<K>, V> extends AbstractMap<K, V>
            implements NavigableMap<K, V> {

        /**
         * The backing map that this view delegates to.
         */
        final CustomTreeMap<K, V> m;

        /**
         * The absolute lower bound of this view, ignored if {@link #fromStart} is {@code true}.
         */
        final K lo;

        /**
         * The absolute upper bound of this view, ignored if {@link #toEnd} is {@code true}.
         */
        final K hi;

        /**
         * Whether this view is unbounded below.
         */
        final boolean fromStart;

        /**
         * Whether this view is unbounded above.
         */
        final boolean toEnd;

        /**
         * Whether {@link #lo} itself is included in this view.
         */
        final boolean loInclusive;

        /**
         * Whether {@link #hi} itself is included in this view.
         */
        final boolean hiInclusive;

        /**
         * Lazily created, cached entry-set view.
         */
        transient EntrySetView entrySetView;

        /**
         * Lazily created, cached navigable key-set view.
         */
        transient KeySet<K> navigableKeySetView;

        /**
         * Lazily created, cached reverse-order view.
         */
        transient NavigableMap<K, V> descendingMapView;

        /**
         * Constructs a range view over {@code map} restricted to the given
         * absolute bounds.
         *
         * @param map         the backing map
         * @param fromStart   {@code true} if this view is unbounded below
         * @param lo          the absolute lower bound, ignored if {@code fromStart}
         * @param loInclusive whether the lower bound is inclusive
         * @param toEnd       {@code true} if this view is unbounded above
         * @param hi          the absolute upper bound, ignored if {@code toEnd}
         * @param hiInclusive whether the upper bound is inclusive
         * @throws IllegalArgumentException if {@code lo} is greater than {@code hi}
         */
        NavigableSubMap(final CustomTreeMap<K, V> map, final boolean fromStart, final K lo, final boolean loInclusive,
                        final boolean toEnd, final K hi, final boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (map.compare(lo, hi) > 0)
                    throw new IllegalArgumentException("fromKey > toKey");
            } else {
                if (!fromStart)
                    map.compare(lo, lo);
                if (!toEnd)
                    map.compare(hi, hi);
            }
            this.m = map;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        /**
         * Returns whether the given key falls below this view's absolute lower bound.
         *
         * @param key the key to test
         * @return {@code true} if {@code key} is below the range
         */
        final boolean tooLow(final K key) {
            if (fromStart)
                return false;
            int c = m.compare(key, lo);
            return c < 0 || (c == 0 && !loInclusive);
        }

        /**
         * Returns whether the given key falls above this view's absolute upper bound.
         *
         * @param key the key to test
         * @return {@code true} if {@code key} is above the range
         */
        final boolean tooHigh(final K key) {
            if (toEnd)
                return false;
            int c = m.compare(key, hi);
            return c > 0 || (c == 0 && !hiInclusive);
        }

        /**
         * Returns whether the given key lies within this view's range.
         *
         * @param key the key to test
         * @return {@code true} if {@code key} is in range
         */
        final boolean inRange(final K key) {
            return !tooLow(key) && !tooHigh(key);
        }

        /**
         * Returns whether the given key lies within this view's range, treating
         * both bounds as inclusive. Used to validate the bounds of derived views,
         * which may legitimately sit on an exclusive endpoint.
         *
         * @param key the key to test
         * @return {@code true} if {@code key} is in the closed range
         */
        final boolean inClosedRange(final K key) {
            return (fromStart || m.compare(key, lo) >= 0) && (toEnd || m.compare(hi, key) >= 0);
        }

        /**
         * Returns whether the given key is acceptable as a bound of a derived
         * view with the given inclusivity.
         *
         * @param key       the key to test
         * @param inclusive whether the derived bound is inclusive
         * @return {@code true} if {@code key} is an acceptable bound
         */
        final boolean inRange(final K key, final boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /**
         * Returns whether the given object is a non-{@code null} key lying
         * within this view's range.
         *
         * @param key the key to test, of any type
         * @return {@code true} if {@code key} is non-null and in range
         */
        final boolean inRangeObject(final Object key) {
            return key != null && inRange((K) key);
        }

        /**
         * Returns the node with the smallest key in this view's range, in the
         * backing map's ordering.
         *
         * @return the absolutely lowest in-range node, or {@code null} if the view is empty
         */
        final Node<K, V> absLowest() {
            Node<K, V> e = fromStart ? m.getFirstNode() : m.getCeilingNode(lo, loInclusive);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        /**
         * Returns the node with the largest key in this view's range, in the
         * backing map's ordering.
         *
         * @return the absolutely highest in-range node, or {@code null} if the view is empty
         */
        final Node<K, V> absHighest() {
            Node<K, V> e = toEnd ? m.getLastNode() : m.getFloorNode(hi, hiInclusive);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /**
         * Returns the in-range node with the least key greater than or equal to {@code key}.
         *
         * @param key the key to match
         * @return the matching node, or {@code null} if none is in range
         */
        final Node<K, V> absCeiling(final K key) {
            if (tooLow(key))
                return absLowest();
            Node<K, V> e = m.getCeilingNode(key, true);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        /**
         * Returns the in-range node with the least key strictly greater than {@code key}.
         *
         * @param key the key to match
         * @return the matching node, or {@code null} if none is in range
         */
        final Node<K, V> absHigher(final K key) {
            if (tooLow(key))
                return absLowest();
            Node<K, V> e = m.getCeilingNode(key, false);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        /**
         * Returns the in-range node with the greatest key less than or equal to {@code key}.
         *
         * @param key the key to match
         * @return the matching node, or {@code null} if none is in range
         */
        final Node<K, V> absFloor(final K key) {
            if (tooHigh(key))
                return absHighest();
            Node<K, V> e = m.getFloorNode(key, true);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /**
         * Returns the in-range node with the greatest key strictly less than {@code key}.
         *
         * @param key the key to match
         * @return the matching node, or {@code null} if none is in range
         */
        final Node<K, V> absLower(final K key) {
            if (tooHigh(key))
                return absHighest();
            Node<K, V> e = m.getFloorNode(key, false);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /**
         * Returns the first node lying strictly above this view's range, used
         * as the stopping fence for an ascending iteration.
         *
         * @return the node just past the upper end of the range, or {@code null} if unbounded
         */
        final Node<K, V> absHighFence() {
            return toEnd ? null : m.getCeilingNode(hi, !hiInclusive);
        }

        /**
         * Returns the last node lying strictly below this view's range, used
         * as the stopping fence for a descending iteration.
         *
         * @return the node just past the lower end of the range, or {@code null} if unbounded
         */
        final Node<K, V> absLowFence() {
            return fromStart ? null : m.getFloorNode(lo, !loInclusive);
        }

        /**
         * Returns this view's first node in its own iteration order.
         *
         * @return the first node of this view, or {@code null} if empty
         */
        abstract Node<K, V> subLowest();

        /**
         * Returns this view's last node in its own iteration order.
         *
         * @return the last node of this view, or {@code null} if empty
         */
        abstract Node<K, V> subHighest();

        /**
         * Returns this view's ceiling node for {@code key}, in its own ordering.
         *
         * @param key the key to match
         * @return the matching node, or {@code null}
         */
        abstract Node<K, V> subCeiling(K key);

        /**
         * Returns this view's higher node for {@code key}, in its own ordering.
         *
         * @param key the key to match
         * @return the matching node, or {@code null}
         */
        abstract Node<K, V> subHigher(K key);

        /**
         * Returns this view's floor node for {@code key}, in its own ordering.
         *
         * @param key the key to match
         * @return the matching node, or {@code null}
         */
        abstract Node<K, V> subFloor(K key);

        /**
         * Returns this view's lower node for {@code key}, in its own ordering.
         *
         * @param key the key to match
         * @return the matching node, or {@code null}
         */
        abstract Node<K, V> subLower(K key);

        /**
         * Returns an iterator over this view's keys in its own order.
         *
         * @return a key iterator
         */
        abstract Iterator<K> keyIterator();

        /**
         * Returns an iterator over this view's keys in the reverse of its own order.
         *
         * @return a reversed key iterator
         */
        abstract Iterator<K> descendingKeyIterator();

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> ceilingEntry(final K key) {
            requireNonNull(key);
            return exportEntry(subCeiling(key));
        }

        /**
         * {@inheritDoc}
         */
        public K ceilingKey(final K key) {
            requireNonNull(key);
            return keyOrNull(subCeiling(key));
        }

        /**
         * {@inheritDoc}
         */
        public void clear() {
            Iterator<Entry<K, V>> it = entrySet().iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsKey(final Object key) {
            return inRangeObject(key) && m.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Entry<K, V>> entrySet() {
            EntrySetView view = entrySetView;
            return view != null ? view : (entrySetView = new EntrySetView());
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> firstEntry() {
            return exportEntry(subLowest());
        }

        /**
         * {@inheritDoc}
         */
        public K firstKey() {
            return key(subLowest());
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> floorEntry(final K key) {
            requireNonNull(key);
            return exportEntry(subFloor(key));
        }

        /**
         * {@inheritDoc}
         */
        public K floorKey(final K key) {
            requireNonNull(key);
            return keyOrNull(subFloor(key));
        }

        /**
         * {@inheritDoc}
         */
        public V get(final Object key) {
            return inRangeObject(key) ? m.get(key) : null;
        }

        /**
         * {@inheritDoc}
         */
        public SortedMap<K, V> headMap(final K toKey) {
            return headMap(toKey, false);
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> higherEntry(final K key) {
            requireNonNull(key);
            return exportEntry(subHigher(key));
        }

        /**
         * {@inheritDoc}
         */
        public K higherKey(final K key) {
            requireNonNull(key);
            return keyOrNull(subHigher(key));
        }

        /**
         * {@inheritDoc} Runs in {@code O(log n)}; it does not count the view's
         * elements.
         */
        @Override
        public boolean isEmpty() {
            return (fromStart && toEnd) ? m.isEmpty() : absLowest() == null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> lastEntry() {
            return exportEntry(subHighest());
        }

        /**
         * {@inheritDoc}
         */
        public K lastKey() {
            return key(subHighest());
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> lowerEntry(final K key) {
            requireNonNull(key);
            return exportEntry(subLower(key));
        }

        /**
         * {@inheritDoc}
         */
        public K lowerKey(final K key) {
            requireNonNull(key);
            return keyOrNull(subLower(key));
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<K> navigableKeySet() {
            KeySet<K> view = navigableKeySetView;
            return view != null ? view : (navigableKeySetView = new KeySet<>(this));
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> pollFirstEntry() {
            Node<K, V> e = subLowest();
            Entry<K, V> result = exportEntry(e);
            if (e != null)
                m.deleteNode(e);
            return result;
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> pollLastEntry() {
            Node<K, V> e = subHighest();
            Entry<K, V> result = exportEntry(e);
            if (e != null)
                m.deleteNode(e);
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if {@code key} lies outside this view's range
         */
        public V put(final K key, final V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return m.put(key, value);
        }

        /**
         * {@inheritDoc}
         */
        public V remove(final Object key) {
            return inRangeObject(key) ? m.remove(key) : null;
        }

        /**
         * {@inheritDoc} Counting is {@code O(n)} in the size of the view, but
         * the result is memorised until the backing map is next modified.
         */
        @Override
        public int size() {
            return (fromStart && toEnd) ? m.size() : entrySet().size();
        }

        /**
         * {@inheritDoc}
         */
        public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        /**
         * {@inheritDoc}
         */
        public SortedMap<K, V> tailMap(final K fromKey) {
            return tailMap(fromKey, true);
        }

        /**
         * The entry-set view of a range view. Its {@code size()} is memorised
         * against the backing map's {@code modCount}, so repeated size queries
         * between mutations cost {@code O(1)} instead of a full range walk.
         */
        final class EntrySetView extends AbstractSet<Entry<K, V>> {

            /**
             * The cached element count, or {@code -1} if not yet computed.
             */
            private transient int cachedSize = -1;

            /**
             * The backing map's {@code modCount} when {@link #cachedSize} was computed.
             */
            private transient int cachedSizeModCount;

            @Override
            public boolean contains(final Object o) {
                if (!(o instanceof Map.Entry<?, ?> entry))
                    return false;
                Object key = entry.getKey();
                if (!inRangeObject(key))
                    return false;
                Node<K, V> node = m.getNode(key);
                return node != null && valueEquals(node.value, entry.getValue());
            }

            @Override
            public boolean isEmpty() {
                return absLowest() == null;
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return entryIterator();
            }

            @Override
            public boolean remove(final Object o) {
                if (!(o instanceof Map.Entry<?, ?> entry))
                    return false;
                Object key = entry.getKey();
                if (!inRangeObject(key))
                    return false;
                Node<K, V> node = m.getNode(key);
                if (node == null || !valueEquals(node.value, entry.getValue()))
                    return false;
                m.deleteNode(node);
                return true;
            }

            @Override
            public int size() {
                if (fromStart && toEnd)
                    return m.size();
                if (cachedSize == -1 || cachedSizeModCount != m.modCount) {
                    cachedSizeModCount = m.modCount;
                    int count = 0;
                    for (Iterator<?> it = iterator(); it.hasNext(); it.next())
                        count++;
                    cachedSize = count;
                }
                return cachedSize;
            }
        }

        /**
         * Returns an iterator over this view's entries in its own order.
         *
         * @return an entry iterator
         */
        abstract Iterator<Entry<K, V>> entryIterator();

        /**
         * Base class for range-view iterators.
         *
         * <p>Termination is decided by comparing the next node's key against a
         * precomputed <i>fence</i> key by reference identity, so iterating a
         * range view performs <b>no key comparisons at all</b>. The naive
         * alternative — re-testing {@code inRange(next.key)} on every step —
         * costs up to two comparator invocations per element.
         *
         * <p>The fence is captured as the fence node's <i>key reference</i>
         * rather than the node itself, because
         * {@link CustomTreeMap#deleteNode(Node)} may move a key from one node
         * object to another; the key reference follows the logical element,
         * a node reference does not.
         *
         * @param <T> the type yielded by the iterator
         */
        abstract class SubMapIterator<T> implements Iterator<T> {

            /**
             * The node that will be returned by the next call to {@code next()}.
             */
            Node<K, V> next;

            /**
             * The node most recently returned, or {@code null} if none is removable.
             */
            Node<K, V> lastReturned;

            /**
             * The key at which iteration stops, or {@link CustomTreeMap#UNBOUNDED}.
             */
            final Object fenceKey;

            /**
             * The backing map's {@code modCount} as of the last synchronisation point.
             */
            int expectedModCount;

            SubMapIterator(final Node<K, V> first, final Node<K, V> fence) {
                this.expectedModCount = m.modCount;
                this.lastReturned = null;
                this.next = first;
                this.fenceKey = fence == null ? UNBOUNDED : fence.key;
            }

            /**
             * {@inheritDoc} Two reference tests; no comparator calls.
             */
            @Override
            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            /**
             * Advances forward and returns the node just passed.
             *
             * @return the next node in ascending order
             */
            final Node<K, V> nextNode() {
                Node<K, V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = successor(e);
                lastReturned = e;
                return e;
            }

            /**
             * Advances backward and returns the node just passed.
             *
             * @return the next node in descending order
             */
            final Node<K, V> prevNode() {
                Node<K, V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = predecessor(e);
                lastReturned = e;
                return e;
            }

            /**
             * Removes the last returned element from an ascending traversal,
             * redirecting {@link #next} when the backing map relabels a
             * two-child node with its successor.
             */
            final void removeAscending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                if (lastReturned.left != null && lastReturned.right != null)
                    next = lastReturned;
                m.deleteNode(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

            /**
             * Removes the last returned element from a descending traversal.
             */
            final void removeDescending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                m.deleteNode(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }
        }

        /**
         * Ascending entry iterator over a range view. Yields live nodes.
         */
        final class SubMapEntryIterator extends SubMapIterator<Entry<K, V>> {

            SubMapEntryIterator(final Node<K, V> first, final Node<K, V> fence) {
                super(first, fence);
            }

            @Override
            public Entry<K, V> next() {
                return nextNode();
            }

            @Override
            public void remove() {
                removeAscending();
            }
        }

        /**
         * Descending entry iterator over a range view. Yields live nodes.
         */
        final class DescendingSubMapEntryIterator extends SubMapIterator<Entry<K, V>> {

            DescendingSubMapEntryIterator(final Node<K, V> last, final Node<K, V> fence) {
                super(last, fence);
            }

            @Override
            public Entry<K, V> next() {
                return prevNode();
            }

            @Override
            public void remove() {
                removeDescending();
            }
        }

        /**
         * Ascending key iterator over a range view.
         */
        final class SubMapKeyIterator extends SubMapIterator<K> {

            SubMapKeyIterator(final Node<K, V> first, final Node<K, V> fence) {
                super(first, fence);
            }

            @Override
            public K next() {
                return nextNode().key;
            }

            @Override
            public void remove() {
                removeAscending();
            }
        }

        /**
         * Descending key iterator over a range view.
         */
        final class DescendingSubMapKeyIterator extends SubMapIterator<K> {

            DescendingSubMapKeyIterator(final Node<K, V> last, final Node<K, V> fence) {
                super(last, fence);
            }

            @Override
            public K next() {
                return prevNode().key;
            }

            @Override
            public void remove() {
                removeDescending();
            }
        }
    }

    /**
     * A {@link NavigableSubMap} that iterates and compares keys in the same
     * (ascending) order as the backing map.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static final class AscendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        /**
         * Constructs an ascending range view over {@code map}. See
         * {@link NavigableSubMap#NavigableSubMap} for parameter details; bounds
         * are absolute.
         */
        AscendingSubMap(final CustomTreeMap<K, V> map, final boolean fromStart, final K lo, final boolean loInclusive,
                        final boolean toEnd, final K hi, final boolean hiInclusive) {
            super(map, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        /**
         * {@inheritDoc}
         */
        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        /**
         * {@inheritDoc}
         *
         * @return a {@link DescendingSubMap} over the same absolute range
         */
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> view = descendingMapView;
            return view != null ? view
                    : (descendingMapView = new DescendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive));
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if {@code toKey} lies outside this view's range
         */
        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m, fromStart, lo, loInclusive, false, toKey, inclusive);
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if either bound lies outside this view's range,
         *                                   or {@code fromKey} is greater than {@code toKey}
         */
        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive,
                                         final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (!inRange(fromKey, fromInclusive) || !inRange(toKey, toInclusive))
                throw new IllegalArgumentException("key out of range");
            int cmp = m.compare(fromKey, toKey);
            if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException();
            return new AscendingSubMap<>(m, false, fromKey, fromInclusive, false, toKey, toInclusive);
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if {@code fromKey} lies outside this view's range
         */
        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m, false, fromKey, inclusive, toEnd, hi, hiInclusive);
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return new SubMapEntryIterator(absLowest(), absHighFence());
        }

        @Override
        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Node<K, V> subLowest() {
            return absLowest();
        }

        @Override
        Node<K, V> subHighest() {
            return absHighest();
        }

        @Override
        Node<K, V> subCeiling(final K key) {
            return absCeiling(key);
        }

        @Override
        Node<K, V> subHigher(final K key) {
            return absHigher(key);
        }

        @Override
        Node<K, V> subFloor(final K key) {
            return absFloor(key);
        }

        @Override
        Node<K, V> subLower(final K key) {
            return absLower(key);
        }
    }

    /**
     * A {@link NavigableSubMap} that iterates and compares keys in reverse
     * (descending) order relative to the backing map's ordering.
     *
     * <p>Its {@link #lo}/{@link #hi} bounds remain absolute; only the mapping
     * from relative operations to absolute ones is reversed. A "first" element
     * is the absolutely highest in-range node, a "ceiling" is an absolute
     * floor, and so on.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static final class DescendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        /**
         * Lazily created reverse of the backing map's comparator.
         */
        private transient Comparator<? super K> reverseComparator;

        /**
         * Constructs a descending range view over {@code map}. See
         * {@link NavigableSubMap#NavigableSubMap} for parameter details; bounds
         * are absolute.
         */
        DescendingSubMap(final CustomTreeMap<K, V> map, final boolean fromStart, final K lo, final boolean loInclusive,
                         final boolean toEnd, final K hi, final boolean hiInclusive) {
            super(map, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        /**
         * {@inheritDoc}
         *
         * @return the backing map's comparator reversed, or the reverse
         *         natural ordering if the backing map uses natural ordering
         */
        public Comparator<? super K> comparator() {
            Comparator<? super K> result = reverseComparator;
            if (result == null) {
                Comparator<? super K> comp = m.comparator();
                result = reverseComparator = (comp == null) ? Comparator.reverseOrder() : comp.reversed();
            }
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @return an {@link AscendingSubMap} over the same absolute range
         */
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> view = descendingMapView;
            return view != null ? view
                    : (descendingMapView = new AscendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive));
        }

        /**
         * {@inheritDoc} Because iteration is reversed, a "head" holds the
         * largest keys and is expressed as a tighter absolute <i>lower</i> bound.
         *
         * @throws IllegalArgumentException if {@code toKey} lies outside this view's range
         */
        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m, false, toKey, inclusive, toEnd, hi, hiInclusive);
        }

        /**
         * {@inheritDoc} Note the reversed comparison, since {@code fromKey} is
         * expected to be greater than or equal to {@code toKey} in a descending
         * view's ordering.
         *
         * @throws IllegalArgumentException if either bound lies outside this view's range,
         *                                   or {@code fromKey} is less than {@code toKey}
         */
        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive,
                                         final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (!inRange(fromKey, fromInclusive) || !inRange(toKey, toInclusive))
                throw new IllegalArgumentException("key out of range");
            int cmp = m.compare(fromKey, toKey);
            if (cmp < 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException();
            return new DescendingSubMap<>(m, false, toKey, toInclusive, false, fromKey, fromInclusive);
        }

        /**
         * {@inheritDoc} Because iteration is reversed, a "tail" holds the
         * smallest keys and is expressed as a tighter absolute <i>upper</i> bound.
         *
         * @throws IllegalArgumentException if {@code fromKey} lies outside this view's range
         */
        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m, fromStart, lo, loInclusive, false, fromKey, inclusive);
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
        }

        @Override
        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Node<K, V> subLowest() {
            return absHighest();
        }

        @Override
        Node<K, V> subHighest() {
            return absLowest();
        }

        @Override
        Node<K, V> subCeiling(final K key) {
            return absFloor(key);
        }

        @Override
        Node<K, V> subHigher(final K key) {
            return absLower(key);
        }

        @Override
        Node<K, V> subFloor(final K key) {
            return absCeiling(key);
        }

        @Override
        Node<K, V> subLower(final K key) {
            return absHigher(key);
        }
    }

    /**
     * A {@link NavigableSet} view of the keys of a {@link NavigableMap} (either
     * the full {@link CustomTreeMap} or one of its range views). Navigation is
     * delegated to the backing map; iteration uses the backing map's dedicated
     * key iterators, so no {@link Map.Entry} objects are created.
     *
     * @param <E> the type of keys held by this set (and by the backing map)
     */
    private static final class KeySet<E extends Comparable<E>> extends AbstractSet<E> implements NavigableSet<E> {

        /**
         * The map whose keys this set is a view of.
         */
        private final NavigableMap<E, ?> map;

        /**
         * Constructs a key-set view backed by the given map.
         *
         * @param map the map whose keys this set will view
         */
        KeySet(final NavigableMap<E, ?> map) {
            this.map = map;
        }

        /**
         * {@inheritDoc}
         */
        public E ceiling(final E item) {
            return map.ceilingKey(item);
        }

        /**
         * {@inheritDoc}
         */
        public void clear() {
            map.clear();
        }

        /**
         * {@inheritDoc}
         */
        public Comparator<? super E> comparator() {
            return map.comparator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o) {
            return map.containsKey(o);
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<E> descendingIterator() {
            return map instanceof CustomTreeMap ? ((CustomTreeMap<E, Object>) map).descendingKeyIterator()
                                                : ((NavigableSubMap<E, Object>) map).descendingKeyIterator();
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<E> descendingSet() {
            return map.descendingMap().navigableKeySet();
        }

        /**
         * {@inheritDoc}
         */
        public E first() {
            return map.firstKey();
        }

        /**
         * {@inheritDoc}
         */
        public E floor(final E item) {
            return map.floorKey(item);
        }

        /**
         * {@inheritDoc}
         */
        public SortedSet<E> headSet(final E toElement) {
            return headSet(toElement, false);
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
            return map.headMap(toElement, inclusive).navigableKeySet();
        }

        /**
         * {@inheritDoc}
         */
        public E higher(final E item) {
            return map.higherKey(item);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        /**
         * Returns an iterator over the keys in this set, in ascending order.
         * Delegates to the backing map's dedicated key iterator, which
         * allocates no entry objects.
         *
         * @return an iterator over the keys in this set
         */
        public Iterator<E> iterator() {
            return map instanceof CustomTreeMap ? ((CustomTreeMap<E, Object>) map).keyIterator()
                                                : ((NavigableSubMap<E, Object>) map).keyIterator();
        }

        /**
         * {@inheritDoc}
         */
        public E last() {
            return map.lastKey();
        }

        /**
         * {@inheritDoc}
         */
        public E lower(final E item) {
            return map.lowerKey(item);
        }

        /**
         * {@inheritDoc}
         */
        public E pollFirst() {
            Map.Entry<E, ?> e = map.pollFirstEntry();
            return e == null ? null : e.getKey();
        }

        /**
         * {@inheritDoc}
         */
        public E pollLast() {
            Map.Entry<E, ?> e = map.pollLastEntry();
            return e == null ? null : e.getKey();
        }

        /**
         * {@inheritDoc} Correct for maps that permit {@code null} values: it
         * tests for presence rather than inferring it from a {@code null}
         * return of {@code remove}.
         */
        public boolean remove(final Object o) {
            if (!map.containsKey(o))
                return false;
            map.remove(o);
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return map.size();
        }

        /**
         * {@inheritDoc}
         */
        public SortedSet<E> subSet(final E fromElement, final E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
                                      final E toElement, final boolean toInclusive) {
            return map.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        /**
         * {@inheritDoc}
         */
        public SortedSet<E> tailSet(final E fromElement) {
            return tailSet(fromElement, true);
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
            return map.tailMap(fromElement, inclusive).navigableKeySet();
        }
    }

    /**
     * An internal, mutable red-black tree node, doubling as this map's
     * {@link Map.Entry} implementation. A {@code Node} is directly wired into
     * the tree structure via its {@link #left}, {@link #right}, and
     * {@link #parent} links.
     *
     * <p>Nodes are handed out by the entry-set iterators so that
     * {@link #setValue(Object)} writes through to the map, as the {@link Map}
     * contract requires. They must not be retained after the iterator has
     * advanced, and the structural links are never exposed.
     *
     * @param <K> the type of the key held by this node
     * @param <V> the type of the value held by this node
     */
    private static final class Node<K, V> implements Map.Entry<K, V> {

        /**
         * This node's key.
         */
        K key;

        /**
         * This node's currently associated value.
         */
        V value;

        /**
         * This node's left child, or {@code null} if none.
         */
        Node<K, V> left;

        /**
         * This node's right child, or {@code null} if none.
         */
        Node<K, V> right;

        /**
         * This node's parent, or {@code null} if this is the root.
         */
        Node<K, V> parent;

        /**
         * This node's color; defaults to {@link CustomTreeMap#BLACK} until set otherwise (e.g. on insertion).
         */
        boolean color = BLACK;

        /**
         * Constructs a new node with the given key, value, and parent. Its
         * color defaults to {@link CustomTreeMap#BLACK} and it has no children.
         *
         * @param key    this node's key
         * @param value  this node's initial value
         * @param parent this node's parent, or {@code null} if it will be the root
         */
        Node(final K key, final V value, final Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V setValue(final V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> e))
                return false;
            return valueEquals(key, e.getKey()) && valueEquals(value, e.getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        /**
         * Returns a string representation of this node in the form
         * {@code key=value}.
         *
         * @return a string representation of this node
         */
        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}