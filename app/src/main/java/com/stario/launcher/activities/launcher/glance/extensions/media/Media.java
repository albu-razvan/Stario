/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.activities.launcher.glance.extensions.media;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.glance.GlanceDialogExtension;
import com.stario.launcher.activities.launcher.glance.GlanceViewExtension;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.services.NotificationService;
import com.stario.launcher.ui.common.glance.GlanceConstraintLayout;
import com.stario.launcher.ui.common.media.SliderComposeView;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.media.AccentBitmapTransformation;
import com.stario.launcher.utils.media.BlurBitmapTransformation;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.glide.transformations.CropSquareTransformation;

public class Media extends GlanceDialogExtension {
    public static final String PREFERENCE_ENTRY = "com.stario.Media.MEDIA";
    private static final String TAG = "com.stario.launcher.media";
    private static final long SEEK_TIME = 5000;
    private static final float MIN_BITMAP_SIZE = 256;
    private static final Integer PLAYING = 1;
    private static final Integer PAUSED = 0;

    private final MediaPreview preview;
    private final Handler handler;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChangedListener;
    private final Map<MediaController, MediaController.Callback> controllerCallbacks;

    private MediaSessionManager mediaSessionManager;
    private ConstraintLayout coverParent;
    private SliderComposeView slider;
    private MediaController session;
    private ViewGroup interactions;
    private ImageView playPause;
    private boolean skipUpdate;
    private ImageView forward;
    private String lastArtist;
    private Bitmap lastCover;
    private ImageView rewind;
    private String lastSong;
    private ImageView cover;
    private TextView artist;
    private ImageView skip;
    private TextView song;

    public Media() {
        super();

        this.session = null;
        this.lastSong = "";
        this.lastArtist = "";
        this.handler = new Handler(Looper.getMainLooper());
        this.controllerCallbacks = new HashMap<>();

        this.sessionsChangedListener = controllers -> {
            handler.post(this::update);
        };

        this.preview = new MediaPreview();
    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    protected GlanceViewExtension getViewExtensionPreview() {
        return preview;
    }

    @Override
    public boolean isEnabled() {
        return session != null;
    }

    @Override
    protected void updateScaling(@FloatRange(from = 0f, to = 1f) float fraction,
                                 float scale) {
        cover.setScaleY(scale);
        interactions.setScaleY(scale);

        coverParent.setAlpha(fraction);
        interactions.setAlpha(fraction);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mediaSessionManager =
                (MediaSessionManager) activity.getSystemService(Context.MEDIA_SESSION_SERVICE);

        if (mediaSessionManager != null) {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener,
                    new ComponentName(activity, NotificationService.class));
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected GlanceConstraintLayout inflateExpanded(LayoutInflater inflater, ConstraintLayout container) {
        GlanceConstraintLayout root = (GlanceConstraintLayout) inflater.inflate(R.layout.media,
                container, false);

        interactions = root.findViewById(R.id.interactions);
        cover = root.findViewById(R.id.album_cover);
        artist = root.findViewById(R.id.artist);
        song = root.findViewById(R.id.song);
        playPause = root.findViewById(R.id.play_pause);
        rewind = root.findViewById(R.id.rewind);
        skip = root.findViewById(R.id.skip);
        forward = root.findViewById(R.id.forward);
        slider = root.findViewById(R.id.slider);

        coverParent = (ConstraintLayout) cover.getParent();

        playPause.setTag(PAUSED);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            activity.registerReceiver(receiver,
                    new IntentFilter(NotificationService.UPDATE_NOTIFICATIONS), Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(receiver,
                    new IntentFilter(NotificationService.UPDATE_NOTIFICATIONS));
        }

        Lifecycle lifecycle = activity.getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                try {
                    activity.unregisterReceiver(receiver);

                    disable();
                } catch (Exception exception) {
                    Log.e(TAG, "Receiver not registered");
                }

                lifecycle.removeObserver(this);
            }
        });

