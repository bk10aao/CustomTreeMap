import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# 1. Load the benchmark data files (long format: Benchmark;Size;Score (ns/op))
custom_map_raw = pd.read_csv('CustomTreeMap_jmh_performance.csv', sep=';')
tree_map_raw = pd.read_csv('TreeMap_jmh_performance.csv', sep=';')

# 2. Pivot each file from long format into a wide Size x Benchmark matrix
custom_map_df = custom_map_raw.pivot(index='Size', columns='Benchmark', values='Score (ns/op)').reset_index()
tree_map_df = tree_map_raw.pivot(index='Size', columns='Benchmark', values='Score (ns/op)').reset_index()

# 3. Extract methods and sizes (methods are shared between both files)
methods = [col for col in custom_map_df.columns if col != 'Size']
sizes = sorted(custom_map_df['Size'].tolist())

# 4. Construct the relative performance matrix (Log2 ratios)
heatmap_data = np.zeros((len(methods), len(sizes)))
text_labels = []
for i, m in enumerate(methods):
    row_labels = []
    for j, size in enumerate(sizes):
        c_val = custom_map_df.loc[custom_map_df['Size'] == size, m].values[0]
        j_val = tree_map_df.loc[tree_map_df['Size'] == size, m].values[0]
        # Prevent division by zero errors
        if c_val == 0: c_val = 1
        if j_val == 0: j_val = 1
        # Log2 ratio calculation (Positive values indicate CustomTreeMap is faster)
        ratio = np.log2(j_val / c_val)
        heatmap_data[i, j] = ratio
        if j_val > c_val:
            factor = j_val / c_val
            row_labels.append(f"+{factor:.1f}x")
        else:
            factor = c_val / j_val
            row_labels.append(f"-{factor:.1f}x")
    text_labels.append(row_labels)
text_labels = np.array(text_labels)

# 5. Sort methods from top to bottom by their geometric trends
avg_ratios = np.mean(heatmap_data, axis=1)
sorted_idx = np.argsort(avg_ratios)
heatmap_data = heatmap_data[sorted_idx]
text_labels = text_labels[sorted_idx]
sorted_methods = [methods[idx] for idx in sorted_idx]

# 6. Initialize figure with a fully transparent background
fig, ax = plt.subplots(figsize=(15, 13), facecolor='none')
ax.set_facecolor('none')

# 7. Clip the log2 ratios to [-4.0, 4.0] (maps color range bounds to 16x variation)
clipped_heatmap_data = np.clip(heatmap_data, -4.0, 4.0)

# 8. Create a custom divergent colormap (Red = TreeMap Faster, Blue = CustomTreeMap Faster)
cmap = sns.diverging_palette(15, 240, as_cmap=True)

# 9. Render the Seaborn Heatmap
sns.heatmap(clipped_heatmap_data,
            annot=text_labels,
            fmt="",
            cmap=cmap,
            center=0,
            xticklabels=sizes,
            yticklabels=sorted_methods,
            ax=ax,
            cbar_kws={'label': '← JDK Faster  |  Relative Speedup Scale (Clipped at 16x)  |  Custom Faster →'},
            linewidths=0.8,
            linecolor='#555555',
            annot_kws={'size': 10, 'weight': 'bold', 'color': '#000000'})

# 10. Format Title, Labels, and Colorbar styling to match transparency guidelines
ax.set_title('CustomTreeMap vs JDK (Positive = Custom Faster, Negative = JDK Faster)',
             color='#ffffff', fontsize=16, fontweight='bold', pad=20)
ax.set_ylabel('Map Interface Methods', color='#aaaaaa', fontsize=13, labelpad=10)
ax.set_xlabel('Collection Size (Elements)', color='#aaaaaa', fontsize=13, labelpad=10)
ax.tick_params(colors='#ffffff', labelsize=11)
plt.xticks(rotation=45, color='#ffffff')
plt.yticks(rotation=0, color='#ffffff')

# Style colorbar text and outline to match dark/transparent setups
cbar = ax.collections[0].colorbar
cbar.ax.tick_params(colors='#ffffff', labelsize=10)
cbar.ax.yaxis.label.set_color('#ffffff')
cbar.ax.yaxis.label.set_fontsize(12)
cbar.outline.set_edgecolor('#555555')
for text in cbar.ax.yaxis.get_ticklabels():
    text.set_color('#ffffff')

# 11. Compute tight layouts and save the file
plt.tight_layout()
plt.savefig('heatmap.png', dpi=300, transparent=True, facecolor='none', edgecolor='none')
plt.close()