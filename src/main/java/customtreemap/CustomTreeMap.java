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

    public CustomTreeMap(final Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public CustomTreeMap(final Map<? extends K, ? extends V> m) {
        this.comparator = null;
        putAll(m);
    }

    public CustomTreeMap(final SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        putAll(m);
    }

    public Entry<K, V> ceilingEntry(final K key) {
        requireNonNull(key);
        return exportEntry(ceilingKey(key));
    }

    public K ceilingKey(final K key) {
        requireNonNull(key);
        Node<K, V> p = getCeilingNode(key, true);
        return p == null ? null : p.key;
    }

    public void clear() {
        size = 0;
        root = null;
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    public boolean containsKey(final Object key) {
        requireNonNull(key);
        return getNode(key) != null;
    }

    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    public NavigableMap<K, V> descendingMap() {
        return new DescendingSubMap<>(this, false, null, false, false, null, false);
    }

    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
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
            public void clear() {
                CustomTreeMap.this.clear();
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    public Entry<K, V> firstEntry() {
        Node<K, V> p = getFirstNode();
        return p == null ? null : exportEntry(p.key);
    }

    public K firstKey() {
        Node<K, V> p = getFirstNode();
        if (p == null)
            throw new NoSuchElementException();
        return p.key;
    }

    public Entry<K, V> floorEntry(final K key) {
        requireNonNull(key);
        return exportEntry(floorKey(key));
    }

    public K floorKey(final K key) {
        requireNonNull(key);
        Node<K, V> p = getFloorNode(key, true);
        return p == null ? null : p.key;
    }

    public V get(final Object key) {
        requireNonNull(key);
        Node<K, V> p = getNode(key);
        return p == null ? null : p.value;
    }

    public SortedMap<K, V> headMap(final K toKey) {
        return headMap(toKey, false);
    }

    public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        requireNonNull(toKey);
        return new AscendingSubMap<>(this, false, null, false, true, toKey, inclusive);
    }

    public Entry<K, V> higherEntry(final K key) {
        requireNonNull(key);
        return exportEntry(higherKey(key));
    }

    public K higherKey(final K key) {
        requireNonNull(key);
        Node<K, V> p = getCeilingNode(key, false);
        return p == null ? null : p.key;
    }

    public Entry<K, V> lastEntry() {
        Node<K, V> p = getLastNode();
        return p == null ? null : exportEntry(p.key);
    }

    public K lastKey() {
        Node<K, V> p = getLastNode();
        if (p == null)
            throw new NoSuchElementException();
        return p.key;
    }

    public Entry<K, V> lowerEntry(final K key) {
        requireNonNull(key);
        return exportEntry(lowerKey(key));
    }

    public K lowerKey(final K key) {
        requireNonNull(key);
        Node<K, V> p = getFloorNode(key, false);
        return p == null ? null : p.key;
    }

    public NavigableSet<K> navigableKeySet() {
        return new KeySet<>(this);
    }

    public Entry<K, V> pollFirstEntry() {
        Node<K, V> p = getFirstNode();
        if (p == null)
            return null;
        Entry<K, V> e = exportEntry(p.key);
        deleteNode(p);
        return e;
    }

    public Entry<K, V> pollLastEntry() {
        Node<K, V> p = getLastNode();
        if (p == null)
            return null;
        Entry<K, V> e = exportEntry(p.key);
        deleteNode(p);
        return e;
    }

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

    public V remove(final Object key) {
        requireNonNull(key);
        Node<K, V> p = getNode(key);
        if (p == null)
            return null;
        V oldValue = p.value;
        deleteNode(p);
        return oldValue;
    }

    public int size() {
        return size;
    }

    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        requireNonNull(fromKey);
        requireNonNull(toKey);
        int cmp = compare(fromKey, toKey);
        if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
            throw new IllegalArgumentException("fromKey > toKey");
        return new AscendingSubMap<>(this, true, fromKey, fromInclusive, true, toKey, toInclusive);
    }

    public SortedMap<K, V> tailMap(final K fromKey) {
        return tailMap(fromKey, true);
    }

    public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        requireNonNull(fromKey);
        return new AscendingSubMap<>(this, true, fromKey, inclusive, false, null, false);
    }

    private static abstract class NavigableSubMap<K extends Comparable<K>, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        final CustomTreeMap<K, V> m;
        final K fromKey;
        final boolean fromInclusive;
        final K toKey;
        final boolean toInclusive;
        final boolean usesFromBound;
        final boolean usesToBound;

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

        abstract boolean isTooLow(final K key, final boolean inclusive);
        abstract boolean isTooHigh(final K key, final boolean inclusive);
        abstract Node<K, V> subMapNode(final boolean isFirst);
        abstract Node<K, V> stepNode(final Node<K, V> node);

        abstract K absLower(K key);
        abstract K absFloor(K key);
        abstract K absCeiling(K key);
        abstract K absHigher(K key);

        final boolean inRange(final K key) {
            return !isTooLow(key, true) && !isTooHigh(key, true);
        }

        final boolean inRange(final K key, final boolean inclusive) {
            return isTooLow(key, inclusive) || isTooHigh(key, inclusive);
        }

        final boolean inRange(final Object key) {
            return key != null && inRange((K) key);
        }

        @Override
        public V get(final Object key) {
            return inRange(key) ? m.get(key) : null;
        }

        @Override
        public boolean containsKey(final Object key) {
            return inRange(key) && m.containsKey(key);
        }

        @Override
        public V put(final K key, final V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("Key out of range");
            return m.put(key, value);
        }

        @Override
        public V remove(final Object key) {
            return inRange(key) ? m.remove(key) : null;
        }

        @Override
        public void clear() {
            Iterator<Entry<K, V>> it = entrySet().iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        private K getSubMapKey(boolean isFirst) {
            Node<K, V> n = subMapNode(isFirst);
            return (n != null && inRange(n.key)) ? n.key : null;
        }

        public K firstKey() {
            K k = getSubMapKey(true);
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        public K lastKey() {
            K k = getSubMapKey(false);
            if (k == null)
                throw new NoSuchElementException();
            return k;
        }

        public Entry<K, V> firstEntry() {
            K k = getSubMapKey(true);
            return k == null ? null : m.exportEntry(k);
        }

        public Entry<K, V> lastEntry() {
            K k = getSubMapKey(false);
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
                m.remove(e.getKey());
            return e;
        }

        public K lowerKey(final K key) {
            requireNonNull(key);
            K candidate = isTooHigh(key, false) ? getSubMapKey(false) : absLower(key);
            return inRange(candidate) ? candidate : null;
        }

        public K floorKey(final K key) {
            requireNonNull(key);
            K candidate = isTooHigh(key, true) ? getSubMapKey(false) : absFloor(key);
            return inRange(candidate) ? candidate : null;
        }

        public K ceilingKey(final K key) {
            requireNonNull(key);
            K candidate = isTooLow(key, true) ? getSubMapKey(true) : absCeiling(key);
            return inRange(candidate) ? candidate : null;
        }

        public K higherKey(final K key) {
            requireNonNull(key);
            K candidate = isTooLow(key, false) ? getSubMapKey(true) : absHigher(key);
            return inRange(candidate) ? candidate : null;
        }

        public Entry<K, V> lowerEntry(final K key) {
            return m.exportEntry(lowerKey(key));
        }

        public Entry<K, V> floorEntry(final K key) {
            return m.exportEntry(floorKey(key));
        }

        public Entry<K, V> ceilingEntry(final K key) {
            return m.exportEntry(ceilingKey(key));
        }

        public Entry<K, V> higherEntry(final K key) {
            return m.exportEntry(higherKey(key));
        }

        public NavigableSet<K> navigableKeySet() {
            return new KeySet<>(this);
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<K, V> headMap(final K toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<K, V> tailMap(final K fromKey) {
            return tailMap(fromKey, true);
        }

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
                            if (!hasNext()) throw new NoSuchElementException();
                            lastReturned = nextNode;
                            nextNode = stepNode(nextNode);
                            return m.exportEntry(lastReturned.key);
                        }

                        @Override
                        public void remove() {
                            if (lastReturned == null) throw new IllegalStateException();
                            m.deleteNode(lastReturned);
                            lastReturned = null;
                        }
                    };
                }

                @Override
                public int size() {
                    int count = 0;
                    for (Node<K, V> n = subMapNode(true); n != null && inRange(n.key); n = stepNode(n)) {
                        count++;
                    }
                    return count;
                }
            };
        }
    }

    private static class AscendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        AscendingSubMap(final CustomTreeMap<K, V> map, final boolean usesFromBound, final K fromKey,
                        final boolean fromInclusive, final boolean usesToBound, final K toKey,
                        final boolean toInclusive) {
            super(map, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        @Override
        boolean isTooLow(final K key, final boolean inclusive) {
            if (!usesFromBound) return false;
            int c = m.compare(key, fromKey);
            return c < 0 || (c == 0 && (!fromInclusive && inclusive));
        }

        @Override
        boolean isTooHigh(final K key, final boolean inclusive) {
            if (!usesToBound) return false;
            int c = m.compare(key, toKey);
            return c > 0 || (c == 0 && (!toInclusive && inclusive));
        }

        @Override
        Node<K, V> subMapNode(boolean isFirst) {
            if (m.root == null) return null;
            return isFirst ? (usesFromBound ? m.getCeilingNode(fromKey, fromInclusive) : m.getFirstNode())
                    : (usesToBound ? m.getFloorNode(toKey, toInclusive) : m.getLastNode());
        }

        @Override
        Node<K, V> stepNode(Node<K, V> node) {
            return m.successor(node);
        }

        @Override K absLower(K key) { return m.lowerKey(key); }
        @Override K absFloor(K key) { return m.floorKey(key); }
        @Override K absCeiling(K key) { return m.ceilingKey(key); }
        @Override K absHigher(K key) { return m.higherKey(key); }

        public NavigableMap<K, V> descendingMap() {
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (inRange(fromKey, fromInclusive) || inRange(toKey, toInclusive))
                throw new IllegalArgumentException();
            int cmp = m.compare(fromKey, toKey);
            if (cmp > 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException("fromKey > toKey");
            return new AscendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (inRange(toKey, inclusive) && isTooHigh(toKey, inclusive))
                throw new IllegalArgumentException();
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (inRange(fromKey, inclusive) && isTooLow(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }

        public Comparator<? super K> comparator() {
            return m.comparator();
        }
    }

    private static class DescendingSubMap<K extends Comparable<K>, V> extends NavigableSubMap<K, V> {

        DescendingSubMap(final CustomTreeMap<K, V> map, final boolean usesFromBound, final K fromKey,
                         final boolean fromInclusive, final boolean usesToBound, final K toKey, final boolean toInclusive) {
            super(map, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        @Override
        boolean isTooLow(final K key, final boolean inclusive) {
            if (!usesToBound) return false;
            int c = m.compare(key, toKey);
            return c < 0 || (c == 0 && (!toInclusive && inclusive));
        }

        @Override
        boolean isTooHigh(final K key, final boolean inclusive) {
            if (!usesFromBound) return false;
            int c = m.compare(key, fromKey);
            return c > 0 || (c == 0 && (!fromInclusive && inclusive));
        }

        @Override
        Node<K, V> subMapNode(boolean isFirst) {
            if (m.root == null) return null;
            return isFirst ? (usesToBound ? m.getFloorNode(toKey, toInclusive) : m.getLastNode())
                    : (usesFromBound ? m.getCeilingNode(fromKey, fromInclusive) : m.getFirstNode());
        }

        @Override
        Node<K, V> stepNode(Node<K, V> node) {
            return m.predecessor(node);
        }

        @Override K absLower(K key) { return m.higherKey(key); }
        @Override K absFloor(K key) { return m.ceilingKey(key); }
        @Override K absCeiling(K key) { return m.floorKey(key); }
        @Override K absHigher(K key) { return m.lowerKey(key); }

        public NavigableMap<K, V> descendingMap() {
            return new AscendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, usesToBound, toKey, toInclusive);
        }

        public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
            requireNonNull(fromKey);
            requireNonNull(toKey);
            if (inRange(fromKey, fromInclusive) || inRange(toKey, toInclusive))
                throw new IllegalArgumentException();
            int cmp = m.compare(fromKey, toKey);
            if (cmp < 0 || (cmp == 0 && (!fromInclusive || !toInclusive)))
                throw new IllegalArgumentException();
            return new DescendingSubMap<>(m, true, fromKey, fromInclusive, true, toKey, toInclusive);
        }

        public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
            requireNonNull(toKey);
            if (inRange(toKey, inclusive) && isTooLow(toKey, inclusive))
                throw new IllegalArgumentException();
            return new DescendingSubMap<>(m, usesFromBound, fromKey, fromInclusive, true, toKey, inclusive);
        }

        public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
            requireNonNull(fromKey);
            if (inRange(fromKey, inclusive) && isTooHigh(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m, true, fromKey, inclusive, usesToBound, toKey, toInclusive);
        }

        public Comparator<? super K> comparator() {
            Comparator<? super K> comp = m.comparator();
            return (comp == null) ? Comparator.reverseOrder() : comp.reversed();
        }
    }

    private static class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> map;

        KeySet(NavigableMap<E, ?> map) {
            this.map = map;
        }

        public E ceiling(final E item) {
            return map.ceilingKey(item);
        }

        public E floor(final E item) {
            return map.floorKey(item);
        }

        public E lower(final E item) {
            return map.lowerKey(item);
        }

        public E higher(final E item) {
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

        public NavigableSet<E> descendingSet() {
            return map.descendingMap().navigableKeySet();
        }

        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
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

    private static class Node<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> parent;
        boolean color = BLACK;

        Node(final K key, V value, final Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    private static <K, V> boolean colorOf(final Node<K, V> p) {
        return p == null ? BLACK : p.color;
    }

    private int compare(final Object k1, final Object k2) {
        if (comparator != null)
            return comparator.compare((K) k1, (K) k2);
        return ((Comparable<? super K>) k1).compareTo((K) k2);
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

    private Map.Entry<K, V> exportEntry(final K key) {
        if (key == null)
            return null;
        Node<K, V> node = getNode(key);
        return node == null ? null : new SimpleImmutableEntry<>(node.key, node.value);
    }

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

    private Node<K, V> getCeilingNode(final K key, final boolean inclusive) {
        Node<K, V> p = root;
        Node<K, V> candidate = null;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                candidate = p;
                p = p.left;
            } else {
                if (cmp <= 0)
                    if (inclusive)
                        return p;
                p = p.right;
            }
        }
        return candidate;
    }

    private Node<K, V> getFirstNode() {
        Node<K, V> p = root;
        if (p != null)
            while (p.left != null)
                p = p.left;
        return p;
    }

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

    private Node<K, V> getLastNode() {
        Node<K, V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

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

    private static <K, V> Node<K, V> leftOf(final Node<K, V> p) {
        return p == null ? null : p.left;
    }

    private static <K, V> Node<K, V> parentOf(final Node<K, V> p) {
        return p == null ? null : p.parent;
    }

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

    private static <K, V> Node<K, V> rightOf(final Node<K, V> p) {
        return p == null ? null : p.right;
    }

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

    private static <K, V> void setColor(final Node<K, V> p, boolean c) {
        if (p != null) p.color = c;
    }


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
}