        root.setOnClickListener(v -> {
            if (session != null) {
                PackageManager packageManager = activity.getPackageManager();

                Intent intent = packageManager.getLaunchIntentForPackage(session.getPackageName());

                if (intent != null) {
                    Vibrations.getInstance().vibrate();

                    activity.startActivity(intent,
                            ActivityOptions.makeScaleUpAnimation(root, 0, 0,
                                    root.getWidth(), root.getHeight()).toBundle());
                }
            }
        });

        update();

        return root;
    }

    @Override
    public void onDestroy() {
        if (mediaSessionManager != null) {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener);
        }

        for (Map.Entry<MediaController, MediaController.Callback> entry : controllerCallbacks.entrySet()) {
            entry.getKey().unregisterCallback(entry.getValue());
        }

        controllerCallbacks.clear();
        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void update() {
        if (activity == null || !NotificationManagerCompat
                .getEnabledListenerPackages(activity)
                .contains(BuildConfig.APPLICATION_ID) ||
                !activity.getApplicationContext()
                        .getSettings()
                        .getBoolean(PREFERENCE_ENTRY, false)) {
            disable();

            return;
        }

        List<MediaController> activeSessions = mediaSessionManager.getActiveSessions(
                new ComponentName(activity, NotificationService.class));

        List<MediaController> inactiveControllers = new ArrayList<>();
        for (MediaController trackedController : controllerCallbacks.keySet()) {
            boolean stillActive = false;
            for (MediaController active : activeSessions) {
                if (active.getSessionToken().equals(trackedController.getSessionToken())) {
                    stillActive = true;
                    break;
                }
            }

            if (!stillActive) {
                inactiveControllers.add(trackedController);
            }
        }

        for (MediaController controller : inactiveControllers) {
            MediaController.Callback callback = controllerCallbacks.remove(controller);

            if (callback != null) {
                controller.unregisterCallback(callback);
            }
        }

        for (MediaController controller : activeSessions) {
            boolean isNew = true;
            for (MediaController tracked : controllerCallbacks.keySet()) {
                if (tracked.getSessionToken().equals(controller.getSessionToken())) {
                    isNew = false;
                    break;
                }
            }

            if (isNew) {
                MediaControllerCallback callback = new MediaControllerCallback(controller);
                controller.registerCallback(callback);

                controllerCallbacks.put(controller, callback);
            }
        }

        updateActiveSession(activeSessions);
        preview.setEnabled(isEnabled());
    }

    private void updateActiveSession(List<MediaController> controllers) {
        if (controllers.isEmpty()) {
            disable();

            return;
        }

        MediaController activeSessionCandidate = null;

        for (MediaController controller : controllers) {
            PlaybackState state = controller.getPlaybackState();

            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                activeSessionCandidate = controller;
                break;
            }
        }

        if (activeSessionCandidate == null && session != null) {
            for (MediaController controller : controllers) {
                if (controller.getSessionToken().equals(session.getSessionToken())) {
                    activeSessionCandidate = controller;
                    break;
                }
            }
        }

        // should not happen, but rather safe than sorry
        // fallback to the first controller
        if (activeSessionCandidate == null) {
            activeSessionCandidate = controllers.get(0);
        }

        if (session != activeSessionCandidate) {
            session = activeSessionCandidate;
            handler.removeCallbacksAndMessages(null);
        }

        updateSession();
    }

    @Override
    protected void show() {
        super.show();

        updateSession();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void updateSession() {
        if (session == null) {
            disable();
        } else if (!isShowing()) {
            reset();

            return;
        }

        MediaMetadata metadata = session.getMetadata();
        if (metadata == null) {
            return;
        }

        playPause.setOnClickListener(view -> {
            if (session != null) {
                Vibrations.getInstance().vibrate();

                PlaybackState playbackState = session.getPlaybackState();

                if (playbackState != null) {
                    if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
                        session.getTransportControls().pause();
                    } else {
                        session.getTransportControls().play();
                    }

                    view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce));
                }
            }
        });

        rewind.setOnClickListener(view -> {
            if (session != null) {
                Vibrations.getInstance().vibrate();

                session.getTransportControls().skipToPrevious();
                skipUpdate = true;

                view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_left));
            }
        });

        skip.setOnClickListener(view -> {
            if (session != null) {
                Vibrations.getInstance().vibrate();

                session.getTransportControls().skipToNext();
                skipUpdate = true;

                view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_right));
            }
        });

        forward.setOnClickListener(view -> {
            if (session != null) {
                Vibrations.getInstance().vibrate();

                PlaybackState state = session.getPlaybackState();
                if (state != null) {
                    long position = state.getPosition() + SEEK_TIME;

                    if (position < metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)) {
                        session.getTransportControls().seekTo(position);
                    } else {
                        session.getTransportControls().skipToNext();
                    }
                } else {
                    session.getTransportControls().skipToNext();
                }

                skipUpdate = true;

                view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.rotate_small));
            }
        });

        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);

        if (title == null) {
            title = "";
        } else {
            title = title.trim();
        }

        if (!title.contentEquals(lastSong)) {
            lastSong = title;
            String finalTitle = title;

            song.animate().alpha(0)
                    .setDuration(Animation.MEDIUM.getDuration())
                    .withEndAction(() -> {
                        song.setText(finalTitle);
                        song.post(() -> song.animate().alpha(1).setDuration(Animation.MEDIUM.getDuration()));
                    });
        }

        String artistStr = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

        if (artistStr == null) {
            artistStr = activity.getResources().getString(R.string.unknown_artist);
        } else {
            artistStr = artistStr.trim();
        }

        if (!artistStr.contentEquals(lastArtist)) {
            lastArtist = artistStr;
            String finalArtist = artistStr;

            artist.animate().alpha(0)
                    .setDuration(Animation.MEDIUM.getDuration())
                    .withEndAction(() -> {
                        artist.setText(finalArtist);
                        artist.post(() -> artist.animate()
                                .alpha(0.85f)
                                .setDuration(Animation.MEDIUM.getDuration())
                        );
                    });
        }

        handler.removeCallbacksAndMessages(null);
        updateSlider(metadata);

        slider.setListener(new SliderComposeView.OnProgressChanged() {
            @Override
            public void changing() {
                skipUpdate = true;
            }

            @Override
            public void progressChanged(float progress) {
                if (session != null) {
                    long position = (long) (progress *
                            metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));

                    session.getTransportControls().seekTo(position);
                }
            }
        });

        String coverUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI);

        if (coverUri == null || coverUri.isBlank()) {
            coverUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
        }

        if (coverUri == null || coverUri.isBlank()) {
            coverUri = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI);
        }

        boolean result = false;
        if (coverUri != null && !coverUri.isBlank()) {
            result = updateCover(coverUri);
        }

        if (!result) {
            Bitmap coverBmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);

            if (coverBmp == null || coverBmp.getWidth() < MIN_BITMAP_SIZE || coverBmp.getHeight() < MIN_BITMAP_SIZE) {
                coverBmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            }

            if (coverBmp == null || coverBmp.getWidth() < MIN_BITMAP_SIZE || coverBmp.getHeight() < MIN_BITMAP_SIZE) {
                coverBmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            }

            updateCover(coverBmp);
        }

        updatePlaybackState();
    }

    private void reset() {
        playPause.setOnClickListener(null);
        rewind.setOnClickListener(null);
        skip.setOnClickListener(null);
        forward.setOnClickListener(null);

        song.setText(null);
        artist.setText(null);

        cover.setImageDrawable(null);

        lastArtist = "";
        lastSong = "";
        lastCover = null;
    }

    public boolean updateCover(String stringUri) {
        if (stringUri != null) {
            try {
                InputStream inputStream;
                Uri uri = Uri.parse(stringUri);

                if (uri != null && "content".equals(uri.getScheme())) {
                    inputStream = activity.getContentResolver().openInputStream(uri);
                } else {
                    inputStream = new FileInputStream(stringUri);
                }

                updateCover(BitmapFactory.decodeStream(inputStream));

                return true;
            } catch (Exception exception) {
                return false;
            }
        }

        return false;
    }

    public void updateCover(Bitmap bitmap) {
        if (bitmap == null && lastCover == null) {
            return;
        }

        if (bitmap != null && lastCover != null &&
                !lastCover.isRecycled() && bitmap.sameAs(lastCover)) {
            return;
        }

        if (bitmap != null) {
            Bitmap.Config config = bitmap.getConfig();
            if (config == null) {
                config = Bitmap.Config.ARGB_8888;
            }

            bitmap = bitmap.copy(config, false);

            DrawableCrossFadeFactory factory =
                    new DrawableCrossFadeFactory.Builder()
                            .setCrossFadeEnabled(true).build();

            if (!activity.isDestroyed()) {
                Glide.with(activity)
                        .load(bitmap)
                        .apply(RequestOptions.bitmapTransform(
                                new MultiTransformation<>(
                                        new CropSquareTransformation(),
                                        new BlurBitmapTransformation(5),
                                        new AccentBitmapTransformation())))
                        .placeholder(cover.getDrawable())
                        .transition(DrawableTransitionOptions.withCrossFade(factory))
                        .into(cover);
            }

            lastCover = bitmap;
        } else {
            lastCover = null;
        }
    }

    private void updateSlider(MediaMetadata metadata) {
        if (session != null) {
            PlaybackState playbackState = session.getPlaybackState();

            if (playbackState != null && !skipUpdate) {
                long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                float progress = (duration > 0) ? (float) playbackState.getPosition() / duration : 0;

                if (Float.isNaN(progress) || Float.isInfinite(progress)) {
                    progress = 0;
                }

                slider.getProgress().setValue(progress);
            }

            handler.postDelayed(() -> updateSlider(metadata), 200);
        }
    }

    public void updatePlaybackState() {
        if (session != null) {
            PlaybackState playbackState = session.getPlaybackState();

            if (playbackState != null) {
                if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
                    if (PAUSED.equals(playPause.getTag())) {
                        AnimatedVectorDrawable drawable =
                                (AnimatedVectorDrawable) ResourcesCompat.getDrawable(activity.getResources(),
                                        R.drawable.ic_play_pause, activity.getTheme());

                        if (drawable != null) {
                            playPause.setImageDrawable(drawable);
                            drawable.start();
                        }

                        playPause.setTag(PLAYING);
                        slider.isPlaying().setValue(true);
                    }
                } else {
                    if (PLAYING.equals(playPause.getTag())) {
                        AnimatedVectorDrawable drawable =
                                (AnimatedVectorDrawable) ResourcesCompat.getDrawable(activity.getResources(),
                                        R.drawable.ic_pause_play, activity.getTheme());

                        if (drawable != null) {
                            playPause.setImageDrawable(drawable);
                            drawable.start();
                        }

                        playPause.setTag(PAUSED);
                        slider.isPlaying().setValue(false);
                    }
                }
            }
        }
    }

    public void disable() {
        session = null;
        preview.setEnabled(isEnabled());

        if (isShowing()) {
            addTransitionListener(new TransitionListener() {
                @Override
                public void onProgressFraction(float fraction) {
                    if (fraction == 0) {
                        reset();
                    }

                    removeTransitionListener(this);
                }
            });

            urgentHide();
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {
        private final MediaController controller;

        MediaControllerCallback(MediaController controller) {
            this.controller = controller;
        }

        @Override
        public void onSessionDestroyed() {
            update();

            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            skipUpdate = false;

            if (state == null) {
                return;
            }

            boolean isCurrentSession = session != null &&
                    session.getSessionToken().equals(controller.getSessionToken());

            if (state.getState() == PlaybackState.STATE_PLAYING && !isCurrentSession) {
                update();
            } else if (isCurrentSession) {
                updatePlaybackState();
            }

            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            if (session != null && session.getSessionToken()
                    .equals(controller.getSessionToken())) {
                updateSession();
            }

            super.onMetadataChanged(metadata);
        }
    }
}