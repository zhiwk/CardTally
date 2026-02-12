package com.example.cardtally;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cardtally.util.ThemeHelper;

public class SettingsFragment extends Fragment {
    private RadioGroup radioGroupTheme;
    private RadioButton radioLight;
    private RadioButton radioDark;
    private RadioButton radioSystem;

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        radioGroupTheme = view.findViewById(R.id.radio_group_theme);
        radioLight = view.findViewById(R.id.radio_light);
        radioDark = view.findViewById(R.id.radio_dark);
        radioSystem = view.findViewById(R.id.radio_system);

        loadCurrentTheme();

        radioGroupTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int themeMode;
                if (checkedId == R.id.radio_light) {
                    themeMode = ThemeHelper.THEME_LIGHT;
                } else if (checkedId == R.id.radio_dark) {
                    themeMode = ThemeHelper.THEME_DARK;
                } else {
                    themeMode = ThemeHelper.THEME_SYSTEM;
                }
                
                ThemeHelper.saveTheme(getContext(), themeMode);
                Toast.makeText(getContext(), "主题已更改，重启应用后生效", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadCurrentTheme() {
        int currentTheme = ThemeHelper.getTheme(getContext());
        switch (currentTheme) {
            case ThemeHelper.THEME_LIGHT:
                radioLight.setChecked(true);
                break;
            case ThemeHelper.THEME_DARK:
                radioDark.setChecked(true);
                break;
            case ThemeHelper.THEME_SYSTEM:
                radioSystem.setChecked(true);
                break;
        }
    }
}
