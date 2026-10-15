package com.desire.widget.engine.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.BatteryManager;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.BluetoothBattery;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.Shapes;
import com.desire.widget.engine.util.SpecProps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Battery of this phone plus connected Bluetooth accessories (earbuds, headphones, watch).
 *
 * <ul>
 *   <li>{@code style: tiles} — up to three shaped tiles (icon + percent): one centred, two side by
 *   side, or one over two. Without Bluetooth access a dashed "add" tile invites a tap.</li>
 *   <li>{@code style: fill} — one wide card filled to the level of the first accessory (or the
 *   phone when none is connected), with its icon, percent and name.</li>
 * </ul>
 * props: shapes, fills, textColors, fillColor, trackColor, textColor, subColor, font, includePhone.
 */
public class DeviceBatteryRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "device_battery";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        boolean preview = ctx.appWidgetId < 0;
        List<BluetoothBattery.Device> devices = new ArrayList<>();
        if (SpecProps.b(p, "includePhone", true)) {
            devices.add(new BluetoothBattery.Device(BatteryRenderer.deviceName(ctx.android),
                    phoneLevel(ctx.android), BluetoothBattery.KIND_PHONE));
        }
        if (preview) {
            devices.add(new BluetoothBattery.Device("Galaxy Watch", 80, BluetoothBattery.KIND_WATCH));
            devices.add(new BluetoothBattery.Device("Pixel Buds", 64, BluetoothBattery.KIND_HEADPHONES));
        } else {
            devices.addAll(BluetoothBattery.connected(ctx.android));
        }
        boolean canAdd = !preview && !BluetoothBattery.hasPermission(ctx.android);

        if ("fill".equals(SpecProps.str(p, "style", "tiles"))) {
            BluetoothBattery.Device d = devices.size() > 1 ? devices.get(1) : devices.isEmpty() ? null : devices.get(0);
            if (d != null) fillCard(canvas, b, p, ctx, d);
            return;
        }
        tiles(canvas, b, p, ctx, devices, canAdd);
    }

    private void tiles(Canvas c, RectF b, Map<String, Object> p, RenderContext ctx,
                       List<BluetoothBattery.Device> devices, boolean canAdd) {
        List<String> shapes = TimeTilesRenderer.strings(p, "shapes", new String[]{"blob", "gem", "circle"});
        int[] fills = ClockText.colors(p, "fills", ctx.theme, new int[]{0xFFD9D9DE, 0xFF0B0B0D, 0xFFE8E070});
        int[] texts = ClockText.colors(p, "textColors", ctx.theme, new int[]{0xFF2A2A30, 0xFFF2F2F4, 0xFF2A2A30});
        int n = Math.min(3, devices.size() + (canAdd ? 1 : 0));
        float side = Math.min(b.width(), b.height());
        RectF sq = new RectF(b.centerX() - side / 2f, b.centerY() - side / 2f, b.centerX() + side / 2f, b.centerY() + side / 2f);
        RectF[] slots;
        float t = side * 0.46f; // tile size
        if (n <= 1) {
            slots = new RectF[]{centered(sq.centerX(), sq.centerY(), side * 0.72f)};
        } else if (n == 2) {
            slots = new RectF[]{centered(sq.left + side * 0.27f, sq.centerY(), t), centered(sq.right - side * 0.27f, sq.centerY(), t)};
        } else {
            slots = new RectF[]{centered(sq.centerX(), sq.top + side * 0.27f, t),
                    centered(sq.left + side * 0.27f, sq.bottom - side * 0.27f, t),
                    centered(sq.right - side * 0.27f, sq.bottom - side * 0.27f, t)};
        }
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "medium"), true));
        tp.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            RectF slot = slots[i];
            if (i >= devices.size()) { // the "add Bluetooth" invitation
                Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
                dash.setStyle(Paint.Style.STROKE);
                dash.setStrokeWidth(slot.width() * 0.03f);
                dash.setColor(0x88FFFFFF);
                dash.setPathEffect(new DashPathEffect(new float[]{slot.width() * 0.07f, slot.width() * 0.05f}, 0));
                c.drawPath(Shapes.path("circle", inset(slot, 0.03f)), dash);
                icon(c, BluetoothBattery.KIND_OTHER, slot.centerX(), slot.centerY() - slot.height() * 0.1f, slot.height() * 0.16f, 0xCCFFFFFF);
                tp.setColor(0xCCFFFFFF);
                tp.setTextSize(slot.height() * 0.14f);
                c.drawText("Add", slot.centerX(), slot.centerY() + slot.height() * 0.26f, tp);
                continue;
            }
            BluetoothBattery.Device d = devices.get(i);
            fp.setColor(fills[i % fills.length]);
            c.drawPath(Shapes.path(shapes.get(i % shapes.size()), slot), fp);
            int tc = texts[i % texts.length];
            icon(c, d.kind, slot.centerX(), slot.centerY() - slot.height() * 0.12f, slot.height() * 0.15f, tc);
            tp.setColor(tc);
            tp.setTextSize(slot.height() * (n == 1 ? 0.2f : 0.19f));
            c.drawText(d.level + "%", slot.centerX(), slot.centerY() + slot.height() * 0.27f, tp);
        }
    }

    private void fillCard(Canvas c, RectF b, Map<String, Object> p, RenderContext ctx, BluetoothBattery.Device d) {
        int fill = ClockText.color(p, "fillColor", ctx.theme, 0xFF7B4CB8);
        int track = ClockText.color(p, "trackColor", ctx.theme, 0xFF2A2633);
        int text = ClockText.color(p, "textColor", ctx.theme, 0xFFF4EEFF);
        int sub = ClockText.color(p, "subColor", ctx.theme, 0xFFE6C9FF);
        float r = Math.min(b.width(), b.height()) * 0.3f;
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(track);
        c.drawRoundRect(b, r, r, fp);
        int save = c.save();
        c.clipRect(b.left, b.top, b.left + Math.max(r * 2f, b.width() * d.level / 100f), b.bottom);
        fp.setColor(fill);
        c.drawRoundRect(b, r, r, fp);
        c.restoreToCount(save);

        float x = b.left + b.width() * 0.09f;
        float big = b.height() * 0.24f;
        icon(c, d.kind, x + big * 0.4f, b.top + b.height() * 0.38f - big * 0.36f, big * 0.42f, sub);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "medium"), true));
        tp.setColor(sub);
        tp.setTextSize(big);
        c.drawText(d.level + "%", x + big * 0.95f, b.top + b.height() * 0.38f, tp);
        tp.setColor(text);
        tp.setTextSize(b.height() * 0.13f);
        String name = d.name;
        float max = b.width() * 0.82f;
        while (name.length() > 1 && tp.measureText(name) > max) name = name.substring(0, name.length() - 1);
        c.drawText(name, x, b.top + b.height() * 0.66f, tp);
    }

    private static RectF centered(float cx, float cy, float size) {
        return new RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
    }

    private static RectF inset(RectF r, float f) {
        return new RectF(r.left + r.width() * f, r.top + r.height() * f, r.right - r.width() * f, r.bottom - r.height() * f);
    }

    /** Simple line icons for phone / watch / headphones / Bluetooth, centred at (cx, cy), size ~2r. */
    static void icon(Canvas c, String kind, float cx, float cy, float r, int color) {
        Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
        st.setStyle(Paint.Style.STROKE);
        st.setStrokeWidth(r * 0.2f);
        st.setStrokeCap(Paint.Cap.ROUND);
        st.setStrokeJoin(Paint.Join.ROUND);
        st.setColor(color);
        Paint fl = new Paint(Paint.ANTI_ALIAS_FLAG);
        fl.setColor(color);
        switch (kind) {
            case BluetoothBattery.KIND_PHONE: {
                RectF body = new RectF(cx - r * 0.55f, cy - r, cx + r * 0.55f, cy + r);
                c.drawRoundRect(body, r * 0.22f, r * 0.22f, st);
                c.drawLine(cx - r * 0.15f, cy + r * 0.68f, cx + r * 0.15f, cy + r * 0.68f, st);
                break;
            }
            case BluetoothBattery.KIND_WATCH: {
                RectF body = new RectF(cx - r * 0.6f, cy - r * 0.6f, cx + r * 0.6f, cy + r * 0.6f);
                c.drawRoundRect(body, r * 0.25f, r * 0.25f, st);
                c.drawRoundRect(new RectF(cx - r * 0.38f, cy - r * 1.1f, cx + r * 0.38f, cy - r * 0.6f), r * 0.1f, r * 0.1f, fl);
                c.drawRoundRect(new RectF(cx - r * 0.38f, cy + r * 0.6f, cx + r * 0.38f, cy + r * 1.1f), r * 0.1f, r * 0.1f, fl);
                break;
            }
            case BluetoothBattery.KIND_HEADPHONES: {
                RectF arc = new RectF(cx - r * 0.85f, cy - r * 0.9f, cx + r * 0.85f, cy + r * 0.8f);
                c.drawArc(arc, 180f, 180f, false, st);
                c.drawRoundRect(new RectF(cx - r * 0.95f, cy, cx - r * 0.45f, cy + r * 0.9f), r * 0.18f, r * 0.18f, fl);
                c.drawRoundRect(new RectF(cx + r * 0.45f, cy, cx + r * 0.95f, cy + r * 0.9f), r * 0.18f, r * 0.18f, fl);
                break;
            }
            default: { // Bluetooth rune
                Path path = new Path();
                path.moveTo(cx - r * 0.5f, cy - r * 0.45f);
                path.lineTo(cx + r * 0.45f, cy + r * 0.45f);
                path.lineTo(cx, cy + r * 0.95f);
                path.lineTo(cx, cy - r * 0.95f);
                path.lineTo(cx + r * 0.45f, cy - r * 0.45f);
                path.lineTo(cx - r * 0.5f, cy + r * 0.45f);
                c.drawPath(path, st);
            }
        }
    }

    private static int phoneLevel(Context c) {
        try {
            BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
            int l = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
            return l >= 0 && l <= 100 ? l : 72;
        } catch (Exception e) {
            return 72;
        }
    }
}
