package com.desire.widget.ui.widgets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.ui.adapters.CategoryAdapter;
import com.desire.widget.ui.adapters.WidgetAdapter;

import java.util.ArrayList;
import java.util.List;

public class WidgetsFragment extends Fragment {
    private WidgetsViewModel viewModel;
    private WidgetAdapter widgetAdapter;
    private CategoryAdapter categoryAdapter;
    private EditText searchInput;
    private ImageView favoritesToggle;
    private RecyclerView categoryRecycler;
    private RecyclerView widgetRecycler;
    private View emptyState;
    private View loadingView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_widgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WidgetsViewModel.class);

        searchInput = view.findViewById(R.id.search_input);
        favoritesToggle = view.findViewById(R.id.favorites_toggle);
        categoryRecycler = view.findViewById(R.id.category_recycler);
        widgetRecycler = view.findViewById(R.id.widget_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        loadingView = view.findViewById(R.id.loading_view);

        View createBtn = view.findViewById(R.id.create_widget_btn);
        if (createBtn != null) createBtn.setOnClickListener(v -> openNewInStudio());

        setupSearch();
        setupFavoritesToggle();
        setupCategoryRecycler();
        setupWidgetRecycler();
        observeData();
        viewModel.refresh();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });
    }

    private void setupFavoritesToggle() {
        favoritesToggle.setOnClickListener(v -> {
            boolean showingFavorites = !favoritesToggle.isSelected();
            favoritesToggle.setSelected(showingFavorites);
            viewModel.setShowFavoritesOnly(showingFavorites);
        });
    }

    private void setupCategoryRecycler() {
        categoryAdapter = new CategoryAdapter();
        categoryAdapter.setOnCategoryClickListener((category, position) -> {
            categoryAdapter.setSelectedPosition(position);
            viewModel.setSelectedCategory(category.getId());
        });
        categoryRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);
    }

    private void setupWidgetRecycler() {
        widgetAdapter = new WidgetAdapter(2);
        widgetAdapter.setOnWidgetClickListener(widget ->
                WidgetInstaller.installToHome(requireView(), widget));
        widgetAdapter.setOnWidgetLongClickListener(this::openInStudio);
        widgetAdapter.setOnFavoriteClickListener((widget, isFavorite) -> {
            viewModel.toggleFavorite(widget.getId(), isFavorite);
        });
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return widgetAdapter.getSpanSize(position);
            }
        });
        widgetRecycler.setLayoutManager(layoutManager);
        widgetRecycler.setAdapter(widgetAdapter);
    }

    private void openNewInStudio() {
        startActivity(new android.content.Intent(
                requireContext(), com.desire.widget.ui.studio.StudioActivity.class));
    }

    private void openInStudio(WidgetEntity widget) {
        android.content.Intent intent = new android.content.Intent(
                requireContext(), com.desire.widget.ui.studio.StudioActivity.class);
        intent.putExtra(com.desire.widget.ui.studio.StudioActivity.EXTRA_SPEC_JSON, widget.getSpecJson());
        intent.putExtra(com.desire.widget.ui.studio.StudioActivity.EXTRA_NAME, widget.getName());
        intent.putExtra(com.desire.widget.ui.studio.StudioActivity.EXTRA_SIZE, widget.getWidgetSize());
        intent.putExtra(com.desire.widget.ui.studio.StudioActivity.EXTRA_WIDGET_ID, widget.getId());
        startActivity(intent);
    }

    private void observeData() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            List<CategoryEntity> allCategories = new ArrayList<>();
            CategoryEntity all = new CategoryEntity();
            all.setId("all");
            all.setName("All");
            all.setOrder(0);
            all.setActive(true);
            allCategories.add(all);
            if (categories != null) {
                allCategories.addAll(categories);
            }
            categoryAdapter.setCategories(allCategories);
        });

        viewModel.getWidgets().observe(getViewLifecycleOwner(), widgets -> {
            widgetAdapter.setWidgets(widgets);
            boolean isEmpty = widgets == null || widgets.isEmpty();
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            widgetRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }
}
