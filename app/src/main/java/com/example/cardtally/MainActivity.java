package com.example.cardtally;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cardtally.util.QuickAddHelper;
import com.example.cardtally.util.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            Fragment initialFragment;
            if (QuickAddHelper.getQuickAdd(this)) {
                initialFragment = new AddRecordFragment();
            } else {
                initialFragment = new HomeFragment();
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, initialFragment)
                    .commit();
        }
    }

    private void applyTheme() {
        int themeMode = ThemeHelper.getTheme(this);
        int themeResId = ThemeHelper.getThemeResId(themeMode);
        setTheme(themeResId);
    }

    private BottomNavigationView.OnItemSelectedListener navListener = new BottomNavigationView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_statistics) {
                selectedFragment = new StatisticsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();

            return true;
        }
    };
}
