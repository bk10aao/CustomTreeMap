from pathlib import Path
import pandas as pd

HEADER_MAPPING = {
    # Constructors
    "testDefaultConstructor": "CustomTreeMap()",
    "testComparatorConstructor": "CustomTreeMap(Comparator)",
    "testMapConstructor": "CustomTreeMap(Map)",
    "testSortedMapConstructor": "CustomTreeMap(SortedMap)",

    # Core Operations
    "testPut": "put(K, V)",
    "testPutAll": "putAll(Map)",
    "testGet": "get(Object)",
    "testRemove": "remove(Object)",
    "testClear": "clear()",
    "testSize": "size()",
    "testIsEmpty": "isEmpty()",
    "testContainsKey": "containsKey(Object)",
    "testContainsValue": "containsValue(Object)",
    "testComparator": "comparator()",

    # Navigation & Bounds
    "testFirstKey": "firstKey()",
    "testLastKey": "lastKey()",
    "testFirstEntry": "firstEntry()",
    "testLastEntry": "lastEntry()",
    "testPollFirstEntry": "pollFirstEntry()",
    "testPollLastEntry": "pollLastEntry()",
    "testLowerKey": "lowerKey(K)",
    "testLowerEntry": "lowerEntry(K)",
    "testFloorKey": "floorKey(K)",
    "testFloorEntry": "floorEntry(K)",
    "testCeilingKey": "ceilingKey(K)",
    "testCeilingEntry": "ceilingEntry(K)",
    "testHigherKey": "higherKey(K)",
    "testHigherEntry": "higherEntry(K)",

    # Views & SubMaps
    "testKeySet": "keySet()",
    "testValues": "values()",
    "testEntrySet": "entrySet()",
    "testNavigableKeySet": "navigableKeySet()",
    "testDescendingKeySet": "descendingKeySet()",
    "testDescendingMap": "descendingMap()",
    "testSubMapTwoArgs": "subMap(K, K)",
    "testSubMapFourArgs": "subMap(K, boolean, K, boolean)",
    "testHeadMapOneArg": "headMap(K)",
    "testHeadMapTwoArgs": "headMap(K, boolean)",
    "testTailMapOneArg": "tailMap(K)",
    "testTailMapTwoArgs": "tailMap(K, boolean)",

    # Iterators
    "testKeySetIterator": "keySet().iterator()",
    "testValuesIterator": "values().iterator()",
    "testEntrySetIterator": "entrySet().iterator()",
    "testNavigableKeySetIterator": "navigableKeySet().iterator()",
    "testNavigableKeySetDescendingIterator": "navigableKeySet().descendingIterator()",
    "testDescendingKeySetIterator": "descendingKeySet().iterator()",
    "testDescendingMapEntrySetIterator": "descendingMap().entrySet().iterator()",
    "testSubMapIterator": "subMap().iterator()",
    "testHeadMapIterator": "headMap().iterator()",
    "testTailMapIterator": "tailMap().iterator()",
    "testSubMapKeySetIterator": "subMap().keySet().iterator()",
    "testSubMapValuesIterator": "subMap().values().iterator()",
}

COLUMN_ORDER = list(HEADER_MAPPING.values())


def convert_to_wide_matrix(input_file: str | Path, output_file: str | Path) -> None:
    input_path = Path(input_file)
    output_path = Path(output_file)

    if not input_path.exists():
        print(f"Skipping '{input_path}' (file not found).")
        return

    df = pd.read_csv(input_path, sep=';')

    # Extract short method name from fully-qualified package string
    clean_benchmark = df['Benchmark'].astype(str).str.strip('"\t ')
    method_names = clean_benchmark.str.split('.').str[-1]

    # Map method names using HEADER_MAPPING with raw name fallback
    df['Metric'] = method_names.map(HEADER_MAPPING).fillna(method_names)

    # Pivot table and aggregate scores safely using mean
    pivot_df = df.pivot_table(
        index='Size',
        columns='Metric',
        values='Score (ns/op)',
        aggfunc='mean'
    ).round()

    # Safely convert to nullable Int64 to avoid crashes on missing/NaN cells
    pivot_df = pivot_df.astype('Int64')

    # Reorder according to COLUMN_ORDER while appending any unmapped extra columns
    available_columns = [col for col in COLUMN_ORDER if col in pivot_df.columns]
    extra_columns = [col for col in pivot_df.columns if col not in available_columns]
    pivot_df = pivot_df.reindex(columns=available_columns + extra_columns)

    # Export wide matrix CSV
    pivot_df.to_csv(output_path, sep=';')
    print(f"Wide matrix CSV created: {output_path}")


if __name__ == "__main__":
    convert_to_wide_matrix("CustomTreeMap_jmh_performance.csv", "CustomTreeMap_wide_matrix.csv")
    convert_to_wide_matrix("TreeMap_jmh_performance.csv", "TreeMap_wide_matrix.csv")