# Iconify Integration Guide

This guide explains how to use Iconify icons (from [iconify.design](https://iconify.design)) in the Android Smart Home app to match the Figma design.

## Overview

Iconify is a unified icon framework with 200+ icon sets and 250,000+ icons. Since Figma uses Iconify icons, we need to convert them to Android VectorDrawable format for use in the app.

## Method 1: Using Android Studio Vector Asset Studio (Recommended)

This is the easiest method for individual icons:

### Steps:

1. **Find the Icon on Iconify**
   - Go to [iconify.design](https://iconify.design)
   - Search for the icon you need (e.g., "mdi:home", "material-symbols:settings")
   - Click on the icon to view details

2. **Download SVG**
   - Click the "Download" button
   - Select "SVG" format
   - Save the file locally

3. **Import to Android Studio**
   - Right-click on `app/src/main/res/drawable` folder
   - Select **New → Vector Asset**
   - Choose **Local file (SVG, PSD)**
   - Click the folder icon and select your downloaded SVG file
   - Set the icon name (e.g., `ic_home_iconify`)
   - Click **Next** and **Finish**

4. **Use in Layouts**
   ```xml
   <ImageView
       android:layout_width="24dp"
       android:layout_height="24dp"
       android:src="@drawable/ic_home_iconify"
       app:tint="@color/white" />
   ```

## Method 2: Direct SVG to VectorDrawable Conversion

For bulk conversion or automation:

1. **Download SVG from Iconify**
   - Visit: `https://api.iconify.design/{icon-set}/{icon-name}.svg`
   - Example: `https://api.iconify.design/mdi/home.svg`
   - Save the SVG file

2. **Convert using Vector Asset Studio** (same as Method 1)
   - Or use online tools like [SVG to VectorDrawable Converter](https://inloop.github.io/svg2android/)

3. **Place in `res/drawable/`**
   - Name it following the pattern: `ic_{icon_name}.xml`
   - Example: `ic_home_mdi.xml`

## Method 3: Using Iconify API at Runtime (Advanced)

For dynamic icons or when you need many icons without bundling:

⚠️ **Note**: This requires network access and SVG rendering library.

1. **Add SVG rendering dependency** (if needed):
   ```groovy
   // Option: Use Coil with SVG support (for Compose)
   // Or use AndroidSVG library for traditional views
   ```

2. **Fetch icon from Iconify API**:
   ```java
   String iconUrl = "https://api.iconify.design/mdi/home.svg";
   // Load using your image loading library
   ```

## Icon Naming Convention

To match Figma design and maintain consistency:

- **Format**: `ic_{name}_{icon_set}.xml`
- **Examples**:
  - `ic_home_mdi.xml` (Material Design Icons)
  - `ic_settings_material.xml` (Material Symbols)
  - `ic_user_heroicons.xml` (Heroicons)

## Common Icon Sets in Figma

Based on typical Figma usage, these icon sets are commonly used:

1. **Material Design Icons (mdi)**: `mdi:home`, `mdi:settings`, `mdi:account`
2. **Material Symbols (material-symbols)**: `material-symbols:home`, `material-symbols:settings`
3. **Heroicons (heroicons)**: `heroicons:home`, `heroicons:user`
4. **Tabler Icons (tabler)**: `tabler:home`, `tabler:settings`

## Finding Icons from Figma

1. **In Figma**:
   - Select the icon element
   - Check the properties panel for icon name
   - Or check the layer name (often contains iconify reference)

2. **Search on Iconify**:
   - Go to [iconify.design](https://iconify.design)
   - Search using the icon name from Figma
   - Verify it matches visually

## Best Practices

1. **Consistency**: Use the same icon set throughout the app when possible
2. **Size**: Standard icon size is 24dp (adjust as needed)
3. **Tinting**: Use `app:tint` for color changes instead of hardcoding colors in the vector
4. **Naming**: Follow the naming convention for easy identification
5. **Organization**: Group related icons in the same icon set

## Example: Converting a Figma Icon

**Figma Icon**: `mdi:home-outline`

1. Visit: `https://iconify.design/icon-sets/mdi/home-outline.html`
2. Download SVG
3. Import to Android Studio as `ic_home_outline_mdi.xml`
4. Use in layout:
   ```xml
   <ImageView
       android:layout_width="24dp"
       android:layout_height="24dp"
       android:src="@drawable/ic_home_outline_mdi"
       app:tint="@color/white" />
   ```

## Troubleshooting

### SVG Not Converting Properly
- Some complex SVG features aren't supported in VectorDrawable
- Simplify the SVG or use a different icon variant
- Check Android Studio's conversion warnings

### Icons Not Showing
- Ensure `vectorDrawables.useSupportLibrary = true` in `build.gradle`
- Use `app:srcCompat` instead of `android:src` for ImageView
- Check that the drawable resource exists

### Size Issues
- VectorDrawables scale automatically
- Set explicit `layout_width` and `layout_height`
- Use `app:tint` for color changes

## Resources

- [Iconify Website](https://iconify.design)
- [Iconify Icon Sets](https://icon-sets.iconify.design)
- [Android VectorDrawable Guide](https://developer.android.com/develop/ui/views/graphics/vector-drawable-resources)
- [Vector Asset Studio](https://developer.android.com/studio/write/vector-asset-studio)

## Quick Reference: Iconify API URLs

- **SVG Download**: `https://api.iconify.design/{icon-set}/{icon-name}.svg`
- **JSON Data**: `https://api.iconify.design/{icon-set}/{icon-name}.json`
- **Icon Info**: `https://iconify.design/icon-sets/{icon-set}/{icon-name}.html`

---

**Note**: This integration method ensures icons match the Figma design exactly while maintaining Android best practices for vector drawables.
