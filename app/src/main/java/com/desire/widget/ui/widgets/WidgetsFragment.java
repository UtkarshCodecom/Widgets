package com.desire.widget.ui.widgets;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.desire.widget.ui.customize.CustomizeWidgetActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * The widget gallery. "All" is a tidy overview — one section per category, a few widgets each with
 * "See all" — and a category chip shows just that category. Previews run live (see
 * {@link LivePreview}) while this screen is in the foreground.
 */
public class WidgetsFragment extends Fragment implements GalleryAdapter.Listener {
    private WidgetsViewModel viewModel;
    private GalleryAdapter galleryAdapter;
    private CategoryAdapter categoryAdapter;
    private final List<CategoryEntity> chipCategories = new ArrayList<>();

    private RecyclerView categoryRecycler;
    private RecyclerView widgetRecycler;
    private View titleRow, searchRow, emptyState;
    private EditText searchInput;
    private ImageView favoritesToggle;
    private TextView emptyTitle, emptySubtitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_widgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WidgetsViewModel.class);

        titleRow = view.findViewById(R.id.title_row);
        searchRow = view.findViewById(R.id.search_row);
        searchInput = view.findViewById(R.id.search_input);
        favoritesToggle = view.findViewById(R.id.favorites_toggle);
        categoryRecycler = view.findViewById(R.id.category_recycler);
        widgetRecycler = view.findViewById(R.id.widget_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        emptyTitle = view.findViewById(R.id.empty_title);
        emptySubtitle = view.findViewById(R.id.empty_subtitle);

        setupSearch(view);
        setupFavorites();
        setupCategories();
        setupGallery();
        observe();
    }

    @Override
    public void onResume() {
        super.onResume();
        galleryAdapter.setLive(true);
    }

    @Override
    public void onPause() {
        galleryAdapter.setLive(false);
        super.onPause();
    }

    // ------------------------------------------------------------------ setup

    private void setupSearch(View root) {
        root.findViewById(R.id.search_icon).setOnClickListener(v -> openSearch(true));
        root.findViewById(R.id.search_cancel).setOnClickListener(v -> openSearch(false));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (searchRow.getVisibility() == View.VISIBLE) {
                            openSearch(false);
                        } else if (!WidgetsViewModel.ALL.equals(viewModel.getSelectedCategory())) {
                            selectCategory(0); // back from a category returns to the overview
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
                widgetRecycler.scrollToPosition(0);
            }
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void openSearch(boolean open) {
        titleRow.setVisibility(open ? View.GONE : View.VISIBLE);
        searchRow.setVisibility(open ? View.VISIBLE : View.GONE);
        categoryRecycler.setVisibility(open ? View.GONE : View.VISIBLE);
        if (open) {
            searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        } else {
            searchInput.setText("");
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void setupFavorites() {
        favoritesToggle.setOnClickListener(v -> {
            boolean on = !viewModel.isFavoritesOnly();
            viewModel.setShowFavoritesOnly(on);
            favoritesToggle.setImageResource(on ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            // White outline when off; the heart's own red when showing favourites.
            favoritesToggle.setImageTintList(on ? null
                    : android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.text_primary)));
            widgetRecycler.scrollToPosition(0);
        });
    }

    private void setupCategories() {
        categoryAdapter = new CategoryAdapter();
        categoryAdapter.setOnCategoryClickListener((category, position) -> selectCategory(position));
        categoryRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);
    }

    private void selectCategory(int position) {
        if (position < 0 || position >= chipCategories.size()) return;
        categoryAdapter.setSelectedPosition(position);
        categoryRecycler.smoothScrollToPosition(position);
        viewModel.setSelectedCategory(chipCategories.get(position).getId());
        widgetRecycler.scrollToPosition(0);
    }

    private void setupGallery() {
        galleryAdapter = new GalleryAdapter(this);
        GridLayoutManager lm = new GridLayoutManager(requireContext(), GalleryAdapter.SPAN_COUNT);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return galleryAdapter.getSpanSize(position);
            }
        });
        widgetRecycler.setLayoutManager(lm);
        widgetRecycler.setItemAnimator(null); // live previews re-bind often; no cross-fades
        widgetRecycler.setAdapter(galleryAdapter);
    }

    private void observe() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            chipCategories.clear();
            CategoryEntity all = new CategoryEntity();
            all.setId(WidgetsViewModel.ALL);
            all.setName("All");
            chipCategories.add(all);
            if (categories != null) chipCategories.addAll(categories);
            categoryAdapter.setCategories(new ArrayList<>(chipCategories));
            int selected = 0;
            for (int i = 0; i < chipCategories.size(); i++) {
                if (chipCategories.get(i).getId().equals(viewModel.getSelectedCategory())) selected = i;
            }
            categoryAdapter.setSelectedPosition(selected);
        });

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            galleryAdapter.submitList(items);
            boolean empty = items == null || items.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            widgetRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                if (viewModel.isFavoritesOnly() && !viewModel.isSearching()) {
                    emptyTitle.setText(R.string.no_favorites);
                    emptySubtitle.setText(R.string.no_favorites_hint);
                } else {
                    emptyTitle.setText(R.string.no_widgets);
                    emptySubtitle.setText(R.string.no_results_hint);
                }
            }
        });
    }

    // ------------------------------------------------------------------ GalleryAdapter.Listener

    @Override
    public void onWidgetClick(WidgetEntity widget) {
        String specJson = widget.getSpecJson();
        if (specJson == null || specJson.trim().isEmpty()) {
            // No native definition (e.g. thumbnail-only legacy entry) — fall back to direct install.
            WidgetInstaller.installToHome(requireView(), widget);
            return;
        }
        if (widget.isPro() && !com.desire.widget.billing.BillingManager.getInstance(requireContext()).isPro()) {
            showProUpsell();
            return;
        }
        // The widget page (looks, saved variants, wallpaper preview) leads into customizing.
        Intent intent = new Intent(requireContext(), com.desire.widget.ui.customize.WidgetDetailActivity.class);
        intent.putExtra(CustomizeWidgetActivity.EXTRA_SPEC_JSON, specJson);
        intent.putExtra(CustomizeWidgetActivity.EXTRA_NAME, widget.getName());
        intent.putExtra(CustomizeWidgetActivity.EXTRA_SIZE, widget.getWidgetSize());
        intent.putExtra(CustomizeWidgetActivity.EXTRA_WIDGET_ID, widget.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(WidgetEntity widget, boolean favorite) {
        viewModel.toggleFavorite(widget.getId(), favorite);
    }

    @Override
    public void onSeeAll(String categoryId) {
        for (int i = 0; i < chipCategories.size(); i++) {
            if (chipCategories.get(i).getId().equals(categoryId)) {
                selectCategory(i);
                return;
            }
        }
    }

    private void showProUpsell() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pro widget")
                .setMessage("Unlock all Pro widgets, fonts, and backgrounds with a one-time purchase.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Unlock Pro", (d, w) ->
                        com.desire.widget.billing.BillingManager.getInstance(requireContext())
                                .launchPurchase(requireActivity()))
                .show();
    }
}
