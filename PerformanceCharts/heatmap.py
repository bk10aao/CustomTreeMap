import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
script_dir = os.path.dirname(os.path.abspath(__file__))
custom_csv = os.path.join(script_dir, 'CustomTreeMap_wide_matrix.csv')
jdk_csv = os.path.join(script_dir, 'TreeMap_wide_matrix.csv')

# Load data files
# CustomTreeMap acts as the primary/custom comparison, JDK TreeMap as the baseline
custom_df = pd.read_csv(custom_csv, sep=';')
jdk_df = pd.read_csv(jdk_csv, sep=';')

# Clean headers
custom_df.columns = [c.replace('"', '').strip() for c in custom_df.columns]
jdk_df.columns = [c.replace('"', '').strip() for c in jdk_df.columns]

sizes = custom_df['Size'].tolist()
methods = [c for c in custom_df.columns if c != 'Size']

heatmap_data = np.zeros((len(methods), len(sizes)))
text_labels = []

for i, m in enumerate(methods):
    row_labels = []
    for j, size in enumerate(sizes):
        custom_val = custom_df.loc[custom_df['Size'] == size, m].values[0]
        jdk_val = jdk_df.loc[jdk_df['Size'] == size, m].values[0]

        if pd.isna(custom_val) or custom_val == 0: custom_val = 1
        if pd.isna(jdk_val) or jdk_val == 0: jdk_val = 1

        # log2 ratio: positive means CustomTreeMap is faster (JDK TreeMap took more time)
        ratio = np.log2(jdk_val / custom_val)
        heatmap_data[i, j] = ratio

        if jdk_val >= custom_val:
            factor = jdk_val / custom_val
            row_labels.append(f"+{factor:.1f}x" if factor < 100 else f"+{factor:.0f}x")
        else:
            factor = custom_val / jdk_val
            row_labels.append(f"-{factor:.1f}x" if factor < 100 else f"-{factor:.0f}x")
    text_labels.append(row_labels)

text_labels = np.array(text_labels)

# Sort methods by average performance ratio
avg_ratios = np.mean(heatmap_data, axis=1)
sorted_idx = np.argsort(avg_ratios)

heatmap_data = heatmap_data[sorted_idx]
text_labels = text_labels[sorted_idx]
sorted_methods = [methods[idx] for idx in sorted_idx]

# Plotting the heatmap - dynamically sized based on method count
fig, ax = plt.subplots(figsize=(14, max(8, len(methods) * 0.35)), facecolor='none')
ax.set_facecolor('none')

clipped_data = np.clip(heatmap_data, -4.0, 4.0)
cmap = sns.diverging_palette(15, 240, as_cmap=True)

sns.heatmap(clipped_data,
            annot=text_labels,
            fmt="",
            cmap=cmap,
            center=0,
            xticklabels=sizes,
            yticklabels=sorted_methods,
            ax=ax,
            cbar_kws={
                'label': '← JDK TreeMap Faster  |  Relative Speedup Scale (Clipped at 16x)  |  CustomTreeMap Faster →'},
            linewidths=0.5,
            linecolor='#444444',
            annot_kws={'size': 7, 'weight': 'bold'})

ax.set_title(
    'CustomTreeMap vs JDK TreeMap Performance Comparison Matrix Heatmap\n(Positive/Blue = CustomTreeMap Faster, Negative/Red = JDK TreeMap Faster)',
    color='#ffffff', fontsize=15, fontweight='bold', pad=25)
ax.set_ylabel('TreeMap Methods & Operations', color='#aaaaaa', fontsize=12, labelpad=12)
ax.set_xlabel('Collection Size (Elements)', color='#aaaaaa', fontsize=12, labelpad=12)

ax.tick_params(colors='#ffffff', labelsize=9)
plt.xticks(rotation=45)
plt.yticks(rotation=0)

cbar = ax.collections[0].colorbar
cbar.ax.tick_params(colors='#ffffff', labelsize=9)
cbar.ax.yaxis.label.set_color('#ffffff')
cbar.ax.yaxis.label.set_fontsize(10)

plt.tight_layout()
output_filename = os.path.join(script_dir, 'custom_treemap_performance_heatmap.png')
plt.savefig(output_filename, dpi=300, transparent=True)
plt.close()

print(f"Heatmap saved successfully as {output_filename}")
print("Top 5 relative slower methods for CustomTreeMap on average:")
print(sorted_methods[:5])
print("Top 5 relative faster methods for CustomTreeMap on average:")
print(sorted_methods[-5:])