package com.desire.widget.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.R;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminFragment extends Fragment {
    private AdminViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupCardIcons(view);
        setupDashboardCards(view);
        setupNavigation(view);
        viewModel.loadAll();
    }

    private void setupCardIcons(View view) {
        setCard(view, R.id.widgets_management_card, "Widgets", R.drawable.ic_widgets);
        setCard(view, R.id.categories_management_card, "Categories", R.drawable.ic_widgets);
        setCard(view, R.id.themes_management_card, "Themes", R.drawable.ic_customize);
        setCard(view, R.id.announcements_card, "Announcements", R.drawable.ic_premium);
        setCard(view, R.id.offers_card, "Offers", R.drawable.ic_premium);
        setCard(view, R.id.analytics_card, "Analytics", R.drawable.ic_widgets);
        setCard(view, R.id.feature_flags_card, "Feature Flags", R.drawable.ic_settings);
        setCard(view, R.id.app_config_card, "App Config", R.drawable.ic_settings);
    }

    private void setCard(View root, int cardId, String title, int iconRes) {
        View card = root.findViewById(cardId);
        if (card != null) {
            TextView tv = card.findViewById(R.id.card_title);
            ImageView iv = card.findViewById(R.id.card_icon);
            if (tv != null) tv.setText(title);
            if (iv != null) iv.setImageResource(iconRes);
        }
    }

    private void setupDashboardCards(View view) {
        view.findViewById(R.id.widgets_management_card).setOnClickListener(v ->
                openSection("Widgets"));
        view.findViewById(R.id.categories_management_card).setOnClickListener(v ->
                openSection("Categories"));
        view.findViewById(R.id.themes_management_card).setOnClickListener(v ->
                openSection("Themes"));
        view.findViewById(R.id.announcements_card).setOnClickListener(v ->
                openSection("Announcements"));
        view.findViewById(R.id.offers_card).setOnClickListener(v ->
                openSection("Offers"));
        view.findViewById(R.id.analytics_card).setOnClickListener(v ->
                openSection("Analytics"));
        view.findViewById(R.id.feature_flags_card).setOnClickListener(v ->
                openSection("FeatureFlags"));
        view.findViewById(R.id.app_config_card).setOnClickListener(v ->
                openSection("AppConfig"));
    }

    private void setupNavigation(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.admin_toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void openSection(String section) {
        AdminDetailFragment detailFragment = AdminDetailFragment.newInstance(section);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment, "AdminDetail")
                .addToBackStack(null)
                .commit();
    }
}
