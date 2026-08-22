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

public class CustomTreeMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private Node<K, V> root;
    private int size = 0;
    private final Comparator<? super K> comparator;

    public CustomTreeMap() {
        this.comparator = null;
    }

    public CustomTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public CustomTreeMap(Map<? extends K, ? extends V> m) {
        this.comparator = null;
        putAll(m);
    }

    public CustomTreeMap(SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        putAll(m);
    }

    private static class Node<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> parent;
        boolean color = BLACK;

        Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        @Override
        public K getKey() {
            return key; }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    private int compare(Object k1, Object k2) {
        if (comparator != null)
            return comparator.compare((K) k1, (K) k2);
        return ((Comparable<? super K>) k1).compareTo((K) k2);
    }

    private Map.Entry<K, V> exportEntry(K key) {
        if (key == null) return null;
        Node<K, V> node = getNode(key);
        return node == null ? null : new AbstractMap.SimpleImmutableEntry<>(node.key, node.value);
    }

    private Node<K, V> getNode(Object key) {
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

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        if(key == null)
            throw new NullPointerException();
        return getNode(key) != null;
    }

    public V get(Object key) {
        Node<K, V> p = getNode(key);
        return p == null ? null : p.value;
    }

    public V put(K key, V value) {
        if (key == null)
            throw new NullPointerException("Null keys not supported");
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
            if (cmp < 0) t = t.left;
            else if (cmp > 0) t = t.right;
            else return t.setValue(value);
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

    public V remove(Object key) {
        Node<K, V> p = getNode(key);
        if (p == null)
            return null;
        V oldValue = p.value;
        deleteNode(p);
        return oldValue;
    }

    public void clear() {
        size = 0;
        root = null;
    }

    private void rotateLeft(Node<K, V> p) {
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

    private void rotateRight(Node<K, V> p) {
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

    private void fixAfterInsertion(Node<K, V> x) {
        x.color = RED;
        while (x != null && x != root && x.parent.color == RED) {
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
        }
        root.color = BLACK;
    }

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

    private void fixAfterDeletion(Node<K, V> x) {
        while (x != root && colorOf(x) == BLACK) {
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
        }
        setColor(x, BLACK);
    }

    private static <K, V> boolean colorOf(Node<K, V> p) {
        return p == null ? BLACK : p.color;
    }
    private static <K, V> Node<K, V> parentOf(Node<K, V> p) {
        return p == null ? null : p.parent;
    }
    private static <K, V> void setColor(Node<K, V> p, boolean c) {
        if (p != null) p.color = c;
    }

    private static <K, V> Node<K, V> leftOf(Node<K, V> p) {
        return p == null ? null : p.left;
    }

    private static <K, V> Node<K, V> rightOf(Node<K, V> p) {
        return p == null ? null : p.right;
    }

    private Node<K, V> successor(Node<K, V> t) {
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

    public K firstKey() {
        if (root == null)
            throw new NoSuchElementException();
        Node<K, V> p = root;
        while (p.left != null)
            p = p.left;
        return p.key;
    }

    public K lastKey() {
        if (root == null)
            throw new NoSuchElementException();
        Node<K, V> p = root;
        while (p.right != null)
            p = p.right;
        return p.key;
    }

    public Entry<K, V> firstEntry() {
        return exportEntry(isEmpty() ? null : firstKey());
    }

    public Entry<K, V> lastEntry() {
        return exportEntry(isEmpty() ? null : lastKey());
    }

    public Entry<K, V> pollFirstEntry() {
        if (root == null)
            return null;
        K k = firstKey();
        Entry<K, V> e = exportEntry(k);
        remove(k);
        return e;
    }

    public Entry<K, V> pollLastEntry() {
        if (root == null)
            return null;
        K k = lastKey();
        Entry<K, V> e = exportEntry(k);
        remove(k);
        return e;
    }

    public K lowerKey(K key) {
        Node<K, V> p = root;
        K candidate = null;
        while (p != null)
            if (compare(key, p.key) > 0) {
                candidate = p.key;
                p = p.right;
            } else
                p = p.left;
        return candidate;
    }

    public K floorKey(K key) {
        if(key == null)
            throw new NullPointerException();
        Node<K, V> p = root;
        K candidate = null;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                candidate = p.key;
                p = p.right;
            } else if (cmp < 0)
                p = p.left;
            else
                return p.key;
        }
        return candidate;
    }

    public K ceilingKey(K key) {
        Node<K, V> p = root;
        K candidate = null;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                candidate = p.key;
                p = p.left;
            } else if (cmp > 0)
                p = p.right;
            else
                return p.key;
        }
        return candidate;
    }

    public K higherKey(K key) {
        Node<K, V> p = root;
        K candidate = null;
        while (p != null)
            if (compare(key, p.key) < 0) {
                candidate = p.key;
                p = p.left;
            } else
                p = p.right;
        return candidate;
    }

    public Entry<K, V> lowerEntry(K key) {
        requireNonNull(key);
        return exportEntry(lowerKey(key));
    }

    public Entry<K, V> floorEntry(K key) {
        requireNonNull(key);
        return exportEntry(floorKey(key));
    }

    public Entry<K, V> ceilingEntry(K key) {
        requireNonNull(key);
        return exportEntry(ceilingKey(key));
    }

    public Entry<K, V> higherEntry(K key) {
        requireNonNull(key);
        return exportEntry(higherKey(key));
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    public NavigableSet<K> navigableKeySet() {
        return new KeySet<>(this);
    }

    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    public NavigableMap<K, V> descendingMap() {
        return new DescendingSubMap<>(this, false, null, false, false, null, false);
    }

    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        int cmp = compare(fromKey, toKey);
        if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
            throw new IllegalArgumentException("fromKey > toKey");
        return new AscendingSubMap<>(this, true, fromKey, fromInclusive, true, toKey, toInclusive);
    }

    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        requireNonNull(toKey);
        return new AscendingSubMap<>(this, false, null, false, true, toKey, inclusive);
    }

    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this, true, fromKey, inclusive, false, null, false);
    }

    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private Node<K, V> nextNode = root == null ? null : getNode(firstKey());
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

            @Override public int size() {
                return size;
            }
        };
    }

    private static class AscendingSubMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final CustomTreeMap<K, V> m;
        private final K fromKey;
        private final boolean fromInclusive;
        private final K toKey;
        private final boolean toInclusive;
        private final boolean usesFromBound;
        private final boolean usesToBound;

        AscendingSubMap(CustomTreeMap<K, V> map,
                        boolean usesFromBound, K fromKey, boolean fromInclusive,
                        boolean usesToBound, K toKey, boolean toInclusive) {
            this.m = map;
            this.usesFromBound = usesFromBound;
            this.fromKey = fromKey;
            this.fromInclusive = fromInclusive;
            this.usesToBound = usesToBound;
            this.toKey = toKey;
            this.toInclusive = toInclusive;
        }

        private boolean isTooLow(K key, boolean inclusive) {
            if (!usesFromBound)
                return false;
            int c = m.compare(key, fromKey);
            if (c < 0)
                return true;
            return c == 0 && (!fromInclusive && inclusive);
        }

        private boolean isTooHigh(K key, boolean inclusive) {
            if (!usesToBound)
                return false;
            int c = m.compare(key, toKey);
            if (c > 0)
                return true;
            return c == 0 && (!toInclusive && inclusive);
        }

        public boolean inRange(K key) {
            return !isTooLow(key, true) && !isTooHigh(key, true);
        }

        private boolean inRange(Object key) {
            return key != null && inRange((K) key);
        }

        public V get(Object key) {
            return inRange(key) ? m.get(key) : null;
        }

        public boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        public V put(K key, V value) {
            if (!inRange(key)) throw new IllegalArgumentException("Key out of range");
            return m.put(key, value);
        }

        public V remove(Object key) {
            return inRange(key) ? m.remove(key) : null;
        }

        private K getFirstKeyOrNull() {
            if (m.root == null) return null;
            K k = usesFromBound ? (fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey)) : (m.root == null ? null : m.firstKey());
            return (k != null && inRange(k)) ? k : null;
        }

        private K getLastKeyOrNull() {
            if (m.root == null) return null;
            K k = usesToBound ? (toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey)) : (m.root == null ? null : m.lastKey());
            return (k != null && inRange(k)) ? k : null;
        }

        public K firstKey() {
            K k = getFirstKeyOrNull();
            requireNonNull(k);
            return k;
        }

        @Override
        public K lastKey() {
            K k = getLastKeyOrNull();
            requireNonNull(k);
            return k;
        }

        public Entry<K, V> firstEntry() {
            K k = getFirstKeyOrNull();
            return k == null ? null : m.exportEntry(k);
        }

        public Entry<K, V> lastEntry() {
            K k = getLastKeyOrNull();
            return k == null ? null : m.exportEntry(k);
        }

        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> e = firstEntry();
            if (e != null)
                m.remove(e.getKey());
            return e;
        }
        public Entry<K, V> pollLastEntry() {
            Entry<K, V> e = lastEntry();
            if (e != null)
                m.remove(e.getKey()); return e;
        }

        public K lowerKey(K key) {
            K candidate;
            if (usesToBound && m.compare(key, toKey) > 0) {
                candidate = toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.lowerKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K floorKey(K key) {
            K candidate;
            if (usesToBound && m.compare(key, toKey) > 0) {
                candidate = toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.floorKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K ceilingKey(K key) {
            K candidate;
            if (usesFromBound && m.compare(key, fromKey) < 0) {
                candidate = fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.ceilingKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K higherKey(K key) {
            K candidate;
            if (usesFromBound && m.compare(key, fromKey) < 0) {
                candidate = fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.higherKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public Entry<K, V> lowerEntry(K key) {
            return m.exportEntry(lowerKey(key));
        }

        public Entry<K, V> floorEntry(K key) {
            return m.exportEntry(floorKey(key));
        }

        public Entry<K, V> ceilingEntry(K key) {
            return m.exportEntry(ceilingKey(key));
        }

        public Entry<K, V> higherEntry(K key) {
            return m.exportEntry(higherKey(key));
        }

        public NavigableMap<K, V> descendingMap() {
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        public NavigableSet<K> navigableKeySet() {
            return new KeySet<>(this);
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            int cmp = m.compare(fromKey, toKey);
            if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException("fromKey > toKey");
            if (isTooLow(fromKey, fromInclusive) || isTooHigh(toKey, toInclusive))
                throw new IllegalArgumentException("Key out of range");
            return new AscendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if (isTooHigh(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (isTooLow(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private Entry<K, V> nextEntry = firstEntry();
                        private Entry<K, V> lastReturned = null;

                        @Override public boolean hasNext() {
                            return nextEntry != null && inRange(nextEntry.getKey());
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!hasNext())
                                throw new NoSuchElementException();
                            lastReturned = nextEntry;
                            K nextKey = m.higherKey(nextEntry.getKey());
                            nextEntry = (nextKey != null && inRange(nextKey)) ? m.exportEntry(nextKey) : null;
                            return lastReturned;
                        }

                        @Override
                        public void remove() {
                            if (lastReturned == null)
                                throw new IllegalStateException();
                            m.remove(lastReturned.getKey());
                            lastReturned = null;
                        }
                    };
                }

                @Override
                public int size() {
                    int count = 0;
                    K currentKey = getFirstKeyOrNull();
                    while (currentKey != null && inRange(currentKey)) {
                        count++;
                        currentKey = m.higherKey(currentKey);
                    }
                    return count;
                }
            };
        }
    }

    private static class DescendingSubMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final CustomTreeMap<K, V> m;
        private final K fromKey;
        private final boolean fromInclusive;
        private final K toKey;
        private final boolean toInclusive;
        private final boolean usesFromBound;
        private final boolean usesToBound;

        DescendingSubMap(CustomTreeMap<K, V> map, boolean usesFromBound, K fromKey, boolean fromInclusive, boolean usesToBound, K toKey, boolean toInclusive) {
            this.m = map;
            this.usesFromBound = usesFromBound;
            this.fromKey = fromKey;
            this.fromInclusive = fromInclusive;
            this.usesToBound = usesToBound;
            this.toKey = toKey;
            this.toInclusive = toInclusive;
        }

        private boolean isTooLow(K key, boolean inclusive) {
            if (!usesToBound)
                return false;
            int c = m.compare(key, toKey);
            if (c < 0)
                return true;
            return c == 0 && (!toInclusive && inclusive);
        }

        private boolean isTooHigh(K key, boolean inclusive) {
            if (!usesFromBound)
                return false;
            int c = m.compare(key, fromKey);
            if (c > 0)
                return true;
            return c == 0 && (!fromInclusive && inclusive);
        }

        public boolean inRange(K key) {
            return !isTooLow(key, true) && !isTooHigh(key, true);
        }

        private boolean inRange(Object key) {
            return key != null && inRange((K) key);
        }

        public V get(Object key) {
            return inRange(key) ? m.get(key) : null;
        }

        public boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        public V put(K key, V value) {
            if (!inRange(key)) throw new IllegalArgumentException("Key out of range");
            return m.put(key, value);
        }

        public V remove(Object key) {
            return inRange(key) ? m.remove(key) : null;
        }

        private K getFirstKeyOrNull() {
            if (m.root == null) return null;
            K k = usesToBound ? (toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey)) : (m.root == null ? null : m.lastKey());
            return (k != null && inRange(k)) ? k : null;
        }

        private K getLastKeyOrNull() {
            if (m.root == null) return null;
            K k = usesFromBound ? (fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey)) : (m.root == null ? null : m.firstKey());
            return (k != null && inRange(k)) ? k : null;
        }

        public K firstKey() {
            K k = getFirstKeyOrNull();
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        public K lastKey() {
            K k = getLastKeyOrNull();
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        public Entry<K, V> firstEntry() {
            K k = getFirstKeyOrNull(); return k == null ? null : m.exportEntry(k);
        }

        public Entry<K, V> lastEntry() {
            K k = getLastKeyOrNull(); return k == null ? null : m.exportEntry(k);
        }
        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> e = firstEntry(); if (e != null) m.remove(e.getKey()); return e;
        }

        public Entry<K, V> pollLastEntry() {
            Entry<K, V> e = lastEntry(); if (e != null) m.remove(e.getKey()); return e;
        }

        public K lowerKey(K key) {
            K candidate;
            if (usesToBound && m.compare(key, toKey) > 0) {
                candidate = toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.higherKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K floorKey(K key) {
            K candidate;
            if (usesToBound && m.compare(key, toKey) > 0) {
                candidate = toInclusive ? m.floorKey(toKey) : m.lowerKey(toKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.ceilingKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K ceilingKey(K key) {
            K candidate;
            if (usesFromBound && m.compare(key, fromKey) < 0) {
                candidate = fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey);
                return inRange(candidate) ? candidate : null;
            }
            candidate = m.floorKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public K higherKey(K key) {
            K candidate;
            if (usesFromBound) {
                int cmp = m.compare(key, fromKey);
                if (cmp < 0 || (cmp == 0 && !fromInclusive)) {
                    candidate = fromInclusive ? m.ceilingKey(fromKey) : m.higherKey(fromKey);
                    return inRange(candidate) ? candidate : null;
                }
            }
            candidate = m.lowerKey(key);
            return inRange(candidate) ? candidate : null;
        }

        public Entry<K, V> lowerEntry(K key) {
            return m.exportEntry(lowerKey(key));
        }

        public Entry<K, V> floorEntry(K key) {
            return m.exportEntry(floorKey(key));
        }

        public Entry<K, V> ceilingEntry(K key) {
            return m.exportEntry(ceilingKey(key));
        }

        public Entry<K, V> higherEntry(K key) {
            return m.exportEntry(higherKey(key));
        }

        public NavigableMap<K, V> descendingMap() {
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        public NavigableSet<K> navigableKeySet() {
            return new KeySet<>(this);
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            int cmp = m.compare(fromKey, toKey);
            if (cmp < 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException("fromKey < toKey in descending map");

            if (isTooHigh(fromKey, fromInclusive) || isTooLow(toKey, toInclusive))
                throw new IllegalArgumentException("Key out of range");
            return new DescendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if (isTooLow(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (isTooHigh(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        public Comparator<? super K> comparator() {
            Comparator<? super K> comp = m.comparator();
            return (comp == null) ? Comparator.reverseOrder() : comp.reversed();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private Entry<K, V> nextEntry = firstEntry();
                        private Entry<K, V> lastReturned = null;

                        @Override public boolean hasNext() {
                            return nextEntry != null && inRange(nextEntry.getKey());
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!hasNext())
                                throw new NoSuchElementException();
                            lastReturned = nextEntry;
                            K nextKey = m.lowerKey(nextEntry.getKey());
                            nextEntry = (nextKey != null && inRange(nextKey)) ? m.exportEntry(nextKey) : null;
                            return lastReturned;
                        }

                        @Override
                        public void remove() {
                            if (lastReturned == null)
                                throw new IllegalStateException();
                            m.remove(lastReturned.getKey());
                            lastReturned = null;
                        }
                    };
                }

                @Override
                public int size() {
                    int count = 0;
                    K currentKey = getFirstKeyOrNull();
                    while (currentKey != null && inRange(currentKey)) {
                        count++;
                        currentKey = m.lowerKey(currentKey);
                    }
                    return count;
                }
            };
        }
    }

    private static class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> map;

        KeySet(NavigableMap<E, ?> map) {
            this.map = map;
        }

        public E lower(E item) {
            return map.lowerKey(item);
        }

        public E floor(E item) {
            return map.floorKey(item);
        }

        public E ceiling(E item) {
            return map.ceilingKey(item);
        }

        public E higher(E item) {
            return map.higherKey(item);
        }

        public E pollFirst() {
            Map.Entry<E, ?> e = map.pollFirstEntry();
            return e == null ? null : e.getKey();
        }

        public E pollLast() {
            Map.Entry<E, ?> e = map.pollLastEntry();
            return e == null ? null : e.getKey();
        }

        public Iterator<E> iterator() {
            Iterator<? extends Map.Entry<E, ?>> entryIterator = map.entrySet().iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return entryIterator.hasNext();
                }

                @Override
                public E next() {
                    return entryIterator.next().getKey();
                }

                @Override
                public void remove() {
                    entryIterator.remove();
                }
            };
        }

        public NavigableSet<E> descendingSet() {
            return map.descendingMap().navigableKeySet();
        }

        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return map.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return map.headMap(toElement, inclusive).navigableKeySet();
        }

        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return map.tailMap(fromElement, inclusive).navigableKeySet();
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }

        public int size() {
            return map.size();
        }

        public Comparator<? super E> comparator() {
            return map.comparator();
        }

        public E first() {
            return map.firstKey();
        }

        public E last() {
            return map.lastKey();
        }

        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        public void clear() {
            map.clear();
        }
    }
}