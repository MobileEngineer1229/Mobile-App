# Mobile UI Development Workflow

## 📋 Table of Contents
1. [UI Architecture Overview](#ui-architecture-overview)
2. [Layout Creation Workflow](#layout-creation-workflow)
3. [Activity Development Workflow](#activity-development-workflow)
4. [Fragment Development Workflow](#fragment-development-workflow)
5. [RecyclerView & Adapters Workflow](#recyclerview--adapters-workflow)
6. [Design System & Styling](#design-system--styling)
7. [Navigation Patterns](#navigation-patterns)
8. [Common UI Components](#common-ui-components)
9. [Resource Management](#resource-management)
10. [Testing UI Components](#testing-ui-components)

---

## 🏗️ UI Architecture Overview

### Project Structure
```
Smart-Home/app/src/main/
├── java/com/smarthome/iot/
│   ├── ui/                    # Activities & Fragments
│   │   ├── MainActivity.java
│   │   ├── SmartSceneActivity.java
│   │   ├── ControlDeviceActivity.java
│   │   ├── fragments/         # Reusable fragments
│   │   ├── adapters/          # RecyclerView adapters
│   │   ├── dialogs/           # Custom dialogs
│   │   └── views/             # Custom views
│   ├── models/                # Data models
│   ├── network/               # API integration
│   └── utils/                 # Utilities
└── res/
    ├── layout/                # XML layouts
    ├── values/                # Resources (colors, strings, dimens, styles)
    ├── drawable/              # Icons & shapes
    └── font/                  # Custom fonts
```

### UI Component Hierarchy
```
Activity
  ├── AppBarLayout (Optional)
  ├── CoordinatorLayout / ConstraintLayout / LinearLayout
  │   ├── NestedScrollView (for scrollable content)
  │   │   └── Content Views
  │   ├── RecyclerView (for lists)
  │   └── FragmentContainerView (for fragments)
  ├── FloatingActionButton (Optional)
  └── BottomNavigationView (Optional)
```

---

## 📐 Layout Creation Workflow

### Step 1: Choose Root Layout

**For Simple Screens:**
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/dark_1">
    <!-- Content -->
</LinearLayout>
```

**For Complex Screens with AppBar:**
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/dark_1">
    
    <com.google.android.material.appbar.AppBarLayout>
        <!-- App Bar Content -->
    </com.google.android.material.appbar.AppBarLayout>
    
    <androidx.core.widget.NestedScrollView>
        <!-- Scrollable Content -->
    </androidx.core.widget.NestedScrollView>
    
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

**For Constraint-Based Layouts:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/dark_1">
    <!-- Constraint-based content -->
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Step 2: Standard App Bar Pattern

```xml
<!-- App Bar -->
<com.google.android.material.appbar.AppBarLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/dark_1"
    android:elevation="0dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="24dp"
        android:paddingEnd="24dp"
        android:paddingTop="16dp"
        android:paddingBottom="12dp">

        <ImageButton
            android:id="@+id/buttonBack"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_arrow_back"
            android:contentDescription="@string/back"
            app:tint="@color/white" />

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/screen_title"
            android:textColor="@color/white"
            android:textSize="20sp"
            android:textStyle="bold"
            android:fontFamily="@font/urbanist"
            android:gravity="center"
            android:layout_marginStart="16dp"
            android:layout_marginEnd="16dp" />

        <ImageButton
            android:id="@+id/buttonMore"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_more_vert"
            android:contentDescription="@string/more_options"
            app:tint="@color/white" />

    </LinearLayout>

</com.google.android.material.appbar.AppBarLayout>
```

### Step 3: Content Area Pattern

```xml
<!-- Content -->
<androidx.core.widget.NestedScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:clipToPadding="false"
    android:paddingBottom="70dp"
    app:layout_behavior="@string/appbar_scrolling_view_behavior">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="24dp"
        android:paddingEnd="24dp"
        android:paddingTop="24dp"
        android:paddingBottom="100dp">
        
        <!-- Your content here -->
        
    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

### Step 4: Bottom Navigation Pattern

```xml
<!-- Bottom Navigation -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottomNavigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    android:background="@color/dark_1"
    android:clickable="true"
    android:focusable="true"
    android:focusableInTouchMode="true"
    android:elevation="16dp"
    style="@style/CustomBottomNavigationView"
    app:itemIconTint="@color/nav_item_color_selector"
    app:itemTextColor="@color/nav_item_color_selector"
    app:labelVisibilityMode="labeled"
    app:itemIconSize="14dp"
    app:itemPaddingTop="4dp"
    app:itemPaddingBottom="16dp"
    app:itemPadding="0dp"
    app:itemTextAppearanceActive="@style/BottomNavTextStyleActive"
    app:itemTextAppearanceInactive="@style/BottomNavTextStyle"
    app:menu="@menu/bottom_navigation" />
```

---

## 🎯 Activity Development Workflow

### Step 1: Create Activity Class

```java
package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

public class MyActivity extends AppCompatActivity {
    private TextView textViewTitle;
    private ImageButton buttonBack;
    private ImageButton buttonMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        setStatusBarColor();
        initializeViews();
        setupClickListeners();
        loadData();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        buttonBack = findViewById(R.id.buttonBack);
        buttonMore = findViewById(R.id.buttonMore);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // Show more options
        });
    }

    private void loadData() {
        // Load data from API or mock
    }
}
```

### Step 2: Standard Activity Patterns

#### Pattern 1: List Screen (with RecyclerView)
```java
private RecyclerView recyclerView;
private MyAdapter adapter;
private List<Item> items;

private void setupRecyclerView() {
    items = new ArrayList<>();
    adapter = new MyAdapter(items);
    adapter.setOnItemClickListener(item -> {
        // Handle item click
    });
    
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(adapter);
}

private void loadData() {
    // Load data
    items = getData();
    adapter.setItems(items);
}
```

#### Pattern 2: Grid Screen (2 columns)
```java
private void setupRecyclerView() {
    GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
    recyclerView.setLayoutManager(gridLayoutManager);
    recyclerView.setAdapter(adapter);
}
```

#### Pattern 3: Screen with Bottom Navigation
```java
private BottomNavigationView bottomNavigation;

private void setupBottomNavigation() {
    bottomNavigation = findViewById(R.id.bottomNavigation);
    bottomNavigation.setOnItemSelectedListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.nav_smart) {
            // Already on this screen
            return true;
        }
        // ... other items
        return false;
    });
    
    bottomNavigation.setSelectedItemId(R.id.nav_smart);
    BottomNavSpacingHelper.removeSpacing(bottomNavigation);
}
```

#### Pattern 4: Screen with FloatingActionButton
```java
private FloatingActionButton fab;

private void setupFAB() {
    fab = findViewById(R.id.fab);
    fab.setOnClickListener(v -> {
        // Navigate to create screen
        Intent intent = new Intent(this, CreateActivity.class);
        startActivityForResult(intent, 100);
    });
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 100 && resultCode == RESULT_OK) {
        // Reload data
        loadData();
    }
}
```

---

## 🧩 Fragment Development Workflow

### Step 1: Create Fragment Class

```java
package com.smarthome.iot.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smarthome.iot.R;

public class MyFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    
    private String param1;

    public static MyFragment newInstance(String param1) {
        MyFragment fragment = new MyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            param1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupControls();
    }

    private void initializeViews(View view) {
        // Find views
    }

    private void setupControls() {
        // Setup click listeners, etc.
    }
}
```

### Step 2: Load Fragment in Activity

```java
// In Activity's onCreate or method
Fragment fragment = MyFragment.newInstance("param_value");
FragmentManager fragmentManager = getSupportFragmentManager();
FragmentTransaction transaction = fragmentManager.beginTransaction();
transaction.replace(R.id.fragmentContainer, fragment);
transaction.commit();
```

---

## 📋 RecyclerView & Adapters Workflow

### Step 1: Create Item Layout

```xml
<!-- item_my_item.xml -->
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardBackgroundColor="@color/dark_4"
    app:cardElevation="0dp"
    android:layout_marginBottom="12dp"
    android:clickable="true"
    android:focusable="true"
    android:focusableInTouchMode="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">

        <ImageView
            android:id="@+id/imageViewIcon"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_device"
            android:tint="@color/primary" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="16dp">

            <TextView
                android:id="@+id/textViewTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Item Title"
                android:textColor="@color/white"
                android:textSize="16sp"
                android:textStyle="bold"
                android:fontFamily="@font/urbanist" />

            <TextView
                android:id="@+id/textViewSubtitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Item Subtitle"
                android:textColor="@color/white_alpha_70"
                android:textSize="14sp"
                android:fontFamily="@font/urbanist"
                android:layout_marginTop="4dp" />

        </LinearLayout>

        <ImageView
            android:id="@+id/imageViewArrow"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@drawable/ic_arrow_forward"
            android:tint="@color/white_alpha_70" />

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

### Step 2: Create Adapter

```java
package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.MyItem;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private List<MyItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MyItem item);
    }

    public MyAdapter() {
        this.items = new ArrayList<>();
    }

    public void setItems(List<MyItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyItem item = items.get(position);
        
        holder.textViewTitle.setText(item.getTitle());
        holder.textViewSubtitle.setText(item.getSubtitle());
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageViewIcon;
        TextView textViewTitle;
        TextView textViewSubtitle;
        ImageView imageViewArrow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewSubtitle = itemView.findViewById(R.id.textViewSubtitle);
            imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
        }
    }
}
```

### Step 3: Use Adapter in Activity

```java
private void setupRecyclerView() {
    recyclerView = findViewById(R.id.recyclerView);
    
    MyAdapter adapter = new MyAdapter();
    adapter.setOnItemClickListener(item -> {
        // Handle click
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    });
    
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(adapter);
    
    // Load data
    List<MyItem> items = loadItems();
    adapter.setItems(items);
}
```

---

## 🎨 Design System & Styling

### Color System

**Primary Colors:**
- `@color/primary` - #405FF2 (Blue)
- `@color/primary_dark` - #334366

**Status Colors:**
- `@color/success` - #12D18E (Green)
- `@color/error` - #F75555 (Red)
- `@color/warning` - #FACC15 (Yellow)
- `@color/info` - #405FF2 (Blue)

**Dark Theme Colors:**
- `@color/dark_1` - #181A20 (Background)
- `@color/dark_2` - #1E2025
- `@color/dark_3` - #1F222A
- `@color/dark_4` - #262A35 (Cards)
- `@color/dark_5` - #35383F

**Text Colors:**
- `@color/white` - Primary text
- `@color/white_alpha_70` - Secondary text
- `@color/white_alpha_50` - Tertiary text

### Typography

**Font Family:**
- All text elements MUST use: `android:fontFamily="@font/urbanist"`

**Text Sizes:**
- Small: `12sp` - Secondary text, labels
- Medium: `14sp` - Body text
- Normal: `16sp` - Primary body text
- Large: `18sp` - Section headers
- XLarge: `20sp` - Screen titles
- XXLarge: `24sp+` - Hero text

**Text Styles:**
```xml
<!-- Regular -->
android:textSize="14sp"
android:fontFamily="@font/urbanist"

<!-- Bold -->
android:textSize="16sp"
android:textStyle="bold"
android:fontFamily="@font/urbanist"
```

### Spacing System

**Padding/Margin:**
- Small: `8dp` - Tight spacing
- Medium: `12dp` - Standard spacing
- Large: `16dp` - Card padding
- XLarge: `24dp` - Screen padding, section spacing
- XXLarge: `32dp` - Major section spacing

**Card Styling:**
```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardBackgroundColor="@color/dark_4"
    app:cardElevation="0dp"
    android:layout_marginBottom="12dp">
```

### Button Styles

**Primary Button:**
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button.NoCaps"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:backgroundTint="@color/primary"
    android:textColor="@color/white"
    android:textSize="14sp"
    android:fontFamily="@font/urbanist"
    app:cornerRadius="12dp" />
```

**Text Button:**
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button.TextButton.NoCaps"
    android:textColor="@color/white"
    android:fontFamily="@font/urbanist" />
```

**Outlined Button:**
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button.OutlinedButton.NoCaps"
    android:textColor="@color/white"
    android:fontFamily="@font/urbanist" />
```

---

## 🧭 Navigation Patterns

### Pattern 1: Simple Navigation

```java
// Navigate to another activity
Intent intent = new Intent(this, TargetActivity.class);
intent.putExtra("key", value);
startActivity(intent);
```

### Pattern 2: Navigation with Result

```java
// Start activity for result
Intent intent = new Intent(this, TargetActivity.class);
startActivityForResult(intent, REQUEST_CODE);

// Handle result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        // Handle result
        if (data != null) {
            String result = data.getStringExtra("result_key");
        }
    }
}

// In TargetActivity, set result
Intent resultIntent = new Intent();
resultIntent.putExtra("result_key", resultValue);
setResult(RESULT_OK, resultIntent);
finish();
```

### Pattern 3: Navigation with Clear Top

```java
// Navigate and clear back stack
Intent intent = new Intent(this, MainActivity.class);
intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
startActivity(intent);
finish();
```

### Pattern 4: Bottom Navigation Pattern

```java
private void setupBottomNavigation() {
    BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
    
    bottomNav.setOnItemSelectedListener(item -> {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.nav_smart) {
            Intent intent = new Intent(this, SmartSceneActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        // ... other items
        
        return false;
    });
    
    // Set current item as selected
    bottomNav.setSelectedItemId(R.id.nav_smart);
    BottomNavSpacingHelper.removeSpacing(bottomNav);
}
```

---

## 🧩 Common UI Components

### 1. Device Card Pattern

```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardBackgroundColor="@color/dark_4"
    android:clickable="true"
    android:focusable="true"
    android:focusableInTouchMode="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">

        <ImageView
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_device"
            android:tint="@color/primary" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="16dp">

            <TextView
                android:text="Device Name"
                android:textColor="@color/white"
                android:textSize="16sp"
                android:textStyle="bold"
                android:fontFamily="@font/urbanist" />

            <TextView
                android:text="Room Name • Wi-Fi"
                android:textColor="@color/white_alpha_70"
                android:textSize="12sp"
                android:fontFamily="@font/urbanist"
                android:layout_marginTop="4dp" />

        </LinearLayout>

        <com.google.android.material.switchmaterial.SwitchMaterial
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:thumbTint="@color/primary"
            android:trackTint="@color/dark_5" />

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

### 2. Category Card Pattern

```xml
<androidx.cardview.widget.CardView
    android:layout_width="0dp"
    android:layout_height="120dp"
    android:layout_weight="1"
    app:cardCornerRadius="12dp"
    app:cardBackgroundColor="@color/dark_4"
    android:clickable="true"
    android:focusable="true"
    android:foreground="?attr/selectableItemBackground">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="16dp">

        <ImageView
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_sun"
            android:tint="@color/orange" />

        <TextView
            android:text="Category"
            android:textColor="@color/white"
            android:textSize="14sp"
            android:textStyle="bold"
            android:fontFamily="@font/urbanist"
            android:layout_marginTop="8dp" />

        <TextView
            android:text="12 items"
            android:textColor="@color/white_alpha_70"
            android:textSize="12sp"
            android:fontFamily="@font/urbanist"
            android:layout_marginTop="4dp" />

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

### 3. Tab Button Pattern

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:background="@color/dark_4"
    android:padding="4dp">

    <com.google.android.material.button.MaterialButton
        android:id="@+id/buttonTab1"
        style="@style/Widget.MaterialComponents.Button.NoCaps"
        android:layout_width="0dp"
        android:layout_height="40dp"
        android:layout_weight="1"
        android:backgroundTint="@color/primary"
        android:text="Tab 1"
        android:textColor="@color/white"
        android:textSize="12sp"
        android:fontFamily="@font/urbanist"
        app:cornerRadius="8dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/buttonTab2"
        style="@style/Widget.MaterialComponents.Button.NoCaps"
        android:layout_width="0dp"
        android:layout_height="40dp"
        android:layout_weight="1"
        android:backgroundTint="@android:color/transparent"
        android:text="Tab 2"
        android:textColor="@color/white_alpha_70"
        android:textSize="12sp"
        android:fontFamily="@font/urbanist"
        app:cornerRadius="8dp" />

</LinearLayout>
```

### 4. Input Field Pattern

```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/field_label"
    app:hintTextColor="@color/white_alpha_70"
    app:boxStrokeColor="@color/primary"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/white"
        android:textColorHint="@color/white_alpha_70"
        android:fontFamily="@font/urbanist"
        android:inputType="text" />

</com.google.android.material.textfield.TextInputLayout>
```

### 5. Switch/Toggle Pattern

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:text="Setting Name"
            android:textColor="@color/white"
            android:textSize="16sp"
            android:fontFamily="@font/urbanist" />

        <TextView
            android:text="Setting description"
            android:textColor="@color/white_alpha_70"
            android:textSize="12sp"
            android:fontFamily="@font/urbanist"
            android:layout_marginTop="4dp" />

    </LinearLayout>

    <com.google.android.material.switchmaterial.SwitchMaterial
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:thumbTint="@color/primary"
        android:trackTint="@color/dark_5" />

</LinearLayout>
```

---

## 📦 Resource Management

### String Resources

**Always extract strings to resources:**
```xml
<!-- res/values/strings.xml -->
<string name="screen_title">Screen Title</string>
<string name="button_label">Button Label</string>
```

**Usage:**
```java
textView.setText(getString(R.string.screen_title));
button.setText(getString(R.string.button_label));
```

### Drawable Resources

**Icons:**
- Use vector drawables (`ic_*.xml`) in `res/drawable/`
- Standard size: `24dp` for icons
- Tint: `app:tint="@color/white"` or specific color

**Backgrounds:**
- Card backgrounds: `bg_rounded_dark_4.xml`
- Button backgrounds: Use MaterialButton with `backgroundTint`
- Custom shapes: Create in `res/drawable/`

### Dimension Resources

**Use predefined dimensions:**
```xml
<!-- From res/values/dimens.xml -->
android:padding="@dimen/spacing_16"
android:textSize="@dimen/text_size_medium"
android:layout_marginBottom="@dimen/spacing_24"
```

---

## 🧪 Testing UI Components

### Manual Testing Checklist

1. **Layout Testing:**
   - [ ] Test on different screen sizes (phone, tablet)
   - [ ] Test in portrait and landscape
   - [ ] Verify text doesn't overflow
   - [ ] Check spacing and alignment
   - [ ] Verify dark theme colors

2. **Interaction Testing:**
   - [ ] All buttons are clickable
   - [ ] RecyclerView items are clickable
   - [ ] Navigation works correctly
   - [ ] Bottom navigation works
   - [ ] FAB works (if present)

3. **Data Testing:**
   - [ ] Empty state displays correctly
   - [ ] Loading state (if applicable)
   - [ ] Error state handling
   - [ ] Mock data displays correctly

4. **Accessibility:**
   - [ ] Content descriptions set
   - [ ] Text is readable
   - [ ] Touch targets are adequate (min 48dp)

---

## 🎯 Common UI Patterns in Project

### Pattern 1: List Screen with Empty State

```java
private void updateUI() {
    if (items.isEmpty()) {
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
    } else {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        adapter.setItems(items);
    }
}
```

### Pattern 2: Loading State

```java
private ProgressBar progressBar;
private RecyclerView recyclerView;

private void showLoading() {
    progressBar.setVisibility(View.VISIBLE);
    recyclerView.setVisibility(View.GONE);
}

private void hideLoading() {
    progressBar.setVisibility(View.GONE);
    recyclerView.setVisibility(View.VISIBLE);
}
```

### Pattern 3: Error Handling

```java
private void handleError(String errorMessage) {
    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    // Optionally show error state UI
}
```

### Pattern 4: Tab Switching

```java
private String currentTab = "tab1";

private void switchTab(String tab) {
    currentTab = tab;
    updateTabButtons();
    loadDataForTab(tab);
}

private void updateTabButtons() {
    if ("tab1".equals(currentTab)) {
        buttonTab1.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        buttonTab1.setTextColor(ContextCompat.getColor(this, R.color.white));
        buttonTab2.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        buttonTab2.setTextColor(ContextCompat.getColor(this, R.color.white_alpha_70));
    } else {
        // Reverse
    }
}
```

---

## 🔧 Common Issues & Solutions

### Issue 1: RecyclerView Not Showing Data

**Solution:**
```java
// Ensure adapter is set before setting data
recyclerView.setAdapter(adapter);
adapter.setItems(items);

// Force refresh
recyclerView.post(() -> {
    adapter.notifyDataSetChanged();
    recyclerView.invalidate();
    recyclerView.requestLayout();
});
```

### Issue 2: Click Events Not Working

**Solution:**
```xml
<!-- In layout -->
<CardView
    android:clickable="true"
    android:focusable="true"
    android:focusableInTouchMode="true">
    
    <!-- Child views should NOT be clickable -->
    <LinearLayout
        android:clickable="false"
        android:focusable="false">
        <!-- Content -->
    </LinearLayout>
</CardView>
```

### Issue 3: Bottom Navigation Not Clickable

**Solution:**
```xml
<!-- In layout -->
<BottomNavigationView
    android:clickable="true"
    android:focusable="true"
    android:focusableInTouchMode="true"
    android:elevation="16dp" />
```

```java
// In code
bottomNavigation.setClickable(true);
bottomNavigation.setFocusable(true);
bottomNavigation.setEnabled(true);
```

### Issue 4: Text Not Using Custom Font

**Solution:**
```xml
<!-- MUST add to ALL text elements -->
android:fontFamily="@font/urbanist"
```

### Issue 5: Status Bar Color Not Applied

**Solution:**
```java
private void setStatusBarColor() {
    Window window = getWindow();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
    }
}
```

---

## 📝 UI Development Checklist

### Before Creating New Screen

- [ ] Check if similar screen exists (reuse patterns)
- [ ] Plan navigation flow
- [ ] Design layout structure
- [ ] Identify reusable components

### During Development

- [ ] Create layout XML file
- [ ] Create Activity/Fragment class
- [ ] Apply theme in `onCreate` (before `super.onCreate`)
- [ ] Set status bar color
- [ ] Initialize all views
- [ ] Setup click listeners
- [ ] Add to AndroidManifest (if Activity)
- [ ] Extract all strings to resources
- [ ] Use predefined colors and dimensions
- [ ] Apply Urbanist font to all text

### After Development

- [ ] Test on different screen sizes
- [ ] Test navigation flow
- [ ] Verify dark theme
- [ ] Check for memory leaks
- [ ] Verify accessibility
- [ ] Test with mock data
- [ ] Update documentation if needed

---

## 🎨 Design Guidelines

### Spacing Rules

- **Screen Padding**: `24dp` (start, end, top)
- **Card Padding**: `16dp`
- **Card Margin**: `12dp` bottom
- **Item Spacing**: `8dp` - `12dp`
- **Section Spacing**: `24dp`

### Card Styling Rules

- **Corner Radius**: `12dp`
- **Background**: `@color/dark_4`
- **Elevation**: `0dp` (flat design)
- **Margin Bottom**: `12dp`

### Button Rules

- **Height**: `56dp` for primary buttons
- **Corner Radius**: `12dp` for regular, `1000dp` for pill
- **Text**: No caps, Urbanist font
- **Padding**: `24dp` horizontal for text buttons

### Icon Rules

- **Size**: `24dp` standard, `40dp` for app bar, `48dp` for large
- **Tint**: `@color/white` or specific color
- **Padding**: `16dp` around icons in buttons

---

## 🔄 Complete UI Development Example

### Example: Creating a New List Screen

#### Step 1: Create Layout
```xml
<!-- activity_my_list.xml -->
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <!-- App Bar -->
    <AppBarLayout>...</AppBarLayout>
    
    <!-- Content -->
    <NestedScrollView>
        <LinearLayout>
            <RecyclerView
                android:id="@+id/recyclerView"
                android:layout_width="match_parent"
                android:layout_height="wrap_content" />
        </LinearLayout>
    </NestedScrollView>
    
    <!-- Bottom Navigation -->
    <BottomNavigationView>...</BottomNavigationView>
</CoordinatorLayout>
```

#### Step 2: Create Item Layout
```xml
<!-- item_my_item.xml -->
<CardView>
    <LinearLayout>
        <!-- Item content -->
    </LinearLayout>
</CardView>
```

#### Step 3: Create Model
```java
public class MyItem {
    private Integer id;
    private String name;
    // ... getters/setters
}
```

#### Step 4: Create Adapter
```java
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    // ... adapter implementation
}
```

#### Step 5: Create Activity
```java
public class MyListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_list);
        
        setStatusBarColor();
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadData();
    }
    
    // ... implementation
}
```

#### Step 6: Add to AndroidManifest
```xml
<activity
    android:name=".ui.MyListActivity"
    android:exported="false"
    android:theme="@style/Theme.SmartHome" />
```

---

## 📚 UI Component Library

### Standard Components Available

1. **Cards**: `CardView` with `dark_4` background
2. **Buttons**: `MaterialButton` with various styles
3. **Input Fields**: `TextInputLayout` with `TextInputEditText`
4. **Switches**: `SwitchMaterial` with primary tint
5. **Bottom Sheets**: `BottomSheetDialog`
6. **Dialogs**: `AlertDialog` or custom dialogs
7. **Progress Indicators**: `ProgressBar` or `CircularProgressView`
8. **Lists**: `RecyclerView` with various layout managers

---

## 🎯 Best Practices

1. **Always use Material Design components**
2. **Extract all strings to resources**
3. **Use predefined colors and dimensions**
4. **Apply Urbanist font to ALL text elements**
5. **Handle empty states**
6. **Handle loading states**
7. **Handle error states**
8. **Test on multiple screen sizes**
9. **Ensure accessibility (content descriptions)**
10. **Follow dark theme consistently**

---

**Last Updated**: December 2024
**Version**: 1.0.0
