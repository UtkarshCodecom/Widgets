package com.desire.widget.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.R;

public class AdminFragment extends Fragment {
    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupDashboardCards(view);
        setupNavigation(view);
        viewModel.loadAll();
    }

    private void setupDashboardCards(View view) {
        view.findViewById(R.id.widgets_management_card).setOnClickListener(v ->
                openSection("Widgets"));
        view.findViewById(R.id.themes_management_card).setOnClickListener(v ->
                openSection("Themes"));
        view.findViewById(R.id.categories_management_card).setOnClickListener(v ->
                openSection("Categories"));
        view.findViewById(R.id.announcements_card).setOnClickListener(v ->
                openSection("Announcements"));
        view.findViewById(R.id.offers_card).setOnClickListener(v ->
                openSection("Offers"));
        view.findViewById(R.id.feature_flags_card).setOnClickListener(v ->
                openSection("FeatureFlags"));
        view.findViewById(R.id.analytics_card).setOnClickListener(v ->
                openSection("Analytics"));
        view.findViewById(R.id.app_config_card).setOnClickListener(v ->
                openSection("AppConfig"));
    }

    private void setupNavigation(View view) {
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
