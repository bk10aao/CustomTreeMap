import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy.stats import gmean
# Load CSV files with semicolon delimiter (long format: Benchmark;Size;Score (ns/op))
custom_raw = pd.read_csv('CustomTreeMap_jmh_performance.csv', sep=';')
jdk_raw = pd.read_csv('TreeMap_jmh_performance.csv', sep=';')
# Pivot each file into a wide Size x Benchmark matrix
custom_df = custom_raw.pivot(index='Size', columns='Benchmark', values='Score (ns/op)').reset_index()
jdk_df = jdk_raw.pivot(index='Size', columns='Benchmark', values='Score (ns/op)').reset_index()
common_sizes = set(custom_df['Size']).intersection(set(jdk_df['Size']))
custom_df = (
    custom_df[custom_df['Size'].isin(common_sizes)]
    .sort_values('Size')
    .reset_index(drop=True)
)
jdk_df = (
    jdk_df[jdk_df['Size'].isin(common_sizes)]
    .sort_values('Size')
    .reset_index(drop=True)
)
methods = [col for col in custom_df.columns if col != 'Size']
custom_df_fixed = custom_df.copy()
jdk_df_fixed = jdk_df.copy()
for col in methods:
  custom_df_fixed[col] = custom_df_fixed[col].replace(0, 1)
  jdk_df_fixed[col] = jdk_df_fixed[col].replace(0, 1)
ratios = []
labels = []
colors = []
jdk_win_color = '#FF4D4D'
custom_win_color = '#4DA6FF'
for m in methods:
  g_custom = gmean(custom_df_fixed[m])
  g_jdk = gmean(jdk_df_fixed[m])
  if g_jdk < g_custom:
    speedup = g_custom / g_jdk
    ratios.append(speedup - 1)
    colors.append(jdk_win_color)
  else:
    speedup = g_jdk / g_custom
    ratios.append(-(speedup - 1))
    colors.append(custom_win_color)
  labels.append(m)
sorted_indices = np.argsort(ratios)
sorted_ratios = [ratios[idx] for idx in sorted_indices]
sorted_labels = [labels[idx] for idx in sorted_indices]
sorted_colors = [colors[idx] for idx in sorted_indices]
min_ratio = min(sorted_ratios)
max_ratio = max(sorted_ratios)
# Extra margin on sides so annotations fit cleanly
left_limit = min_ratio - 0.1
right_limit = max_ratio + 0.1
fig_height = max(6, len(methods) * 0.45)
fig, ax = plt.subplots(figsize=(12, fig_height), facecolor='none')
ax.set_facecolor('none')
bars = ax.barh(
    range(len(sorted_labels)),
    sorted_ratios,
    color=sorted_colors,
    alpha=0.9,
    height=0.6,
)
ax.axvline(x=0, color='#ffffff', linewidth=1.2)
ax.set_xlim(left_limit, right_limit)
ticks = []
if left_limit < -1.0:
  ticks.append(-1.0)
ticks.append(0.0)
for t in [1.0, 2.0, 3.0, 4.0]:
  if t <= right_limit:
    ticks.append(t)
ax.set_xticks(ticks)
ax.set_xticklabels(
    [f'{abs(t)+1:.1f}x' if abs(t) > 0.05 else 'Tie' for t in ticks],
    color='#ffffff',
    fontsize=11,
)
ax.set_ylim(-0.5, len(methods) - 0.5)
ax.set_yticks(range(len(sorted_labels)))
ax.set_yticklabels(sorted_labels, color='#ffffff', fontsize=10)
# Add exact numeric speedup values directly next to each bar
for idx, (bar, r) in enumerate(zip(bars, sorted_ratios)):
  val = abs(r)
  if val < 0.02:
    text_str = 'Tie'
  else:
    factor = val + 1
    if r < 0:
      text_str = f'JDK {factor:.2f}x'
    else:
      text_str = f'Custom {factor:.2f}x'
  # Position text outside the bar on the correct side
  if r >= 0:
    ax.text(
        r + 0.02,
        idx,
        f'  {text_str}',
        va='center',
        ha='left',
        color='#ffffff',
        fontsize=9,
        fontweight='bold',
    )
  else:
    ax.text(
        r - 0.02,
        idx,
        f'{text_str}  ',
        va='center',
        ha='right',
        color='#ffffff',
        fontsize=9,
        fontweight='bold',
    )
ax.set_title(
    (
        'Overall Relative Performance Comparison (CustomTreeMap vs JDK'
        ' TreeMap)\n(Geometric Mean Across All Sizes)'
    ),
    fontsize=14,
    fontweight='bold',
    pad=15,
    color='#ffffff',
)
ax.set_xlabel(
    '← JDK Faster  |  Relative Speedup Factor  |  Custom Faster →',
    fontsize=12,
    labelpad=10,
    color='#ffffff',
)
ax.grid(True, axis='x', linestyle='--', alpha=0.3, color='#888888')
ax.tick_params(colors='#ffffff', which='both', length=0)
for spine in ax.spines.values():
  spine.set_edgecolor('#555555')
plt.tight_layout()
plt.savefig('geometric.png', dpi=300, transparent=True)
plt.close()
print(
    'Generated geometric comparison graph with exact values successfully!'
)