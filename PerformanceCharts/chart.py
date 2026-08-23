#!/usr/bin/env python3
"""
Generate performance benchmark charts as PNG files comparing CustomTreeMap and JDK TreeMap
with a transparent background.
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
from matplotlib.lines import Line2D
import pandas as pd
import os
import sys
import re
import numpy as np

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────

CUSTOMTREEMAP_CSV_PATH = "CustomTreeMap_jmh_performance.csv"
TREEMAP_CSV_PATH = "TreeMap_jmh_performance.csv"
OUTPUT_DIR = "charts"

COLORS = {
    'purple': '#9B6EF3',  # CustomTreeMap
    'blue': '#4DA6FF',    # JDK TreeMap
    'grid': '#252525',
}

FIGURE_SIZE = (12, 6.2)
DPI = 150

# Map full CSV signature labels -> short chart title / filename-safe key
def canonicalize(name: str) -> str:
    """Normalize a Benchmark column value to a stable chart key."""
    n = name.strip().strip('"')

    # Already-short common forms
    aliases = {
        # constructors (CustomTreeMap / TreeMap / Constructor prefixes)
        'CustomTreeMap()': 'TreeMap()',
        'TreeMap()': 'TreeMap()',
        'Constructor()': 'TreeMap()',
        'CustomTreeMap(final Comparator<? super K> comparator)': 'TreeMap(Comparator)',
        'TreeMap(final Comparator<? super K> comparator)': 'TreeMap(Comparator)',
        'Constructor(final Comparator<? super K> comparator)': 'TreeMap(Comparator)',
        'CustomTreeMap(final Map<? extends K, ? extends V> m)': 'TreeMap(Map)',
        'TreeMap(final Map<? extends K, ? extends V> m)': 'TreeMap(Map)',
        'Constructor(final Map<? extends K, ? extends V> m)': 'TreeMap(Map)',
        'CustomTreeMap(final SortedMap<K, ? extends V> m)': 'TreeMap(SortedMap)',
        'TreeMap(final SortedMap<K, ? extends V> m)': 'TreeMap(SortedMap)',
        'Constructor(final SortedMap<K, ? extends V> m)': 'TreeMap(SortedMap)',
        # core
        'put(final K key, final V value)': 'put(K, V)',
        'get(final Object key)': 'get(Object)',
        'remove(final Object key)': 'remove(Object)',
        'containsKey(final Object key)': 'containsKey(Object)',
        'containsValue(final Object value)': 'containsValue(Object)',
        'clear()': 'clear()',
        'size()': 'size()',
        'isEmpty()': 'isEmpty()',
        'comparator()': 'comparator()',
        'putAll(Map<? extends K, ? extends V> m)': 'putAll(Map)',
        # navigable
        'ceilingEntry(final K key)': 'ceilingEntry(K)',
        'ceilingKey(final K key)': 'ceilingKey(K)',
        'floorEntry(final K key)': 'floorEntry(K)',
        'floorKey(final K key)': 'floorKey(K)',
        'higherEntry(final K key)': 'higherEntry(K)',
        'higherKey(final K key)': 'higherKey(K)',
        'lowerEntry(final K key)': 'lowerEntry(K)',
        'lowerKey(final K key)': 'lowerKey(K)',
        'firstEntry()': 'firstEntry()',
        'firstKey()': 'firstKey()',
        'lastEntry()': 'lastEntry()',
        'lastKey()': 'lastKey()',
        'pollFirstEntry()': 'pollFirstEntry()',
        'pollLastEntry()': 'pollLastEntry()',
        # views
        'entrySet()': 'entrySet()',
        'entrySet().iterator()': 'entrySet().iterator()',
        'keySet()': 'keySet()',
        'keySet().iterator()': 'keySet().iterator()',
        'values()': 'values()',
        'values().iterator()': 'values().iterator()',
        'navigableKeySet()': 'navigableKeySet()',
        'navigableKeySet().iterator()': 'navigableKeySet().iterator()',
        'navigableKeySet().descendingIterator()': 'navigableKeySet().descendingIterator()',
        'descendingKeySet()': 'descendingKeySet()',
        'descendingKeySet().iterator()': 'descendingKeySet().iterator()',
        'descendingMap()': 'descendingMap()',
        'descendingMap().entrySet().iterator()': 'descendingMap().entrySet().iterator()',
        # submaps
        'headMap(final K toKey)': 'headMap(K)',
        'headMap(final K toKey, final boolean inclusive)': 'headMap(K, boolean)',
        'headMap(final K toKey).entrySet().iterator()': 'headMap(K).iterator()',
        'tailMap(final K fromKey)': 'tailMap(K)',
        'tailMap(final K fromKey, final boolean inclusive)': 'tailMap(K, boolean)',
        'tailMap(final K fromKey).entrySet().iterator()': 'tailMap(K).iterator()',
        'subMap(final K fromKey, final K toKey)': 'subMap(K, K)',
        'subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive)': 'subMap(K, boolean, K, boolean)',
        'subMap(final K fromKey, final K toKey).entrySet().iterator()': 'subMap(K, K).entrySet().iterator()',
        'subMap(final K fromKey, final K toKey).keySet().iterator()': 'subMap(K, K).keySet().iterator()',
        'subMap(final K fromKey, final K toKey).values().iterator()': 'subMap(K, K).values().iterator()',
    }
    if n in aliases:
        return aliases[n]
    # fallback: strip "final " noise for a readable key
    return re.sub(r'\bfinal\s+', '', n)


def safe_filename(label: str) -> str:
    s = re.sub(r'[<>:"/\\|?*]', '', label)
    s = re.sub(r'\s+', '_', s)
    s = s.replace('(', '_').replace(')', '').replace(',', '').replace('.', '_')
    s = re.sub(r'_+', '_', s).strip('_')
    return s or 'chart'


# ──────────────────────────────────────────────────────────────────────────────
# CSV Loading (long format: Benchmark;Size;Score (ns/op))
# ──────────────────────────────────────────────────────────────────────────────

def load_csv(filepath):
    """
    Load long-format JMH CSV.
    Returns: dict[size][canonical_op] = score
    """
    df = pd.read_csv(filepath, sep=';')
    # normalize column names
    cols = {c: c.strip() for c in df.columns}
    df = df.rename(columns=cols)

    bench_col = 'Benchmark'
    size_col = 'Size'
    score_col = [c for c in df.columns if 'Score' in c or 'ns' in c.lower()]
    if not score_col:
        raise ValueError(f"No score column in {filepath}: {list(df.columns)}")
    score_col = score_col[0]

    df[bench_col] = df[bench_col].astype(str).str.strip().str.strip('"')
    df[size_col] = pd.to_numeric(df[size_col], errors='coerce')
    df[score_col] = pd.to_numeric(df[score_col], errors='coerce')
    df = df.dropna(subset=[size_col, score_col])

    data = {}
    for _, row in df.iterrows():
        size = int(row[size_col])
        op = canonicalize(row[bench_col])
        score = float(row[score_col])
        data.setdefault(size, {})[op] = score
    return data


# ──────────────────────────────────────────────────────────────────────────────
# Chart Generation
# ──────────────────────────────────────────────────────────────────────────────

def format_y_axis(value, pos):
    if value == 0:
        return '0'
    if abs(value) >= 1_000_000:
        return f'{value/1_000_000:,.2f}M'
    if abs(value) >= 1_000:
        return f'{value/1_000:,.2f}k'
    return f'{value:,.2f}'


def create_chart(op_label, custom_data, jdk_data, canonical_sizes, output_path):
    custom_values = [
        custom_data[s][op_label] if s in custom_data and op_label in custom_data[s] else np.nan
        for s in canonical_sizes
    ]
    jdk_values = [
        jdk_data[s][op_label] if s in jdk_data and op_label in jdk_data[s] else np.nan
        for s in canonical_sizes
    ]

    # skip if both series entirely missing
    if all(np.isnan(custom_values)) and all(np.isnan(jdk_values)):
        return False

    fig, ax = plt.subplots(figsize=FIGURE_SIZE, dpi=DPI)
    fig.patch.set_alpha(0)
    ax.set_facecolor('none')

    x_positions = list(range(len(canonical_sizes)))

    ax.plot(x_positions, custom_values, color=COLORS['purple'], linewidth=1.5, zorder=2)
    ax.plot(x_positions, jdk_values, color=COLORS['blue'], linewidth=1.5, zorder=2)

    ax.scatter(x_positions, custom_values, color=COLORS['purple'], s=35, marker='o',
               edgecolors=COLORS['purple'], linewidths=1.5, zorder=3)
    ax.scatter(x_positions, jdk_values, color=COLORS['blue'], s=35, marker='o',
               edgecolors=COLORS['blue'], linewidths=1.5, zorder=3)

    ax.grid(True, color=COLORS['grid'], linewidth=0.8, linestyle='-', zorder=0)
    ax.set_axisbelow(True)

    ax.set_xticks(x_positions)
    ax.set_xticklabels([f'{s:,}' for s in canonical_sizes], color='white', fontsize=10)
    ax.tick_params(axis='x', colors='white', length=0, pad=8)
    ax.set_xlim(-0.4, len(canonical_sizes) - 0.6)

    ax.yaxis.set_major_formatter(mticker.FuncFormatter(format_y_axis))
    ax.tick_params(axis='y', colors='white', length=0, pad=8)
    for label in ax.get_yticklabels():
        label.set_color('white')
        label.set_fontsize(10)

    for spine in ax.spines.values():
        spine.set_visible(False)

    ax.set_xlabel('Size', color='white', fontsize=12, labelpad=12)
    ax.set_ylabel('Time (ns/op)', color='white', fontsize=11, labelpad=10)
    ax.set_title(op_label, color='white', fontsize=15, fontweight='bold', pad=14)

    legend_elements = [
        Line2D([0], [0], marker='o', color='none', markerfacecolor=COLORS['purple'],
               markeredgecolor=COLORS['purple'], markeredgewidth=1.5, markersize=8,
               label='Custom', linestyle='none'),
        Line2D([0], [0], marker='o', color='none', markerfacecolor=COLORS['blue'],
               markeredgecolor=COLORS['blue'], markeredgewidth=1.5, markersize=8,
               label='JDK', linestyle='none'),
    ]

    leg = ax.legend(handles=legend_elements, loc='upper center', bbox_to_anchor=(0.5, -0.26),
                    ncol=2, frameon=False, fontsize=12, handlelength=1.5,
                    handletextpad=0.6, columnspacing=2.0)
    for text in leg.get_texts():
        text.set_color('white')
        text.set_fontsize(12)

    plt.tight_layout(rect=[0, 0.18, 1, 1])
    fig.savefig(output_path, dpi=DPI, transparent=True, bbox_inches='tight',
                facecolor='none', edgecolor='none')
    plt.close(fig)
    return True


def main():
    if not os.path.exists(CUSTOMTREEMAP_CSV_PATH):
        print(f"Error: Required file '{CUSTOMTREEMAP_CSV_PATH}' not found.")
        sys.exit(1)
    if not os.path.exists(TREEMAP_CSV_PATH):
        print(f"Error: Required file '{TREEMAP_CSV_PATH}' not found.")
        sys.exit(1)

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    custom_data = load_csv(CUSTOMTREEMAP_CSV_PATH)
    jdk_data = load_csv(TREEMAP_CSV_PATH)

    canonical_sizes = sorted(set(custom_data.keys()) | set(jdk_data.keys()))

    # union of all operation labels present in either dataset
    ops = set()
    for s in canonical_sizes:
        ops |= set(custom_data.get(s, {}).keys())
        ops |= set(jdk_data.get(s, {}).keys())

    generated = 0
    for op in sorted(ops):
        out = os.path.join(OUTPUT_DIR, f'{safe_filename(op)}.png')
        if create_chart(op, custom_data, jdk_data, canonical_sizes, out):
            print(f"  ✓ {op}  →  {out}")
            generated += 1

    print(f"\n✓ {generated} comparison charts saved under '{OUTPUT_DIR}/'")


if __name__ == '__main__':
    main()
