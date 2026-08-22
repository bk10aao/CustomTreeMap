package customtreemap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CustomTreeMap Test Suite")
class CustomTreeMapTest {

    private CustomTreeMap<Integer, String> map;
    private NavigableSet<Integer> keySet;

    @BeforeEach
    void setUp() {
        map = new CustomTreeMap<>();
        keySet = map.navigableKeySet();
    }

    @Test
    @DisplayName("New tree should be empty and valid")
    void testNewTree() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(42));
    }

    @Test
    @DisplayName("Put single entry updates size and properties")
    void testPutSingle() {
        assertNull(map.put(10, "Ten"));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals("Ten", map.get(10));
    }

    @Test
    @DisplayName("Put existing key updates value and returns old value")
    void testPutOverwrite() {
        map.put(10, "Ten");
        assertEquals("Ten", map.put(10, "Ten Updated"));
        assertEquals(1, map.size());
        assertEquals("Ten Updated", map.get(10));
    }

    @Test
    @DisplayName("Remove non-existent key returns null")
    void testRemoveNonExistent() {
        map.put(10, "Ten");
        assertNull(map.remove(99));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("Remove single root entry empties tree")
    void testRemoveSingleRoot() {
        map.put(10, "Ten");
        assertEquals("Ten", map.remove(10));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(10));
    }

    @Test
    @DisplayName("Sequential insertion (1..100) balances correctly")
    void testAscendingInsertion() {
        for (int i = 1; i <= 100; i++) {
            map.put(i, "Val" + i);
        }
        assertEquals(100, map.size());
        for (int i = 1; i <= 100; i++) {
            assertEquals("Val" + i, map.get(i));
        }
    }

    @Test
    @DisplayName("Descending insertion (100..1) balances correctly")
    void testDescendingInsertion() {
        for (int i = 100; i >= 1; i--) {
            map.put(i, "Val" + i);
        }
        assertEquals(100, map.size());
        for (int i = 1; i <= 100; i++) {
            assertEquals("Val" + i, map.get(i));
        }
    }

    @Test
    @DisplayName("Sequential removal (1..100) maintains invariants")
    void testSequentialRemoval() {
        for (int i = 1; i <= 100; i++) {
            map.put(i, "Val" + i);
        }

        for (int i = 1; i <= 100; i++) {
            assertEquals("Val" + i, map.remove(i));
        }
        assertTrue(map.isEmpty());
    }

    @ParameterizedTest(name = "Random Seed Run #{index} - Seed: {0}")
    @ValueSource(longs = {42L, 1337L, 9999L, 20260820L})
    @DisplayName("Randomized Insertions and Deletions against java.util.TreeMap")
    void testRandomizedOperationsAgainstTreeMap(long seed) {
        Random random = new Random(seed);
        TreeMap<Integer, String> referenceMap = new TreeMap<>();

        int operationCount = 1000;
        int keyRange = 250;

        for (int i = 0; i < operationCount; i++) {
            int key = random.nextInt(keyRange);
            int op = random.nextInt(10);

            if (op < 6) {
                String val = "Val-" + key + "-" + i;
                String refPrev = referenceMap.put(key, val);
                String actualPrev = map.put(key, val);

                assertEquals(refPrev, actualPrev, "Mismatch in put return value for key: " + key);
            } else if (op < 9) {
                String refRemoved = referenceMap.remove(key);
                String actualRemoved = map.remove(key);
                assertEquals(refRemoved, actualRemoved, "Mismatch in remove return value for key: " + key);
            } else {
                assertEquals(referenceMap.get(key), map.get(key), "Mismatch in get for key: " + key);
            }
            assertEquals(referenceMap.size(), map.size(), "Size mismatch after op " + i);
        }
        for (Integer key : referenceMap.keySet()) {
            assertEquals(referenceMap.get(key), map.get(key));
        }
    }

    @Test
    @DisplayName("Random shuffling insert followed by random shuffling delete")
    void testRandomShuffledLifecycle() {
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            keys.add(i);
        }
        Collections.shuffle(keys, new Random(777));
        for (int key : keys) {
            map.put(key, "Value" + key);
        }
        assertEquals(500, map.size());
        Collections.shuffle(keys, new Random(888));
        for (int key : keys) {
            assertEquals("Value" + key, map.remove(key));
        }
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Returns true for key present at root")
    void returnsTrueForRootKey() {
        map.put(10, "Ten");

        assertThat(map.containsKey(10)).isTrue();
    }

    @Test
    @DisplayName("Returns true for keys located in left and right subtrees")
    void returnsTrueForSubtreeKeys() {
        map.put(20, "Twenty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(5, "Five");
        map.put(15, "Fifteen");

        assertThat(map.containsKey(20)).isTrue();
        assertThat(map.containsKey(10)).isTrue();
        assertThat(map.containsKey(30)).isTrue();
        assertThat(map.containsKey(5)).isTrue();
        assertThat(map.containsKey(15)).isTrue();
    }

    @Test
    @DisplayName("Returns false for key not present in non-empty tree")
    void returnsFalseForMissingKey() {
        map.put(20, "Twenty");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        assertThat(map.containsKey(5)).isFalse();
        assertThat(map.containsKey(25)).isFalse();
        assertThat(map.containsKey(100)).isFalse();
    }

    @Test
    @DisplayName("Returns true when key value is updated without duplicate insertion")
    void returnsTrueAfterValueUpdate() {
        map.put(10, "Ten");
        map.put(10, "Updated Ten");

        assertThat(map.containsKey(10)).isTrue();
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Throws NullPointerException when passing null key")
    void throwsExceptionForNullKey() {
        assertThrows(NullPointerException.class, () -> map.containsKey(null));
    }

    @Test
    @DisplayName("Returns false when tree is empty")
    void returnsFalseWhenEmpty() {
        assertFalse(map.containsValue("Ten"));
    }

    @Test
    @DisplayName("Returns true when value is present at root")
    void returnsTrueForRootValue() {
        map.put(10, "Ten");

        assertTrue(map.containsValue("Ten"));
    }

    @Test
    @DisplayName("Returns true for values located across subtrees")
    void returnsTrueForSubtreeValues() {
        map.put(20, "Twenty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(5, "Five");
        map.put(15, "Fifteen");

        assertTrue(map.containsValue("Twenty"));
        assertTrue(map.containsValue("Ten"));
        assertTrue(map.containsValue("Thirty"));
        assertTrue(map.containsValue("Five"));
        assertTrue(map.containsValue("Fifteen"));
    }

    @Test
    @DisplayName("Returns false for value not present in tree")
    void returnsFalseForMissingValue() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertFalse(map.containsValue("Hundred"));
    }

    @Test
    @DisplayName("Handles null values correctly")
    void handlesNullValueSearch() {
        map.put(10, "Ten");
        map.put(20, null);

        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("Twenty"));
    }

    @Test
    @DisplayName("Returns false after entry containing value is removed")
    void returnsFalseAfterRemoval() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertTrue(map.containsValue("Ten"));

        map.remove(10);

        assertFalse(map.containsValue("Ten"));
        assertTrue(map.containsValue("Twenty"));
    }

    @Test
    @DisplayName("Handles overwritten values properly")
    void handlesOverwrittenValues() {
        map.put(10, "Old Value");
        map.put(10, "New Value");

        assertFalse(map.containsValue("Old Value"));
        assertTrue(map.containsValue("New Value"));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("Returns true when multiple keys hold the exact same value")
    void returnsTrueForDuplicateValues() {
        map.put(10, "Shared");
        map.put(20, "Shared");

        assertTrue(map.containsValue("Shared"));

        map.remove(10);
        assertTrue(map.containsValue("Shared"));
    }

    @Test
    @DisplayName("Clearing an already empty map keeps it empty")
    void clearEmptyMap() {
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Clearing a non-empty map resets size to 0 and makes it empty")
    void clearNonEmptyMapResetsSizeAndEmptiness() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(5, "Five");

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Keys and values are no longer accessible after clear")
    void clearRemovesAllLookups() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        map.clear();

        assertNull(map.get(10));
        assertNull(map.get(20));
        assertNull(map.get(30));

        assertFalse(map.containsKey(10));
        assertFalse(map.containsValue("Twenty"));
    }

    @Test
    @DisplayName("Map can be populated and cleared multiple times sequentially")
    void multipleClearCycles() {
        map.put(1, "One");
        map.put(2, "Two");
        map.clear();

        assertTrue(map.isEmpty());

        map.put(100, "Hundred");
        assertEquals(1, map.size());
        assertEquals("Hundred", map.get(100));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    @DisplayName("Removing from a cleared map returns null without throwing exceptions")
    void removeFromClearedMap() {
        map.put(10, "Ten");
        map.clear();

        assertNull(map.remove(10));
        assertEquals(0, map.size());
    }

    @Test
    @DisplayName("Throws NoSuchElementException when map is empty")
    void throwsExceptionWhenEmpty() {
        assertThrows(NoSuchElementException.class, () -> map.firstKey());
    }

    @Test
    @DisplayName("Returns the only key when map contains single element")
    void returnsKeyForSingleElement() {
        map.put(42, "Forty-Two");

        assertEquals(42, map.firstKey());
    }

    @Test
    @DisplayName("Returns minimum key in multi-element tree")
    void returnsMinimumKey() {
        map.put(50, "Fifty");
        map.put(20, "Twenty");
        map.put(80, "Eighty");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        assertEquals(10, map.firstKey());
    }

    @Test
    @DisplayName("Updates firstKey when a smaller key is inserted")
    void updatesWhenSmallerKeyInserted() {
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        assertEquals(20, map.firstKey());

        map.put(5, "Five");
        assertEquals(5, map.firstKey());

        map.put(1, "One");
        assertEquals(1, map.firstKey());
    }

    @Test
    @DisplayName("Updates firstKey after removing current minimum key")
    void updatesAfterRemovingMinimum() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.firstKey());

        map.remove(10);
        assertEquals(20, map.firstKey());

        map.remove(20);
        assertEquals(30, map.firstKey());
    }

    @Test
    @DisplayName("Throws NoSuchElementException after clearing non-empty map")
    void throwsExceptionAfterClear() {
        map.put(10, "Ten");
        map.put(5, "Five");

        map.clear();

        assertThrows(NoSuchElementException.class, () -> map.firstKey());
    }

    @Test
    @DisplayName("Works correctly with negative and boundary keys")
    void handlesNegativeKeys() {
        map.put(0, "Zero");
        map.put(100, "One Hundred");
        map.put(-50, "Minus Fifty");
        map.put(-100, "Minus One Hundred");

        assertEquals(-100, map.firstKey());
    }

    @Test
    @DisplayName("Returns maximum key in multi-element tree")
    void returnsMaximumKey() {
        map.put(50, "Fifty");
        map.put(20, "Twenty");
        map.put(80, "Eighty");
        map.put(10, "Ten");
        map.put(95, "Ninety-Five");

        assertEquals(95, map.lastKey());
    }

    @Test
    @DisplayName("Updates lastKey when a larger key is inserted")
    void updatesWhenLargerKeyInserted() {
        map.put(20, "Twenty");
        map.put(10, "Ten");
        assertEquals(20, map.lastKey());

        map.put(50, "Fifty");
        assertEquals(50, map.lastKey());

        map.put(100, "One Hundred");
        assertEquals(100, map.lastKey());
    }

    @Test
    @DisplayName("Updates lastKey after removing current maximum key")
    void updatesAfterRemovingMaximum() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(30, map.lastKey());

        map.remove(30);
        assertEquals(20, map.lastKey());

        map.remove(20);
        assertEquals(10, map.lastKey());
    }

    @Test
    @DisplayName("lastKey throws NoSuchElementException directly when root is null on fresh instance")
    void lastKeyThrowsWhenRootIsNullFreshInstance() {
        assertTrue(map.isEmpty());
        assertThrows(NoSuchElementException.class, () -> map.lastKey());
    }

    @Test
    @DisplayName("Returns null when map is empty")
    void returnsNullWhenEmpty() {
        assertNull(map.floorKey(10));
    }

    @Test
    @DisplayName("Returns exact key when target key exists in the map")
    void returnsExactKeyMatch() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.floorKey(10));
        assertEquals(20, map.floorKey(20));
        assertEquals(30, map.floorKey(30));
    }

    @Test
    @DisplayName("Returns greatest key strictly smaller than target key when exact match is absent")
    void returnsFloorKeyWhenExactMatchAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.floorKey(15));
        assertEquals(20, map.floorKey(25));
        assertEquals(30, map.floorKey(100));
    }

    @Test
    @DisplayName("Returns null when target key is smaller than the minimum key in the map")
    void returnsNullWhenTargetIsSmallerThanMin() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertNull(map.floorKey(5));
        assertNull(map.floorKey(0));
    }

    @Test
    @DisplayName("Navigates complex subtrees correctly to find the floor key")
    void navigatesComplexTree() {
        map.put(40, "Forty");
        map.put(20, "Twenty");
        map.put(60, "Sixty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(50, "Fifty");

        assertEquals(30, map.floorKey(35));
        assertEquals(30, map.floorKey(39));
        assertEquals(50, map.floorKey(59));
        assertEquals(60, map.floorKey(60));
    }

    @Test
    @DisplayName("Handles negative and zero keys properly")
    void handlesNegativeAndZeroKeys() {
        map.put(-50, "Minus Fifty");
        map.put(-10, "Minus Ten");
        map.put(0, "Zero");
        map.put(25, "Twenty-Five");

        assertEquals(-50, map.floorKey(-20));
        assertEquals(-10, map.floorKey(-5));
        assertEquals(0, map.floorKey(12));
        assertNull(map.floorKey(-100));
    }

    @Test
    @DisplayName("Updates floorKey results after key removals")
    void updatesAfterRemoval() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(20, map.floorKey(25));

        map.remove(20);

        assertEquals(10, map.floorKey(25));
    }

    @Test
    @DisplayName("Throws null pointer exception on floorKey with value of null")
    void onGettingFloorKey_withValue_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> map.floorKey(null));
    }

    @Test
    @DisplayName("ceilingKey returns null when map is empty")
    void ceilingKey_returnsNullWhenEmpty() {
        assertNull(map.ceilingKey(10));
    }

    @Test
    @DisplayName("ceilingKey returns exact key when target key exists in the map")
    void ceilingKey_returnsExactKeyMatch() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.ceilingKey(10));
        assertEquals(20, map.ceilingKey(20));
        assertEquals(30, map.ceilingKey(30));
    }

    @Test
    @DisplayName("ceilingKey returns smallest key strictly greater than target key when exact match is absent")
    void ceilingKey_returnsCeilingKeyWhenExactMatchAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.ceilingKey(5));
        assertEquals(20, map.ceilingKey(15));
        assertEquals(30, map.ceilingKey(25));
    }

    @Test
    @DisplayName("ceilingKey returns null when target key is greater than the maximum key in the map")
    void ceilingKey_returnsNullWhenTargetIsGreaterThanMax() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertNull(map.ceilingKey(25));
        assertNull(map.ceilingKey(100));
    }

    @Test
    @DisplayName("ceilingKey navigates complex subtrees correctly to find the ceiling key")
    void ceilingKey_navigatesComplexTree() {
        map.put(40, "Forty");
        map.put(20, "Twenty");
        map.put(60, "Sixty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(50, "Fifty");

        assertEquals(40, map.ceilingKey(35));
        assertEquals(50, map.ceilingKey(41));
        assertEquals(60, map.ceilingKey(55));
        assertEquals(10, map.ceilingKey(1));
    }

    @Test
    @DisplayName("ceilingKey handles negative and zero keys properly")
    void ceilingKey_handlesNegativeAndZeroKeys() {
        map.put(-50, "Minus Fifty");
        map.put(-10, "Minus Ten");
        map.put(0, "Zero");
        map.put(25, "Twenty-Five");

        assertEquals(-10, map.ceilingKey(-20));
        assertEquals(0, map.ceilingKey(-5));
        assertEquals(25, map.ceilingKey(1));
        assertNull(map.ceilingKey(50));
    }

    @Test
    @DisplayName("ceilingKey updates results after key removals")
    void ceilingKey_updatesAfterRemoval() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(20, map.ceilingKey(15));

        map.remove(20);

        assertEquals(30, map.ceilingKey(15));
    }

    @Test
    @DisplayName("ceilingKey throws NullPointerException when passing a null key")
    void ceilingKey_throwsExceptionForNullKey() {
        map.put(10, "Ten");

        assertThrows(NullPointerException.class, () -> map.ceilingKey(null));
    }

    @Test
    @DisplayName("lowerKey returns null when map is empty")
    void lowerKey_returnsNullWhenEmpty() {
        assertNull(map.lowerKey(10));
    }

    @Test
    @DisplayName("lowerKey returns strictly smaller key when exact key exists in the map")
    void lowerKey_returnsStrictlySmallerKeyWhenExactMatchExists() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertNull(map.lowerKey(10));
        assertEquals(10, map.lowerKey(20));
        assertEquals(20, map.lowerKey(30));
    }

    @Test
    @DisplayName("lowerKey returns largest key strictly smaller than target when target is absent")
    void lowerKey_returnsLowerKeyWhenExactMatchAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertNull(map.lowerKey(5));
        assertEquals(10, map.lowerKey(15));
        assertEquals(20, map.lowerKey(25));
        assertEquals(30, map.lowerKey(35));
    }

    @Test
    @DisplayName("lowerKey navigates complex subtrees correctly to find lower key")
    void lowerKey_navigatesComplexTree() {
        map.put(40, "Forty");
        map.put(20, "Twenty");
        map.put(60, "Sixty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(50, "Fifty");

        assertEquals(30, map.lowerKey(40));
        assertEquals(40, map.lowerKey(50));
        assertEquals(50, map.lowerKey(60));
        assertEquals(60, map.lowerKey(100));
    }

    @Test
    @DisplayName("lowerKey handles negative and zero keys properly")
    void lowerKey_handlesNegativeAndZeroKeys() {
        map.put(-50, "Minus Fifty");
        map.put(-10, "Minus Ten");
        map.put(0, "Zero");
        map.put(25, "Twenty-Five");

        assertNull(map.lowerKey(-50));
        assertEquals(-50, map.lowerKey(-10));
        assertEquals(-10, map.lowerKey(0));
        assertEquals(0, map.lowerKey(25));
    }

    @Test
    @DisplayName("lowerKey updates results after key removals")
    void lowerKey_updatesAfterRemoval() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(20, map.lowerKey(30));

        map.remove(20);

        assertEquals(10, map.lowerKey(30));
    }

    @Test
    @DisplayName("lowerKey throws NullPointerException when passing a null key")
    void lowerKey_throwsExceptionForNullKey() {
        map.put(10, "Ten");

        assertThrows(NullPointerException.class, () -> map.lowerKey(null));
    }

    @Test
    @DisplayName("higherKey returns null when map is empty")
    void higherKey_returnsNullWhenEmpty() {
        assertNull(map.higherKey(10));
    }

    @Test
    @DisplayName("higherKey returns strictly greater key when exact key exists in the map")
    void higherKey_returnsStrictlyGreaterKeyWhenExactMatchExists() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(20, map.higherKey(10));
        assertEquals(30, map.higherKey(20));
        assertNull(map.higherKey(30));
    }

    @Test
    @DisplayName("higherKey returns smallest key strictly greater than target when target is absent")
    void higherKey_returnsHigherKeyWhenExactMatchAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.higherKey(5));
        assertEquals(20, map.higherKey(15));
        assertEquals(30, map.higherKey(25));
        assertNull(map.higherKey(35));
    }

    @Test
    @DisplayName("higherKey navigates complex subtrees correctly to find higher key")
    void higherKey_navigatesComplexTree() {
        map.put(40, "Forty");
        map.put(20, "Twenty");
        map.put(60, "Sixty");
        map.put(10, "Ten");
        map.put(30, "Thirty");
        map.put(50, "Fifty");

        assertEquals(50, map.higherKey(40));
        assertEquals(60, map.higherKey(50));
        assertEquals(20, map.higherKey(10));
        assertEquals(40, map.higherKey(30));
    }

    @Test
    @DisplayName("higherKey handles negative and zero keys properly")
    void higherKey_handlesNegativeAndZeroKeys() {
        map.put(-50, "Minus Fifty");
        map.put(-10, "Minus Ten");
        map.put(0, "Zero");
        map.put(25, "Twenty-Five");

        assertEquals(-10, map.higherKey(-50));
        assertEquals(0, map.higherKey(-10));
        assertEquals(25, map.higherKey(0));
        assertNull(map.higherKey(25));
    }

    @Test
    @DisplayName("higherKey updates results after key removals")
    void higherKey_updatesAfterRemoval() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(20, map.higherKey(10));

        map.remove(20);

        assertEquals(30, map.higherKey(10));
    }

    @Test
    @DisplayName("higherKey throws NullPointerException when passing a null key")
    void higherKey_throwsExceptionForNullKey() {
        map.put(10, "Ten");

        assertThrows(NullPointerException.class, () -> map.higherKey(null));
    }

    @Test
    @DisplayName("firstEntry on an empty map should return null")
    void firstEntry_emptyMap_returnsNull() {
        assertNull(map.firstEntry());
    }

    @Test
    @DisplayName("firstEntry on a single-element map returns that element")
    void firstEntry_singleElement_returnsElement() {
        map.put(10, "Ten");

        Map.Entry<Integer, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals(10, entry.getKey());
        assertEquals("Ten", entry.getValue());
    }

    @Test
    @DisplayName("firstEntry returns minimum key when multiple elements are inserted out of order")
    void firstEntry_multipleElements_returnsMinimum() {
        map.put(50, "Fifty");
        map.put(20, "Twenty");
        map.put(70, "Seventy");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        Map.Entry<Integer, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals(10, entry.getKey());
        assertEquals("Ten", entry.getValue());
    }

    @Test
    @DisplayName("firstEntry updates dynamically when the minimum key is removed")
    void firstEntry_afterRemoval_updatesToNewMinimum() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(10, map.firstEntry().getKey());

        map.remove(10);
        assertEquals(20, map.firstEntry().getKey());
    }

    @Test
    @DisplayName("Modifying value in firstEntry snapshot does not alter tree map content")
    void firstEntry_returnedEntryIsSnapshot() {
        map.put(5, "Five");
        Map.Entry<Integer, String> entry = map.firstEntry();
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue("Updated Five"));
        assertEquals("Five", map.get(5));
    }

    @Test
    @DisplayName("lastEntry on an empty map returns null")
    void lastEntry_emptyMap_returnsNull() {
        assertNull(map.lastEntry());
    }

    @Test
    @DisplayName("lastEntry on a single-element map returns that element")
    void lastEntry_singleElement_returnsElement() {
        map.put(10, "Ten");

        Map.Entry<Integer, String> entry = map.lastEntry();
        assertNotNull(entry);
        assertEquals(10, entry.getKey());
        assertEquals("Ten", entry.getValue());
    }

    @Test
    @DisplayName("lastEntry returns maximum key when multiple elements exist")
    void lastEntry_multipleElements_returnsMaximum() {
        map.put(50, "Fifty");
        map.put(20, "Twenty");
        map.put(70, "Seventy");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        Map.Entry<Integer, String> entry = map.lastEntry();
        assertNotNull(entry);
        assertEquals(70, entry.getKey());
        assertEquals("Seventy", entry.getValue());
    }

    @Test
    @DisplayName("lastEntry updates dynamically when the maximum key is removed")
    void lastEntry_afterRemoval_updatesToNewMaximum() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        assertEquals(30, map.lastEntry().getKey());

        map.remove(30);
        assertEquals(20, map.lastEntry().getKey());
    }

    @Test
    @DisplayName("lastEntry returns an immutable entry snapshot")
    void lastEntry_returnsImmutableEntry() {
        map.put(100, "Hundred");
        Map.Entry<Integer, String> entry = map.lastEntry();

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue("Modified"));
    }

    @Test
    @DisplayName("pollFirstEntry and pollLastEntry on empty map return null")
    void pollEntries_emptyMap_returnsNull() {
        assertNull(map.pollFirstEntry());
        assertNull(map.pollLastEntry());
    }

    @Test
    @DisplayName("pollFirstEntry removes and returns the minimum element")
    void pollFirstEntry_removesAndReturnsMinimum() {
        map.put(30, "Thirty");
        map.put(10, "Ten");
        map.put(20, "Twenty");

        Map.Entry<Integer, String> entry = map.pollFirstEntry();

        assertNotNull(entry);
        assertEquals(10, entry.getKey());
        assertEquals("Ten", entry.getValue());
        assertEquals(2, map.size());
        assertFalse(map.containsKey(10));
    }

    @Test
    @DisplayName("pollLastEntry removes and returns the maximum element")
    void pollLastEntry_removesAndReturnsMaximum() {
        map.put(30, "Thirty");
        map.put(10, "Ten");
        map.put(20, "Twenty");

        Map.Entry<Integer, String> entry = map.pollLastEntry();

        assertNotNull(entry);
        assertEquals(30, entry.getKey());
        assertEquals("Thirty", entry.getValue());
        assertEquals(2, map.size());
        assertFalse(map.containsKey(30));
    }

    @Test
    @DisplayName("Repeatedly calling pollFirstEntry drains map in ascending order")
    void pollFirstEntry_repeatedly_drainsInAscendingOrder() {
        map.put(40, "Forty");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        assertEquals(10, map.pollFirstEntry().getKey());
        assertEquals(30, map.pollFirstEntry().getKey());
        assertEquals(40, map.pollFirstEntry().getKey());

        assertNull(map.pollFirstEntry());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Repeatedly calling pollLastEntry drains map in descending order")
    void pollLastEntry_repeatedly_drainsInDescendingOrder() {
        map.put(40, "Forty");
        map.put(10, "Ten");
        map.put(30, "Thirty");

        assertEquals(40, map.pollLastEntry().getKey());
        assertEquals(30, map.pollLastEntry().getKey());
        assertEquals(10, map.pollLastEntry().getKey());

        assertNull(map.pollLastEntry());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("floorEntry returns exact match when key exists, or lower key when absent")
    void floorEntry_exactAndAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        Map.Entry<Integer, String> exact = map.floorEntry(20);
        assertNotNull(exact);
        assertEquals(20, exact.getKey());
        assertEquals("Twenty", exact.getValue());

        Map.Entry<Integer, String> absent = map.floorEntry(25);
        assertNotNull(absent);
        assertEquals(20, absent.getKey());

        assertNull(map.floorEntry(5));
    }

    @Test
    @DisplayName("ceilingEntry returns exact match when key exists, or higher key when absent")
    void ceilingEntry_exactAndAbsent() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        Map.Entry<Integer, String> exact = map.ceilingEntry(30);
        assertNotNull(exact);
        assertEquals(30, exact.getKey());
        assertEquals("Thirty", exact.getValue());

        Map.Entry<Integer, String> absent = map.ceilingEntry(25);
        assertNotNull(absent);
        assertEquals(30, absent.getKey());

        assertNull(map.ceilingEntry(45));
    }

    @Test
    @DisplayName("lowerEntry returns strictly smaller key-value pair")
    void lowerEntry_strictlySmaller() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        Map.Entry<Integer, String> entry = map.lowerEntry(20);
        assertNotNull(entry);
        assertEquals(10, entry.getKey());

        assertNull(map.lowerEntry(10));
    }

    @Test
    @DisplayName("higherEntry returns strictly greater key-value pair")
    void higherEntry_strictlyGreater() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        Map.Entry<Integer, String> entry = map.higherEntry(30);
        assertNotNull(entry);
        assertEquals(40, entry.getKey());

        assertNull(map.higherEntry(40));
    }

    @Test
    @DisplayName("Passing null key throws NullPointerException")
    void nullKey_throwsException() {
        assertThrows(NullPointerException.class, () -> map.floorEntry(null));
        assertThrows(NullPointerException.class, () -> map.ceilingEntry(null));
        assertThrows(NullPointerException.class, () -> map.lowerEntry(null));
        assertThrows(NullPointerException.class, () -> map.higherEntry(null));
    }

    @Test
    @DisplayName("putAll into empty map correctly adds all items")
    void testPutAllIntoEmptyMap() {
        Map<Integer, String> source = Map.of(
                30, "Thirty",
                10, "Ten",
                20, "Twenty"
        );

        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals("Ten", map.get(10));
        assertEquals("Twenty", map.get(20));
        assertEquals("Thirty", map.get(30));
    }

    @Test
    @DisplayName("putAll() with an empty map changes nothing")
    void testPutAllWithEmptyMap() {
        map.put(1, "One");

        map.putAll(Collections.emptyMap());

        assertEquals(1, map.size());
        assertEquals("One", map.get(1));
    }

    @Test
    @DisplayName("putAll() overwrites existing keys with new values")
    void testPutAllOverwritingExistingKeys() {
        map.put(10, "OldTen");
        map.put(20, "OldTwenty");

        Map<Integer, String> updates = Map.of(
                20, "NewTwenty",
                30, "Thirty"
        );

        map.putAll(updates);

        assertEquals(3, map.size());
        assertEquals("OldTen", map.get(10));
        assertEquals("NewTwenty", map.get(20));
        assertEquals("Thirty", map.get(30));
    }

    @Test
    @DisplayName("putAll() supports subtype generics (Map<? extends K, ? extends V>)")
    void testPutAllWithSubtypes() {
        CustomTreeMap<Integer, Object> numberMap = new CustomTreeMap<>();

        Map<Integer, String> specificMap = Map.of(
                1, "One",
                2, "Two"
        );

        numberMap.putAll(specificMap);

        assertEquals(2, numberMap.size());
        assertEquals("One", numberMap.get(1));
        assertEquals("Two", numberMap.get(2));
    }

    @Test
    @DisplayName("putAll() maintains correct tree order and navigation after bulk insertion")
    void testPutAllNavigationIntegrity() {
        Map<Integer, String> source = new HashMap<>();
        for (int i = 100; i >= 10; i -= 10) {
            source.put(i, "Val" + i);
        }

        map.putAll(source);

        assertEquals(10, map.size());
        assertEquals(10, map.firstKey());
        assertEquals(100, map.lastKey());
        assertEquals(50, map.floorKey(55));
        assertEquals(60, map.ceilingKey(55));
    }

    @Test
    @DisplayName("putAll() correctly imports elements from another SortedMap / map")
    void testPutAllFromSortedMap() {
        Map<Integer, String> sortedSource = new CustomTreeMap<>();
        sortedSource.put(5, "Five");
        sortedSource.put(15, "Fifteen");
        sortedSource.put(25, "TwentyFive");

        map.putAll(sortedSource);

        assertEquals(3, map.size());
        assertEquals("Five", map.get(5));
        assertEquals("Fifteen", map.get(15));
        assertEquals("TwentyFive", map.get(25));
    }

    @Test
    void defaultConstructorShouldInitializeEmptyMapWithNullComparator() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.comparator());
    }

    @Test
    void customComparatorConstructorShouldStoreComparatorAndInitializeEmptyMap() {
        Comparator<String> customComparator = Comparator.reverseOrder();
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>(customComparator);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(customComparator, map.comparator());
    }

    @Test
    void customComparatorConstructorShouldAllowNullComparatorForNaturalOrdering() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>((Comparator<String>) null);

        assertTrue(map.isEmpty());
        assertNull(map.comparator());
    }

    @Test
    void mapConstructorShouldPopulateAllEntriesFromGivenMap() {
        Map<String, Integer> inputMap = Map.of("B", 2, "A", 1, "C", 3);
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>(inputMap);

        assertEquals(3, map.size());
        assertEquals(1, map.get("A"));
        assertEquals(2, map.get("B"));
        assertEquals(3, map.get("C"));
        assertNull(map.comparator());
    }

    @Test
    void mapConstructorShouldInitializeEmptyMapWhenGivenEmptyMap() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>(Collections.emptyMap());

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    void mapConstructorShouldThrowNullPointerExceptionWhenMapIsNull() {
        assertThrows(NullPointerException.class, () -> new CustomTreeMap<>((Map<String, Integer>) null));
    }

    @Test
    void sortedMapConstructorShouldPopulateEntriesAndPreserveComparator() {
        Comparator<String> customComparator = Comparator.reverseOrder();
        SortedMap<String, Integer> inputSortedMap = new TreeMap<>(customComparator);
        inputSortedMap.put("A", 1);
        inputSortedMap.put("B", 2);

        CustomTreeMap<String, Integer> map = new CustomTreeMap<>(inputSortedMap);

        assertEquals(2, map.size());
        assertEquals(1, map.get("A"));
        assertEquals(2, map.get("B"));
        assertEquals(customComparator, map.comparator());
    }

    @Test
    void sortedMapConstructorShouldInheritNullComparatorWhenSortedMapUsesNaturalOrdering() {
        SortedMap<String, Integer> inputSortedMap = new TreeMap<>();
        inputSortedMap.put("A", 1);

        CustomTreeMap<String, Integer> map = new CustomTreeMap<>(inputSortedMap);

        assertEquals(1, map.size());
        assertNull(map.comparator());
    }

    @Test
    void sortedMapConstructorShouldThrowNullPointerExceptionWhenSortedMapIsNull() {
        assertThrows(NullPointerException.class, () -> new CustomTreeMap<>((SortedMap<String, Integer>) null));
    }

    @Test
    void entrySetSizeShouldReturnZeroForEmptyMap() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        assertEquals(0, entrySet.size());
    }

    @Test
    void entrySetSizeShouldReflectNumberOfInsertedEntries() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        assertEquals(3, map.entrySet().size());
    }

    @Test
    void entrySetSizeShouldNotIncreaseWhenUpdatingExistingKey() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();
        map.put("A", 1);
        map.put("A", 99);

        assertEquals(1, map.entrySet().size());
    }

    @Test
    void entrySetSizeShouldDecreaseWhenEntryIsRemovedFromMap() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();
        map.put("A", 1);
        map.put("B", 2);

        map.remove("A");

        assertEquals(1, map.entrySet().size());
    }

    @Test
    void entrySetSizeShouldDynamicallyReflectChangesToOuterMap() {
        CustomTreeMap<String, Integer> map = new CustomTreeMap<>();
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        assertEquals(0, entrySet.size());

        map.put("A", 1);
        assertEquals(1, entrySet.size());

        map.clear();
        assertEquals(0, entrySet.size());
    }

    @Test
    @DisplayName("keySet returns empty set on empty map")
    void keySet_emptyMap() {
        Set<Integer> keys = map.keySet();
        assertTrue(keys.isEmpty());
        assertEquals(0, keys.size());
    }

    @Test
    @DisplayName("keySet returns all keys in sorted order")
    void keySet_returnsSortedKeys() {
        map.put(30, "Thirty");
        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertThat(map.keySet().containsAll(List.of(10, 20, 30)));
    }

    @Test
    @DisplayName("Removing element from keySet reflects in backing map")
    void keySet_removeReflectsInMap() {
        map.put(1, "One");
        map.put(2, "Two");

        Set<Integer> keys = map.keySet();
        keys.remove(1);

        assertThat(map.containsKey(1)).isFalse();
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Adding element to keySet throws UnsupportedOperationException")
    void keySet_addThrowsException() {
        assertThatThrownBy(() -> map.keySet().add(100))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("tailMap on empty map returns empty view")
    void tailMap_emptyMap() {
        NavigableMap<Integer, String> tail = map.tailMap(10, true);
        assertTrue(tail.isEmpty());
    }

    @Test
    @DisplayName("tailMap inclusive includes start key")
    void tailMap_inclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        NavigableMap<Integer, String> tail = map.tailMap(20, true);
        assertTrue(tail.keySet().containsAll(List.of(20, 30)));
    }

    @Test
    @DisplayName("tailMap exclusive excludes start key")
    void tailMap_exclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        NavigableMap<Integer, String> tail = map.tailMap(20, false);
        assertTrue(tail.keySet().contains(30));
    }

    @Test
    @DisplayName("Mutations via tailMap alter the backing tree")
    void tailMap_mutationsReflectInBackingMap() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        NavigableMap<Integer, String> tail = map.tailMap(20, true);
        tail.put(25, "Twenty-Five");
        tail.remove(30);

        assertTrue(map.containsKey(25));
        assertFalse(map.containsKey(30));
        assertEquals(Set.of(10, 20, 25), map.keySet());
    }

    @Test
    @DisplayName("Inserting key below lower bound throws IllegalArgumentException")
    void tailMap_outOfBoundsInsertion() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        NavigableMap<Integer, String> tail = map.tailMap(20, true);

        assertThatThrownBy(() -> tail.put(15, "Fifteen"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isEmpty and size reflect backing map state")
    void sizeAndEmpty() {
        assertTrue(keySet.isEmpty());
        assertEquals(0, keySet.size());

        map.put(10, "Ten");
        map.put(20, "Twenty");

        assertEquals(2, keySet.size());
        assertTrue(keySet.contains(10));
        assertTrue(keySet.contains(20));
    }

    @Test
    @DisplayName("Removing element from keySet removes key-value pair from map")
    void remove_reflectsInMap() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        boolean removed = keySet.remove(10);

        assertTrue(removed);
        assertFalse(map.containsKey(10));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("clear() on keySet empties backing map")
    void clear_emptiesMap() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        keySet.clear();

        assertTrue(keySet.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("add() throws UnsupportedOperationException")
    void add_throwsException() {
        assertThrows(UnsupportedOperationException.class, () -> keySet.add(10));
    }

    @Test
    @DisplayName("Iterator remove() deletes current key from backing map")
    void iterator_removeReflectsInMap() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        Iterator<Integer> it = keySet.iterator();
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        it.remove();

        assertFalse(map.containsKey(10));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("subMap(fromKey, toKey) includes fromKey (inclusive) and excludes toKey (exclusive)")
    void subMap_defaultInclusivity() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");

        SortedMap<Integer, String> sub = map.subMap(20, 40);

        assertTrue(sub.containsKey(20), "fromKey (20) should be included");
        assertTrue(sub.containsKey(30), "Intermediate key (30) should be included");
        assertFalse(sub.containsKey(40), "toKey (40) should be excluded");

        assertEquals(2, sub.size());
    }

    @Test
    @DisplayName("firstKey() returns lowest present key >= fromKey and lastKey() returns highest present key < toKey")
    void subMap_firstAndLastKey() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");

        SortedMap<Integer, String> sub = map.subMap(20, 50);

        assertEquals(20, sub.firstKey());
        assertEquals(40, sub.lastKey());
    }

    @Test
    @DisplayName("Keys outside the original tree boundaries still define valid submap ranges")
    void subMap_keysOutsideTreeBounds() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");
        SortedMap<Integer, String> sub = map.subMap(15, 45);
        assertEquals(3, sub.size());
        assertEquals(20, sub.firstKey());
        assertEquals(40, sub.lastKey());
        sub.put(18, "Eighteen");
        assertTrue(map.containsKey(18));

        assertThrows(IllegalArgumentException.class, () -> sub.put(14, "Fourteen"));
        assertThrows(IllegalArgumentException.class, () -> sub.put(45, "Forty-Five"));
    }

    @Test
    @DisplayName("Putting in subMap reflects in backing map")
    void subMap_putReflectsInBackingMap() {
        SortedMap<Integer, String> sub = map.subMap(20, 40);
        sub.put(25, "Twenty-Five");

        assertTrue(map.containsKey(25));
        assertEquals("Twenty-Five", map.get(25));
    }

    @Test
    @DisplayName("Removing from subMap reflects in backing map")
    void subMap_removeReflectsInBackingMap() {
        SortedMap<Integer, String> sub = map.subMap(20, 40);
        sub.remove(30);

        assertFalse(map.containsKey(30));
        assertFalse(sub.containsKey(30));
    }

    @Test
    @DisplayName("Modifying backing tree reflects in subMap view")
    void backingMap_putReflectsInSubMap() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");

        SortedMap<Integer, String> sub = map.subMap(20, 40);
        assertEquals(2, sub.size());

        map.put(25, "Twenty-Five");

        assertTrue(sub.containsKey(25));
        assertEquals(3, sub.size());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when fromKey > toKey")
    void subMap_fromKeyGreaterThanToKey_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> map.subMap(40, 20));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when inserting key out of subMap bounds")
    void subMap_putOutOfRange_throwsException() {
        SortedMap<Integer, String> sub = map.subMap(20, 40);

        assertThrows(IllegalArgumentException.class, () -> sub.put(15, "Fifteen"));
        assertThrows(IllegalArgumentException.class, () -> sub.put(40, "Forty"));
        assertThrows(IllegalArgumentException.class, () -> sub.put(45, "Forty-Five"));
    }

    @Test
    @DisplayName("Should return entries strictly less than toKey")
    void testHeadMapExclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        NavigableMap<Integer, String> head = map.headMap(30, false);

        assertEquals(2, head.size());
        assertTrue(head.containsKey(10));
        assertTrue(head.containsKey(20));
        assertFalse(head.containsKey(30));
    }

    @Test
    @DisplayName("Should return all entries if toKey is greater than all keys")
    void testHeadMapExclusiveKeyGreaterThanAll() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");
        NavigableMap<Integer, String> head = map.headMap(60, false);

        assertEquals(5, head.size());
        assertEquals(map, head);
    }

    @Test
    @DisplayName("Should return empty submap if toKey is less than or equal to lowest key")
    void testHeadMapExclusiveKeyLessThanAll() {
        NavigableMap<Integer, String> head = map.headMap(10, false);

        assertTrue(head.isEmpty());
    }

    @Test
    @DisplayName("Should return entries less than or equal to toKey")
    void testHeadMapInclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        NavigableMap<Integer, String> head = map.headMap(30, true);

        assertEquals(3, head.size());
        assertTrue(head.containsKey(10));
        assertTrue(head.containsKey(20));
        assertTrue(head.containsKey(30));
        assertFalse(head.containsKey(40));
    }

    @Test
    @DisplayName("Should include matching boundary element when toKey exists")
    void testHeadMapInclusiveBoundary() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        NavigableMap<Integer, String> head = map.headMap(10, true);

        assertEquals(1, head.size());
        assertTrue(head.containsKey(10));
    }

    @Test
    @DisplayName("Changes in backing map should reflect in headMap view")
    void testBackingMapMutationReflected() {
        NavigableMap<Integer, String> head = map.headMap(30, false);
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(15, "Fifteen");

        assertEquals(3, head.size());
        assertTrue(head.containsKey(15));
    }

    @Test
    @DisplayName("Changes in headMap view should reflect in backing map")
    void testSubMapMutationReflectedInBackingMap() {
        NavigableMap<Integer, String> head = map.headMap(30, false);

        head.put(25, "Twenty-Five");

        assertTrue(map.containsKey(25));
        assertEquals("Twenty-Five", map.get(25));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding element outside headMap bound")
    void testSubMapPutOutOfBoundsThrows() {
        NavigableMap<Integer, String> head = map.headMap(30, false);

        assertThrows(IllegalArgumentException.class, () -> head.put(35, "Thirty-Five"));
        assertThrows(IllegalArgumentException.class, () -> head.put(30, "Thirty"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when toKey is null in natural order map")
    void testNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> map.headMap(null, true));
    }

    @Test
    @DisplayName("Should properly retrieve lowest and highest keys from submap view")
    void testSubMapBoundaryMethods() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");

        NavigableMap<Integer, String> head = map.headMap(30, true);

        assertEquals(10, head.firstKey());
        assertEquals(30, head.lastKey());
    }

    @Test
    @DisplayName("Should throw NullPointerException when requesting keys on an empty submap view")
    void testEmptySubMapBoundaryMethodsThrow() {
        NavigableMap<Integer, String> head = map.headMap(5, true);
        assertThrows(NullPointerException.class, head::firstKey);
        assertThrows(NullPointerException.class, head::lastKey);
    }

    @Test
    @DisplayName("headMap(toKey) should return entries strictly less than toKey")
    void testHeadMapOneArgExclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        SortedMap<Integer, String> head = map.headMap(30);

        assertEquals(2, head.size());
        assertTrue(head.containsKey(10));
        assertTrue(head.containsKey(20));
        assertFalse(head.containsKey(30));
    }

    @Test
    @DisplayName("headMap(toKey) should return empty view if toKey is smaller than or equal to lowest key")
    void testHeadMapOneArgKeySmallerThanMin() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        SortedMap<Integer, String> head = map.headMap(10);

        assertTrue(head.isEmpty());
        assertEquals(0, head.size());
    }

    @Test
    @DisplayName("headMap(toKey) should include all entries if toKey is greater than max key")
    void testHeadMapOneArgKeyGreaterThanAll() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");

        SortedMap<Integer, String> head = map.headMap(60);

        assertEquals(5, head.size());
        assertEquals(map, head);
    }

    @Test
    @DisplayName("headMap(toKey) should reflect mutations made to the backing map")
    void testHeadMapOneArgBackingMapMutationReflected() {
        map.put(10, "Ten");
        map.put(20, "Twenty");

        SortedMap<Integer, String> head = map.headMap(30);

        map.put(15, "Fifteen");

        assertEquals(3, head.size());
        assertTrue(head.containsKey(15));
    }

    @Test
    @DisplayName("tailMap(fromKey) should include fromKey and all greater entries")
    void testTailMapOneArgInclusive() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        SortedMap<Integer, String> tail = map.tailMap(20);

        assertEquals(2, tail.size());
        assertFalse(tail.containsKey(10));
        assertTrue(tail.containsKey(20));
        assertTrue(tail.containsKey(30));
    }

    @Test
    @DisplayName("tailMap(fromKey) should return empty view if fromKey is strictly greater than max key")
    void testTailMapOneArgKeyGreaterThanMax() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");

        SortedMap<Integer, String> tail = map.tailMap(40);

        assertTrue(tail.isEmpty());
        assertEquals(0, tail.size());
    }

    @Test
    @DisplayName("tailMap(fromKey) should return all entries if fromKey is less than or equal to lowest key")
    void testTailMapOneArgKeyLessThanOrEqualMin() {
        map.put(10, "Ten");
        map.put(20, "Twenty");
        map.put(30, "Thirty");
        map.put(40, "Forty");
        map.put(50, "Fifty");

        SortedMap<Integer, String> tail = map.tailMap(10);

        assertEquals(5, tail.size());
        assertEquals(map, tail);
    }

    @Test
    @DisplayName("tailMap(fromKey) should reflect mutations made to the backing map")
    void testTailMapOneArgBackingMapMutationReflected() {
        map.put(10, "Ten");
        map.put(30, "Thirty");

        SortedMap<Integer, String> tail = map.tailMap(20);

        map.put(25, "Twenty-Five");

        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(25));
        assertTrue(tail.containsKey(30));
        assertFalse(tail.containsKey(10));
    }

    @Test
    @DisplayName("put(key, value) with null key throws NullPointerExceptopn")
    void thePutWithNullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CustomTreeMap<>().put(null, 10));
    }
}