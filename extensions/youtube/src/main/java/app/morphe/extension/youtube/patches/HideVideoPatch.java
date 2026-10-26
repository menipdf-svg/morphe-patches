/*
 * Hide video patch, built on top of Morphe patches (GPLv3).
 * See the included LICENSE and NOTICE files for the terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import kotlin.Unit;

/**
 * Hides the picture of the regular watch player so only the black player background is visible.
 * Audio keeps playing.
 * <p>
 * The video is rendered by a {@link SurfaceView} or {@link TextureView} inside the player view.
 * By default the video view is made fully transparent ({@link #hideView(View)}), which keeps the
 * surface alive so playback is less likely to be interrupted.
 * <p>
 * Alternative approaches, if transparency does not work on a given device or YouTube version:
 * <ul>
 *     <li>{@code view.setVisibility(View.INVISIBLE)}: hides for sure, but destroys the surface and
 *         YouTube may pause playback.</li>
 *     <li>Add a black overlay view on top of the video view.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class HideVideoPatch {

    private static final int MAX_SEARCH_DEPTH = 12;

    private static WeakReference<View> playerViewRef = new WeakReference<>(null);

    /**
     * Video views that this patch made transparent, so they can be restored
     * if the setting is turned off.
     */
    private static final List<WeakReference<View>> hiddenViews = new ArrayList<>();

    static {
        PlayerType.getOnChange().addObserver((PlayerType type) -> {
            applyOnMainThread();
            return Unit.INSTANCE;
        });
    }

    /**
     * Injection point. Called at the end of {@code YouTubePlayerViewNotForReflection.onLayout}.
     */
    public static void onPlayerViewLayout(View playerView) {
        try {
            if (playerView == null) {
                return;
            }
            playerViewRef = new WeakReference<>(playerView);
            applyOnMainThread();
        } catch (Exception ex) {
            Logger.printException(() -> "onPlayerViewLayout failure", ex);
        }
    }

    private static void applyOnMainThread() {
        Utils.runOnMainThread(HideVideoPatch::apply);
    }

    private static void apply() {
        try {
            View playerView = playerViewRef.get();
            if (Settings.HIDE_VIDEO.get()) {
                if (playerView == null) {
                    return;
                }
                List<View> videoViews = new ArrayList<>();
                findVideoViews(playerView, 0, videoViews);
                for (View videoView : videoViews) {
                    hideView(videoView);
                }
            } else {
                restoreHiddenViews();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "apply failure", ex);
        }
    }

    private static void findVideoViews(@Nullable View view, int depth, List<View> result) {
        if (view == null || depth > MAX_SEARCH_DEPTH) {
            return;
        }
        if (view instanceof SurfaceView || view instanceof TextureView) {
            result.add(view);
            return;
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                findVideoViews(group.getChildAt(i), depth + 1, result);
            }
        }
    }

    private static void hideView(View videoView) {
        if (videoView.getAlpha() != 0f) {
            videoView.setAlpha(0f);
        }
        for (WeakReference<View> ref : hiddenViews) {
            if (ref.get() == videoView) {
                return;
            }
        }
        hiddenViews.add(new WeakReference<>(videoView));
    }

    private static void restoreHiddenViews() {
        Iterator<WeakReference<View>> iterator = hiddenViews.iterator();
        while (iterator.hasNext()) {
            View view = iterator.next().get();
            if (view != null) {
                view.setAlpha(1f);
            }
            iterator.remove();
        }
    }
}
