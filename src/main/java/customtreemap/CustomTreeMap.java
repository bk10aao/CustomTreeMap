package customtreemap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
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
     * Marker used for the color of a black node. Also the color of a {@code null} (absent) node.
     */
    private static final boolean BLACK = false;

    /**
     * The root node of the tree, or {@code null} if the map is empty.
     */
    private Node<K, V> root;

    /**
     * The number of key-value mappings currently in the map.
     */
    private int size = 0;

    /**
     * The comparator used to order keys, or {@code null} if natural ordering is used.
     */
    private final Comparator<? super K> comparator;

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
     * ordering as the given sorted map.
     *
     * @param m the sorted map whose mappings and comparator are to be used
     * @throws NullPointerException if {@code m} is {@code null}
     */
    public CustomTreeMap(final SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        putAll(m);
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
        return exportEntry(ceilingKey(key));
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
        Node<K, V> p = getCeilingNode(key, true);
        return p == null ? null : p.key;
    }

    /**
     * Removes all mappings from this map. The map will be empty after this
     * call returns.
     */
    public void clear() {
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
     * reflected in the returned map, and vice versa.
     *
     * @return a reverse-order view of this map
     */
    public NavigableMap<K, V> descendingMap() {
        return new DescendingSubMap<>(this, false, null, false, false, null, false);
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map, ordered
     * by key in ascending order. The set is backed by the map, so changes to
     * the map are reflected in the set, and vice versa. Iterating the set
     * traverses the tree in-order via {@link #successor(Node)}.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public void clear() {
                CustomTreeMap.this.clear();
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private Node<K, V> nextNode = getFirstNode();
                    private Node<K, V> lastReturned = null;

                    @Override
                    public boolean hasNext() {
                        return nextNode != null;
                    }

                    @Override
                    public Entry<K, V> next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        lastReturned = nextNode;
                        nextNode = successor(nextNode);
                        return exportEntry(lastReturned.key);
                    }

                    @Override
                    public void remove() {
                        if (lastReturned == null)
                            throw new IllegalStateException();
                        deleteNode(lastReturned);
                        lastReturned = null;
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    /**
     * Returns the entry associated with the least key in this map, or
     * {@code null} if the map is empty.
     *
     * @return the entry with the least key, or {@code null} if this map is empty
     */
    public Entry<K, V> firstEntry() {
        Node<K, V> p = getFirstNode();
        return p == null ? null : exportEntry(p.key);
    }

    /**
     * Returns the least key currently in this map.
     *
     * @return the least key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    public K firstKey() {
        Node<K, V> p = getFirstNode();
        if (p == null)
            throw new NoSuchElementException();
        return p.key;
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
        return exportEntry(floorKey(key));
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
        Node<K, V> p = getFloorNode(key, true);
        return p == null ? null : p.key;
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
        return new AscendingSubMap<>(this, false, null, false, true, toKey, inclusive);
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
        return exportEntry(higherKey(key));
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
        Node<K, V> p = getCeilingNode(key, false);
        return p == null ? null : p.key;
    }

    /**
     * Returns the entry associated with the greatest key in this map, or
     * {@code null} if the map is empty.
     *
     * @return the entry with the greatest key, or {@code null} if this map is empty
     */
    public Entry<K, V> lastEntry() {
        Node<K, V> p = getLastNode();
        return p == null ? null : exportEntry(p.key);
    }

    /**
     * Returns the greatest key currently in this map.
     *
     * @return the greatest key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    public K lastKey() {
        Node<K, V> p = getLastNode();
        if (p == null)
            throw new NoSuchElementException();
        return p.key;
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
        return exportEntry(lowerKey(key));
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
        Node<K, V> p = getFloorNode(key, false);
        return p == null ? null : p.key;
    }

    /**
     * Returns a {@link NavigableSet} view of the keys contained in this map,
     * in ascending order. The set is backed by the map.
     *
     * @return a navigable set view of the keys in this map
     */
    public NavigableSet<K> navigableKeySet() {
        return new KeySet<>(this);
    }

    /**
     * Removes and returns the entry associated with the least key in this
     * map, or returns {@code null} if the map is empty.
     *
     * @return the removed first entry, or {@code null} if this map is empty
     */
    public Entry<K, V> pollFirstEntry() {
        Node<K, V> p = getFirstNode();
        if (p == null)
            return null;
        Entry<K, V> e = exportEntry(p.key);
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
        if (p == null)
            return null;
        Entry<K, V> e = exportEntry(p.key);
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
        if (root == null) {
            root = new Node<>(key, value, null);
            size = 1;
            return null;
        }
        Node<K, V> t = root;
        Node<K, V> parent;
        int cmp;
        do {
            parent = t;
            cmp = compare(key, t.key);
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
        return null;
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
        return new AscendingSubMap<>(this, true, fromKey, fromInclusive, true, toKey, toInclusive);
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
        return new AscendingSubMap<>(this, true, fromKey, inclusive, false, null, false);
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
     * Removes the given node from the tree, rewiring child/parent links and
     * balancing the tree afterwards if a black node was removed.
     *
     * <p>If the node to delete has two children, its position is instead
     * relabeled with its in-order {@link #successor(Node)}'s key/value, and
     * the successor node (which has at most one child) is physically unlinked
     * in its place.
     *
     * @param p the node to remove; must not be {@code null}
     */
    private void deleteNode(Node<K, V> p) {
        size--;
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
     * Wraps the value for the given key in an immutable, disconnected
     * {@link Map.Entry} suitable for returning to callers, so that external
     * code cannot mutate the tree's internal {@link Node} objects.
     *
     * @param key the key to look up and export, may be {@code null}
     * @return an immutable entry for {@code key}, or {@code null} if
     *         {@code key} is {@code null} or not present in the map
     */
    private Map.Entry<K, V> exportEntry(final K key) {
        if (key == null)
            return null;
        Node<K, V> node = getNode(key);
        return node == null ? null : new SimpleImmutableEntry<>(node.key, node.value);
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
        while (x != root && colorOf(x) == BLACK)
            if (x == leftOf(parentOf(x))) {
                Node<K, V> sib = rightOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else {
                Node<K, V> sib = leftOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
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
        while (x != null && x != root && x.parent.color == RED)
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Node<K, V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<K, V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
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
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                candidate = p;
                p = p.left;
            } else {
                if (cmp == 0)
                    if (inclusive)
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
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                candidate = p;
                p = p.right;
            } else if (cmp < 0) {
                p = p.left;
            } else {
                if (inclusive) return p;
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
     * Performs a standard binary-search-tree lookup for the given key.
     *
     * @param key the key to search for, may be {@code null}
     * @return the node whose key equals {@code key}, or {@code null} if
     *         {@code key} is {@code null} or not found
     */
    private Node<K, V> getNode(final Object key) {
        if (key == null)
            return null;
        Node<K, V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else return p;
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
    private Node<K, V> predecessor(final Node<K, V> t) {
        if (t == null)
            return null;
        else if (t.left != null) {
            Node<K, V> p = t.left;
            while (p.right != null) p = p.right;
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
    private Node<K, V> successor(final Node<K, V> t) {
        if (t == null)
            return null;
        else if (t.right != null) {
            Node<K, V> p = t.right;
            while (p.left != null) p = p.left;
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
     * A {@link NavigableSubMap} that iterates and compares keys in the same
     * (ascending) order as the backing map. This is the view returned by
     * {@link CustomTreeMap#subMap(Comparable, boolean, Comparable, boolean)},
     * {@link CustomTreeMap#headMap(Comparable, boolean)}, and
     * {@link CustomTreeMap#tailMap(Comparable, boolean)}.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static class AscendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        /**
         * Constructs an ascending range view over {@code map} restricted to
         * the given bounds. See {@link NavigableSubMap#NavigableSubMap} for
         * parameter details.
         */
        AscendingSubMap(final CustomTreeMap<K, V> map, final boolean usesFromBound, final K fromKey,
                        final boolean fromInclusive, final boolean usesToBound, final K toKey,
                        final boolean toInclusive) {
            super(map, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        /**
         * {@inheritDoc} Delegates directly to the backing map's {@code ceilingKey}.
         */
        K absCeiling(final K key) {
            return m.ceilingKey(key);
        }

        /**
         * {@inheritDoc} Delegates directly to the backing map's {@code floorKey}.
         */
        K absFloor(final K key) {
            return m.floorKey(key);
        }

        /**
         * {@inheritDoc} Delegates directly to the backing map's {@code higherKey}.
         */
        K absHigher(final K key) {
            return m.higherKey(key);
        }

        /**
         * {@inheritDoc} Delegates directly to the backing map's {@code lowerKey}.
         */
        K absLower(K key) {
            return m.lowerKey(key);
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
         * @return a {@link DescendingSubMap} over the same key range
         */
        public NavigableMap<K, V> descendingMap() {
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if {@code toKey} lies outside this view's range
         */
        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException();
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        /**
         * {@inheritDoc} For an ascending view, "too high" means at or beyond
         * this view's upper ({@code toKey}) bound.
         */
        boolean isTooHigh(final K key, final boolean inclusive) {
            if (!usesToBound)
                return false;
            int c = m.compare(key, toKey);
            return c > 0 || (c == 0 && (!toInclusive && inclusive));
        }

        /**
         * {@inheritDoc} For an ascending view, "too low" means at or below
         * this view's lower ({@code fromKey}) bound.
         */
        boolean isTooLow(final K key, final boolean inclusive) {
            if (!usesFromBound)
                return false;
            int c = m.compare(key, fromKey);
            return c < 0 || (c == 0 && (!fromInclusive && inclusive));
        }

        /**
         * {@inheritDoc} For an ascending view, stepping forward means the in-order successor.
         */
        public Node<K, V> stepNode(final Node<K, V> node) {
            return m.successor(node);
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if the bounds lie outside this view's range,
         *                                   or {@code fromKey} is greater than {@code toKey}
         */
        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (!inRange(fromKey, fromInclusive) || !inRange(toKey, toInclusive))
                throw new IllegalArgumentException();
            int cmp = m.compare(fromKey, toKey);
            if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException();
            return new AscendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        /**
         * {@inheritDoc} The first node is the ceiling of {@code fromKey} (or
         * the backing map's first node if unbounded below); the last node is
         * the floor of {@code toKey} (or the backing map's last node if
         * unbounded above).
         */
        public Node<K, V> subMapNode(final boolean isFirst) {
            if (m.root == null) return null;
            return isFirst ? (usesFromBound ? m.getCeilingNode(fromKey, fromInclusive) : m.getFirstNode())
                    : (usesToBound ? m.getFloorNode(toKey, toInclusive) : m.getLastNode());
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException if {@code fromKey} lies outside this view's range
         */
        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException();
            return new AscendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }
    }

    /**
     * A {@link NavigableSubMap} that iterates and compares keys in reverse
     * (descending) order relative to the backing map's natural ordering.
     * This is the view returned by {@link CustomTreeMap#descendingMap()} and
     * by calling {@code descendingMap()} on an {@link AscendingSubMap}.
     *
     * <p>Because iteration is reversed, the roles of the {@code fromKey}/
     * {@code toKey} bounds are swapped relative to {@link AscendingSubMap}:
     * {@code fromKey} acts as the effective upper bound and {@code toKey} as
     * the effective lower bound when testing range membership.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static class DescendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        /**
         * Constructs a descending range view over {@code map} restricted to
         * the given bounds. See {@link NavigableSubMap#NavigableSubMap} for
         * parameter details.
         */
        DescendingSubMap(final CustomTreeMap<K, V> map, final boolean usesFromBound, final K fromKey,
                         final boolean fromInclusive, final boolean usesToBound, final K toKey,
                         final boolean toInclusive) {
            super(map, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        /**
         * {@inheritDoc} In reverse order, "ceiling" corresponds to the backing map's {@code floorKey}.
         */
        K absCeiling(final K key) {
            return m.floorKey(key);
        }

        /**
         * {@inheritDoc} In reverse order, "floor" corresponds to the backing map's {@code ceilingKey}.
         */
        K absFloor(final K key) {
            return m.ceilingKey(key);
        }

        /**
         * {@inheritDoc} In reverse order, "higher" corresponds to the backing map's {@code lowerKey}.
         */
        K absHigher(final K key) {
            return m.lowerKey(key);
        }

        /**
         * {@inheritDoc} In reverse order, "lower" corresponds to the backing map's {@code higherKey}.
         */
        K absLower(final K key) {
            return m.higherKey(key);
        }

        /**
         * {@inheritDoc}
         *
         * @return the backing map's comparator reversed, or the reverse
         *         natural ordering if the backing map uses natural ordering
         */
        public Comparator<? super K> comparator() {
            Comparator<? super K> comp = m.comparator();
            return (comp == null) ? Comparator.reverseOrder() : comp.reversed();
        }

        /**
         * {@inheritDoc}
         *
         * @return an {@link AscendingSubMap} over the same key range
         */
        public NavigableMap<K, V> descendingMap() {
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        /**
         * {@inheritDoc} Since iteration is reversed, a "head" (the largest
         * keys) is implemented as a further restriction on {@code fromKey}.
         *
         * @throws IllegalArgumentException if {@code toKey} lies outside this view's range
         */
        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        /**
         * {@inheritDoc} For a descending view, "too high" means at or beyond
         * this view's {@code fromKey} bound (the effective upper bound in
         * reverse order).
         */
        boolean isTooHigh(final K key, final boolean inclusive) {
            if (!usesFromBound)
                return false;
            int c = m.compare(key, fromKey);
            return c > 0 || (c == 0 && (!fromInclusive && inclusive));
        }

        /**
         * {@inheritDoc} For a descending view, "too low" means at or below
         * this view's {@code toKey} bound (the effective lower bound in
         * reverse order).
         */
        boolean isTooLow(final K key, final boolean inclusive) {
            if (!usesToBound)
                return false;
            int c = m.compare(key, toKey);
            return c < 0 || (c == 0 && (!toInclusive && inclusive));
        }

        /**
         * {@inheritDoc} For a descending view, stepping forward means the in-order predecessor.
         */
        Node<K, V> stepNode(final Node<K, V> node) {
            return m.predecessor(node);
        }
        /**
         * {@inheritDoc} Note the reversed comparison ({@code cmp < 0}) since
         * {@code fromKey} is expected to be greater than or equal to
         * {@code toKey} in a descending view's ordering.
         *
         * @throws IllegalArgumentException if the bounds lie outside this view's range,
         *                                   or {@code fromKey} is less than {@code toKey}
         */
        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive,
                                         final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (!inRange(fromKey, fromInclusive) || !inRange(toKey, toInclusive))
                throw new IllegalArgumentException();
            int cmp = m.compare(fromKey, toKey);
            if (cmp < 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException();
            return new DescendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        /**
         * {@inheritDoc} The first node (in reverse order) is the floor of
         * {@code fromKey} (or the backing map's last node if unbounded); the
         * last node is the ceiling of {@code toKey} (or the backing map's
         * first node if unbounded).
         */
        Node<K, V> subMapNode(final boolean isFirst) {
            if (m.root == null)
                return null;
            return isFirst
                    ? (usesFromBound ? m.getFloorNode(fromKey, fromInclusive) : m.getLastNode())
                    : (usesToBound   ? m.getCeilingNode(toKey, toInclusive)   : m.getFirstNode());
        }

        /**
         * {@inheritDoc} Since iteration is reversed, a "tail" (the smallest
         * keys) is implemented as a further restriction on {@code toKey}.
         *
         * @throws IllegalArgumentException if {@code fromKey} lies outside this view's range
         */
        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }
    }

    /**
     * A {@link NavigableSet} view of the keys of a {@link NavigableMap}
     * (either the full {@link CustomTreeMap} or one of its range views). Every
     * operation is delegated to the corresponding key- or entry-based
     * operation on {@link #map}, so the set stays live with the backing map.
     *
     * @param <E> the type of keys held by this set (and by the backing map)
     */
    private static class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {

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
            return descendingSet().iterator();
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
         * Returns an iterator over the keys in this set, in ascending order.
         * Implemented by wrapping the backing map's entry-set iterator and
         * projecting each entry down to its key; removal is delegated
         * accordingly.
         *
         * @return an iterator over the keys in this set
         */
        public Iterator<E> iterator() {
            Iterator<? extends Map.Entry<E, ?>> entryIterator = map.entrySet().iterator();
            return new Iterator<>() {
                public boolean hasNext() {
                    return entryIterator.hasNext();
                }

                public E next() {
                    return entryIterator.next().getKey();
                }

                public void remove() {
                    entryIterator.remove();
                }
            };
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
         * {@inheritDoc}
         */
        public boolean remove(final Object o) {
            return map.remove(o) != null;
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
        public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
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
     * Abstract base class for the {@code NavigableMap} range-view returned by
     * {@link CustomTreeMap#subMap(Comparable, boolean, Comparable, boolean)},
     * {@link CustomTreeMap#headMap(Comparable, boolean)}, and
     * {@link CustomTreeMap#tailMap(Comparable, boolean)}. All operations are
     * delegated to the backing {@link #m} instance and restricted to the
     * {@code [fromKey, toKey)}-style range described by the bound fields,
     * so the view stays live: modifications made through it, or directly on
     * the backing map, are visible through both.
     *
     * <p>Subclasses {@link AscendingSubMap} and {@link DescendingSubMap}
     * supply the direction-dependent logic (which bound is "low" vs "high",
     * which sibling node is "next", etc.), since the same range-checking
     * skeleton is reused for both forward and reverse views.
     *
     * @param <K> the type of keys maintained by the backing map
     * @param <V> the type of mapped values
     */
    private static abstract class NavigableSubMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        /**
         * The backing map that this view delegates to.
         */
        final CustomTreeMap<K, V> m;

        /**
         * The lower bound of this view,
         * or {@code null}
         * if {@link #usesFromBound} is {@code false}.
         */
        final K fromKey;

        /**
         * Whether {@link #fromKey} itself is included in this view.
         */
        final boolean fromInclusive;

        /**
         * The upper bound of this view, or {@code null} if {@link #usesToBound} is {@code false}.
         */
        final K toKey;

        /**
         * Whether {@link #toKey} itself is included in this view.
         */
        final boolean toInclusive;

        /**
         * Whether this view has a lower bound at all.
         */
        final boolean usesFromBound;

        /**
         * Whether this view has an upper bound at all.
         */
        final boolean usesToBound;

        /**
         * Constructs a range view over {@code map} restricted to the given bounds.
         *
         * @param map           the backing map
         * @param usesFromBound whether a lower bound is in effect
         * @param fromKey       the lower bound, ignored if {@code usesFromBound} is {@code false}
         * @param fromInclusive whether the lower bound is inclusive
         * @param usesToBound   whether an upper bound is in effect
         * @param toKey         the upper bound, ignored if {@code usesToBound} is {@code false}
         * @param toInclusive   whether the upper bound is inclusive
         */
        NavigableSubMap(final CustomTreeMap<K, V> map, final boolean usesFromBound, final K fromKey,
                        final boolean fromInclusive, final boolean usesToBound, final K toKey,
                        final boolean toInclusive) {
            this.m = map;
            this.usesFromBound = usesFromBound;
            this.fromKey = fromKey;
            this.fromInclusive = fromInclusive;
            this.usesToBound = usesToBound;
            this.toKey = toKey;
            this.toInclusive = toInclusive;
        }

        /**
         * Returns whether {@code key} falls below the effective lower bound
         * of this view (direction-dependent; see subclasses).
         *
         * @param key       the key to test
         * @param inclusive whether the comparison should treat the bound as inclusive
         * @return {@code true} if {@code key} is out of range on the low side
         */
        abstract boolean isTooLow(final K key, final boolean inclusive);

        /**
         * Returns whether {@code key} falls above the effective upper bound
         * of this view (direction-dependent; see subclasses).
         *
         * @param key       the key to test
         * @param inclusive whether the comparison should treat the bound as inclusive
         * @return {@code true} if {@code key} is out of range on the high side
         */
        abstract boolean isTooHigh(final K key, final boolean inclusive);

        /**
         * Returns the first or last node of this view, according to its
         * traversal direction.
         *
         * @param isFirst {@code true} to return this view's first node, {@code false} for its last
         * @return the requested boundary node of this view, or {@code null} if the view is empty
         */
        abstract Node<K, V> subMapNode(final boolean isFirst);

        /**
         * Returns the node adjacent to {@code node} in this view's traversal
         * direction (successor for an ascending view, predecessor for a
         * descending view).
         *
         * @param node the node to step from
         * @return the next node in this view's iteration order
         */
        abstract Node<K, V> stepNode(final Node<K, V> node);

        /**
         * Delegates to the backing map's {@code floorKey}/{@code ceilingKey}
         * as appropriate for this view's direction to find the greatest key
         * less than or equal to {@code key} in absolute (backing-map) ordering.
         *
         * @param key the key to match
         * @return the matching key, or {@code null} if none exists
         */
        abstract K absFloor(K key);


        /**
         * Delegates to the backing map's {@code lowerKey}/{@code higherKey}
         * as appropriate for this view's direction to find the greatest key
         * strictly less than {@code key} in absolute (backing-map) ordering.
         *
         * @param key the key to match
         * @return the matching key, or {@code null} if none exists
         */
        abstract K absLower(K key);

        /**
         * Delegates to the backing map's {@code ceilingKey}/{@code floorKey}
         * as appropriate for this view's direction to find the least key
         * greater than or equal to {@code key} in absolute (backing-map) ordering.
         *
         * @param key the key to match
         * @return the matching key, or {@code null} if none exists
         */
        abstract K absCeiling(K key);

        /**
         * Delegates to the backing map's {@code higherKey}/{@code lowerKey}
         * as appropriate for this view's direction to find the least key
         * strictly greater than {@code key} in absolute (backing-map) ordering.
         *
         * @param key the key to match
         * @return the matching key, or {@code null} if none exists
         */
        abstract K absHigher(K key);

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> ceilingEntry(final K key) {
            return m.exportEntry(ceilingKey(key));
        }

        /**
         * {@inheritDoc}
         */
        public K ceilingKey(final K key) {
            requireNonNull(key);
            K candidate = isTooLow(key, true) ? getSubMapKey(true) : absCeiling(key);
            return inRange(candidate) ? candidate : null;
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
            return inRange(key) && m.containsKey(key);
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
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private Node<K, V> nextNode = subMapNode(true);
                        private Node<K, V> lastReturned = null;

                        @Override
                        public boolean hasNext() {
                            return nextNode != null && inRange(nextNode.key);
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!hasNext())
                                throw new NoSuchElementException();
                            lastReturned = nextNode;
                            nextNode = stepNode(nextNode);
                            return m.exportEntry(lastReturned.key);
                        }

                        @Override
                        public void remove() {
                            if (lastReturned == null)
                                throw new IllegalStateException();
                            m.deleteNode(lastReturned);
                            lastReturned = null;
                        }
                    };
                }

                @Override
                public int size() {
                    int count = 0;
                    for (Node<K, V> n = subMapNode(true); n != null && inRange(n.key); n = stepNode(n))
                        count++;
                    return count;
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> firstEntry() {
            K k = getSubMapKey(true);
            return k == null ? null : m.exportEntry(k);
        }

        /**
         * {@inheritDoc}
         */
        public K firstKey() {
            K k = getSubMapKey(true);
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> floorEntry(final K key) {
            return m.exportEntry(floorKey(key));
        }

        /**
         * {@inheritDoc}
         */
        public K floorKey(final K key) {
            requireNonNull(key);
            K candidate = isTooHigh(key, true) ? getSubMapKey(false) : absFloor(key);
            return inRange(candidate) ? candidate : null;
        }

        /**
         * {@inheritDoc}
         */
        public V get(final Object key) {
            return inRange(key) ? m.get(key) : null;
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
            return m.exportEntry(higherKey(key));
        }

        /**
         * {@inheritDoc}
         */
        public K higherKey(final K key) {
            requireNonNull(key);
            K candidate = isTooLow(key, false) ? getSubMapKey(true) : absHigher(key);
            return inRange(candidate) ? candidate : null;
        }

        /**
         * Returns whether the given object is a non-null key that lies
         * within this view's bounds.
         *
         * @param key the key to test, may be of any type
         * @return {@code true} if {@code key} is non-null and in range
         */
        final boolean inRange(final Object key) {
            return key != null && inRange((K) key);
        }

        /**
         * Returns whether the given key lies within this view's bounds,
         * treating both bounds as inclusive for this check.
         *
         * @param key the key to test
         * @return {@code true} if {@code key} is in range
         */
        final boolean inRange(final K key) {
            return !isTooLow(key, true) && !isTooHigh(key, true);
        }

        /**
         * Returns whether the given key lies within this view's bounds, using
         * the given inclusivity for the boundary comparison.
         *
         * @param key       the key to test
         * @param inclusive whether the boundary comparison should be treated as inclusive
         * @return {@code true} if {@code key} is in range
         */
        final boolean inRange(final K key, final boolean inclusive) {
            return !isTooLow(key, inclusive) && !isTooHigh(key, inclusive);
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> lastEntry() {
            K k = getSubMapKey(false);
            return k == null ? null : m.exportEntry(k);
        }

        /**
         * {@inheritDoc}
         */
        public K lastKey() {
            K k = getSubMapKey(false);
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> lowerEntry(final K key) {
            return m.exportEntry(lowerKey(key));
        }

        /**
         * {@inheritDoc}
         */
        public K lowerKey(final K key) {
            requireNonNull(key);
            K candidate = isTooHigh(key, false) ? getSubMapKey(false) : absLower(key);
            return inRange(candidate) ? candidate : null;
        }

        /**
         * {@inheritDoc}
         */
        public NavigableSet<K> navigableKeySet() {
            return new KeySet<>(this);
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> e = firstEntry();
            if (e != null)
                m.remove(e.getKey());
            return e;
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> pollLastEntry() {
            Entry<K, V> e = lastEntry();
            if (e != null)
                m.remove(e.getKey());
            return e;
        }

        /**
         * {@inheritDoc}
         */
        public V put(final K key, final V value) {
            if (!inRange(key))
                throw new IllegalArgumentException();
            return m.put(key, value);
        }

        /**
         * {@inheritDoc}
         */
        public V remove(final Object key) {
            return inRange(key) ? m.remove(key) : null;
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
         * Returns the first or last key of this view, or {@code null} if the
         * view is empty or the boundary node found falls outside the range
         * (which can happen right after a mutation).
         *
         * @param isFirst {@code true} to fetch this view's first key, {@code false} for its last
         * @return the requested boundary key, or {@code null} if none exists in range
         */
        private K getSubMapKey(boolean isFirst) {
            Node<K, V> n = subMapNode(isFirst);
            return (n != null && inRange(n.key)) ? n.key : null;
        }
    }

    /**
     * An internal, mutable red-black tree node, doubling as this map's
     * {@link Map.Entry} implementation. Unlike the entries returned to
     * callers (see {@link CustomTreeMap#exportEntry(Comparable)}), a
     * {@code Node} is directly wired into the tree structure via its
     * {@link #left}, {@link #right}, and {@link #parent} links and should
     * never be exposed outside the enclosing {@link CustomTreeMap}.
     *
     * @param <K> the type of the key held by this node
     * @param <V> the type of the value held by this node
     */
    private static class Node<K, V> implements Map.Entry<K, V> {

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
        Node(final K key, V value, final Node<K, V> parent) {
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