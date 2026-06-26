package com.desire.widget.ui.widgets;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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

        view.findViewById(R.id.mode_responsive).setOnClickListener(v -> showSystemHomeDialog(view, "Responsive"));
        view.findViewById(R.id.mode_fixed).setOnClickListener(v -> showSystemHomeDialog(view, "Fixed"));

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    private void showSystemHomeDialog(View anchor, String mode) {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(FirebaseService.getInstance().incrementWidgetDownload(widget.getId()));
            } catch (Exception ignored) {}
        });

        View systemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_widget_system_home, null);
        TextView previewTitle = systemView.findViewById(R.id.system_widget_title);
        TextView previewSize = systemView.findViewById(R.id.system_widget_size);
        previewTitle.setText("Widget " + widget.getName());
        previewSize.setText(displaySize(widget.getWidgetSize()));

        Dialog systemDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(systemView)
                .create();

        systemView.findViewById(R.id.add_auto_btn).setOnClickListener(v -> {
            Snackbar.make(anchor, mode + " widget added", Snackbar.LENGTH_SHORT).show();
            systemDialog.dismiss();
            dismiss();
        });
        systemView.findViewById(R.id.cancel_btn).setOnClickListener(v -> systemDialog.dismiss());
        systemDialog.show();
    }

    private String displaySize(String size) {
        if (size == null || size.trim().isEmpty()) return "2x2";
        return size.trim().toUpperCase();
    }
}
