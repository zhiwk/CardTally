package com.example.cardtally;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.cardtally.util.QuickAddHelper;
import com.example.cardtally.util.ThemeHelper;

public class SettingsFragment extends Fragment {
    private CardView cardQuickAdd;
    private Switch switchQuickAdd;
    private CardView cardCategory;
    private CardView cardTheme;
    private TextView textCurrentTheme;

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        cardQuickAdd = view.findViewById(R.id.card_quick_add);
        switchQuickAdd = view.findViewById(R.id.switch_quick_add);
        cardCategory = view.findViewById(R.id.card_category);
        cardTheme = view.findViewById(R.id.card_theme);
        textCurrentTheme = view.findViewById(R.id.text_current_theme);

        switchQuickAdd.setChecked(QuickAddHelper.getQuickAdd(getContext()));

        updateCurrentThemeText();

        switchQuickAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuickAddHelper.saveQuickAdd(getContext(), switchQuickAdd.isChecked());
            }
        });

        cardCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CategoryManageFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        cardTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ThemeSettingsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentThemeText();
        switchQuickAdd.setChecked(QuickAddHelper.getQuickAdd(getContext()));
    }

    private void updateCurrentThemeText() {
        int currentTheme = ThemeHelper.getTheme(getContext());
        String themeName;
        switch (currentTheme) {
            case ThemeHelper.THEME_LIGHT:
                themeName = "浅色主题";
                break;
            case ThemeHelper.THEME_DARK:
                themeName = "深色主题";
                break;
            case ThemeHelper.THEME_SYSTEM:
                themeName = "跟随系统";
                break;
            default:
                themeName = "浅色主题";
        }
        textCurrentTheme.setText(themeName);
    }
}
