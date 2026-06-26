package com.desire.widget.ui.widgets;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.desire.widget.R;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class WidgetPreviewDialog extends DialogFragment {
    private static final String ARG_WIDGET = "widget";
    private WidgetEntity widget;

    public static WidgetPreviewDialog newInstance(WidgetEntity widget) {
        WidgetPreviewDialog dialog = new WidgetPreviewDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WIDGET, widget);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_Widgets_Translucent);
        if (getArguments() != null) {
            widget = (WidgetEntity) getArguments().getSerializable(ARG_WIDGET);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_widget_preview, null);

        ImageView previewImage = view.findViewById(R.id.preview_image);
        TextView widgetName = view.findViewById(R.id.widget_name);
        TextView widgetDescription = view.findViewById(R.id.widget_description);
        TextView categoryBadge = view.findViewById(R.id.category_badge);
        View proBadge = view.findViewById(R.id.pro_badge);
        Button addToHomeBtn = view.findViewById(R.id.add_to_home_btn);

        widgetName.setText(widget.getName());
        widgetDescription.setText(widget.getDescription());
        categoryBadge.setText(widget.getCategoryName());
        proBadge.setVisibility(widget.isPro() ? View.VISIBLE : View.GONE);

        if (widget.getPreviewUrl() != null && !widget.getPreviewUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(widget.getPreviewUrl())
                    .placeholder(R.drawable.placeholder_widget)
                    .error(R.drawable.placeholder_widget)
                    .centerCrop()
                    .into(previewImage);
        }

        addToHomeBtn.setOnClickListener(v -> {
            AppExecutors.getInstance().networkIO().execute(() -> {
                try {
                    Tasks.await(FirebaseService.getInstance().incrementWidgetDownload(widget.getId()));
                } catch (Exception ignored) {}
            });
            Snackbar.make(view, "Widget added to home screen", Snackbar.LENGTH_SHORT).show();
            dismiss();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
