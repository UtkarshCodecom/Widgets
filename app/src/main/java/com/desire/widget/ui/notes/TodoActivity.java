package com.desire.widget.ui.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.util.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Editor for the To-Do widget bound to one {@code appWidgetId}. Open tasks come first (drag ≡ to
 * reorder), completed ones collect under "Completed" with a Clear action. Tap a task to edit it,
 * swipe it away to delete (with Undo). Every change is saved and re-renders that one widget at
 * once — and the widget's rows can be ticked off directly on the home screen too.
 */
public class TodoActivity extends AppCompatActivity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";

    private int appWidgetId = -1;
    /** Canonical order: open tasks (user order), then completed ones (most recent first). */
    private final List<UserDataStore.Todo> todos = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private TodoAdapter adapter;
    private RecyclerView list;
    private TextView emptyView, progressText;
    private LinearProgressIndicator progress;
    private EditText input;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_todo);

        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId >= 0) todos.addAll(UserDataStore.getTodos(this, appWidgetId));
        normalizeOrder();

        View root = findViewById(R.id.todo_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        findViewById(R.id.todo_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.todo_list);
        emptyView = findViewById(R.id.todo_empty);
        progressText = findViewById(R.id.todo_progress_text);
        progress = findViewById(R.id.todo_progress);
        input = findViewById(R.id.todo_input);

        adapter = new TodoAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        touchHelper.attachToRecyclerView(list);

        findViewById(R.id.todo_add).setOnClickListener(v -> addFromInput());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addFromInput(); // keep the keyboard up for quick entry of several tasks
                return true;
            }
            return false;
        });

        refresh(false);
        if (todos.isEmpty()) {
            input.requestFocus();
            input.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    private boolean resumedOnce;

    @Override
    protected void onResume() {
        super.onResume();
        // Tasks may have been ticked on the widget while we were in the background.
        if (resumedOnce && appWidgetId >= 0) {
            todos.clear();
            todos.addAll(UserDataStore.getTodos(this, appWidgetId));
            normalizeOrder();
            refresh(false);
        }
        resumedOnce = true;
    }

    // ------------------------------------------------------------------ actions

    private void addFromInput() {
        String t = input.getText().toString().trim();
        if (t.isEmpty()) return;
        UserDataStore.Todo todo = new UserDataStore.Todo(t, false);
        todos.add(openCount(), todo); // newest open task goes to the end of the open section
        UserDataStore.assignIds(todos);
        input.setText("");
        save();
        refresh(true);
        list.scrollToPosition(Math.max(0, openCount() - 1));
    }

    private void setDone(UserDataStore.Todo t, boolean done) {
        if (t.done == done) return;
        todos.remove(t);
        t.done = done;
        // Ticked → top of Completed; un-ticked → back to the end of the open tasks.
        todos.add(openCount(), t);
        save();
        refresh(true);
    }

    private void edit(UserDataStore.Todo t) {
        EditText field = new EditText(this);
        field.setText(t.text);
        field.setSelection(field.getText().length());
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(field);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit task")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String s = field.getText().toString().trim();
                    if (!s.isEmpty() && !s.equals(t.text)) {
                        t.text = s;
                        save();
                        refresh(true);
                    }
                })
                .show();
        field.requestFocus();
    }

    private void delete(UserDataStore.Todo t) {
        final int index = todos.indexOf(t);
        if (index < 0) return;
        todos.remove(index);
        save();
        refresh(true);
        Snackbar.make(list, "Task deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    todos.add(Math.min(index, todos.size()), t);
                    normalizeOrder();
                    save();
                    refresh(true);
                })
                .show();
    }

    private void clearCompleted() {
        final List<UserDataStore.Todo> before = new ArrayList<>(todos);
        List<UserDataStore.Todo> kept = new ArrayList<>();
        for (UserDataStore.Todo t : todos) if (!t.done) kept.add(t);
        int removed = todos.size() - kept.size();
        todos.clear();
        todos.addAll(kept);
        save();
        refresh(true);
        Snackbar.make(list, removed + (removed == 1 ? " task cleared" : " tasks cleared"), Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    todos.clear();
                    todos.addAll(before);
                    save();
                    refresh(true);
                })
                .show();
    }

    private void save() {
        if (appWidgetId < 0) return;
        UserDataStore.setTodos(this, appWidgetId, todos);
        EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
    }

    // ------------------------------------------------------------------ list model

    private int openCount() {
        int n = 0;
        for (UserDataStore.Todo t : todos) if (!t.done) n++;
        return n;
    }

    /** Open tasks first, keeping relative order (data saved by older versions was mixed). */
    private void normalizeOrder() {
        List<UserDataStore.Todo> open = new ArrayList<>(), done = new ArrayList<>();
        for (UserDataStore.Todo t : todos) (t.done ? done : open).add(t);
        todos.clear();
        todos.addAll(open);
        todos.addAll(done);
    }

    private void refresh(boolean animate) {
        List<Row> next = new ArrayList<>();
        int done = 0;
        for (UserDataStore.Todo t : todos) {
            if (!t.done) next.add(Row.task(t));
            else done++;
        }
        if (done > 0) {
            next.add(Row.section("Completed · " + done));
            for (UserDataStore.Todo t : todos) if (t.done) next.add(Row.task(t));
        }
        List<Row> old = new ArrayList<>(rows);
        rows.clear();
        rows.addAll(next);
        if (animate) {
            DiffUtil.calculateDiff(new RowDiff(old, rows)).dispatchUpdatesTo(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        int total = todos.size();
        emptyView.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        progressText.setText(total == 0 ? "" : done + " of " + total + " done");
        progress.setProgressCompat(total == 0 ? 0 : Math.round(100f * done / total), animate);
    }

    private static final class Row {
        final UserDataStore.Todo todo; // null for the "Completed" section header
        final String title;
        final boolean done;
        final String text;

        private Row(UserDataStore.Todo todo, String title) {
            this.todo = todo;
            this.title = title;
            this.done = todo != null && todo.done;
            this.text = todo != null ? todo.text : null;
        }

        static Row task(UserDataStore.Todo t) { return new Row(t, null); }
        static Row section(String title) { return new Row(null, title); }
    }

    private static final class RowDiff extends DiffUtil.Callback {
        private final List<Row> a, b;

        RowDiff(List<Row> a, List<Row> b) { this.a = a; this.b = b; }

        @Override public int getOldListSize() { return a.size(); }
        @Override public int getNewListSize() { return b.size(); }

        @Override
        public boolean areItemsTheSame(int i, int j) {
            Row x = a.get(i), y = b.get(j);
            if (x.todo == null || y.todo == null) return x.todo == null && y.todo == null;
            return x.todo.id == y.todo.id;
        }

        @Override
        public boolean areContentsTheSame(int i, int j) {
            Row x = a.get(i), y = b.get(j);
            if (x.todo == null) return x.title.equals(y.title);
            return x.done == y.done && String.valueOf(x.text).equals(String.valueOf(y.text));
        }
    }

    // ------------------------------------------------------------------ adapter

    private class TodoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return rows.get(position).todo == null ? 1 : 0;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == 1) return new SectionHolder(inf.inflate(R.layout.item_todo_section, parent, false));
            return new TaskHolder(inf.inflate(R.layout.item_todo, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (holder instanceof SectionHolder) ((SectionHolder) holder).bind(row);
            else ((TaskHolder) holder).bind(row.todo);
        }
    }

    private class SectionHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        SectionHolder(View v) {
            super(v);
            title = v.findViewById(R.id.section_title);
            v.findViewById(R.id.section_action).setOnClickListener(x -> clearCompleted());
        }

        void bind(Row row) {
            title.setText(row.title);
        }
    }

    private class TaskHolder extends RecyclerView.ViewHolder {
        private final CheckBox check;
        private final TextView text;
        private final View drag;

        TaskHolder(View v) {
            super(v);
            check = v.findViewById(R.id.todo_check);
            text = v.findViewById(R.id.todo_text);
            drag = v.findViewById(R.id.todo_drag);
        }

        @SuppressWarnings("ClickableViewAccessibility")
        void bind(UserDataStore.Todo t) {
            check.setOnCheckedChangeListener(null);
            check.setChecked(t.done);
            text.setText(t.text);
            text.setAlpha(t.done ? 0.5f : 1f);
            if (t.done) text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            else text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            check.setOnCheckedChangeListener((b, isChecked) -> b.post(() -> setDone(t, isChecked)));
            text.setOnClickListener(v -> edit(t));
            drag.setVisibility(t.done ? View.INVISIBLE : View.VISIBLE);
            drag.setOnTouchListener((v, e) -> {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) touchHelper.startDrag(this);
                return false;
            });
        }
    }

    // ------------------------------------------------------------------ gestures

    private final ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            int pos = vh.getBindingAdapterPosition();
            if (pos < 0 || pos >= rows.size() || rows.get(pos).todo == null) return 0;
            boolean open = !rows.get(pos).todo.done;
            int drag = open ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
            return makeMovementFlags(drag, ItemTouchHelper.START | ItemTouchHelper.END);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from,
                              @NonNull RecyclerView.ViewHolder to) {
            int a = from.getBindingAdapterPosition(), b = to.getBindingAdapterPosition();
            int open = openCount();
            if (a < 0 || b < 0 || a >= open || b >= open) return false; // open tasks only
            Collections.swap(rows, a, b);
            Collections.swap(todos, a, b); // open tasks occupy the same indices in both lists
            adapter.notifyItemMoved(a, b);
            return true;
        }

        @Override
        public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            super.clearView(rv, vh);
            vh.itemView.setAlpha(1f);
            save(); // persist the new order once the drag ends
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder vh, int actionState) {
            super.onSelectedChanged(vh, actionState);
            if (vh != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) vh.itemView.setAlpha(0.85f);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            int pos = vh.getBindingAdapterPosition();
            if (pos >= 0 && pos < rows.size() && rows.get(pos).todo != null) delete(rows.get(pos).todo);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                float dX, float dY, int actionState, boolean active) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                vh.itemView.setAlpha(1f - Math.min(1f, Math.abs(dX) / vh.itemView.getWidth()));
            }
            super.onChildDraw(c, rv, vh, dX, dY, actionState, active);
        }
    });
}
