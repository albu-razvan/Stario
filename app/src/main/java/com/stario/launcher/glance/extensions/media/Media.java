/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.glance.extensions.media;

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
import android.os.Bundle;
import android.os.Handler;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.GlanceViewExtensionType;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.services.NotificationService;
import com.stario.launcher.ui.glance.GlanceConstraintLayout;
import com.stario.launcher.ui.media.SliderComposeView;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.media.AccentBitmapTransformation;
import com.stario.launcher.utils.media.BlurBitmapTransformation;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class Media extends GlanceDialogExtension {
    public static final String PREFERENCE_ENTRY = "com.stario.Media.MEDIA";
    protected static final String ENABLED_KEY = "com.stario.launcher.media.enabled";
    private static final String TAG = "com.stario.launcher.media";
    private static final long SEEK_TIME = 5000;
    private static final float MIN_BITMAP_SIZE = 256;
    private static final Integer PLAYING = 1;
    private static final Integer PAUSED = 0;
    private final MediaController.Callback callback;
    private final Handler handler;
    private List<MediaController> controllers;
    private ViewGroup interactions;
    private ConstraintLayout coverParent;
    private ImageView cover;
    private TextView song;
    private TextView artist;
    private SliderComposeView slider;
    private ImageView rewind;
    private ImageView playPause;
    private ImageView skip;
    private ImageView forward;
    private boolean skipUpdate;
    private MediaController session;
    private MediaSessionManager mediaSessionManager;

    public Media() {
        super();

        this.session = null;
        this.handler = new Handler();

        this.callback = new MediaController.Callback() {
            @Override
            public void onSessionDestroyed() {
                update();

                super.onSessionDestroyed();
            }

            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                if (session == null ||
                        (controllers.get(0) != null &&
                                !controllers.get(0).getPackageName()
                                        .equals(session.getPackageName()))) {
                    update();
                }

                skipUpdate = false;
                updatePlaybackState();

                super.onPlaybackStateChanged(state);
            }

            @Override
            public void onMetadataChanged(@Nullable MediaMetadata metadata) {
                updateSession();

                super.onMetadataChanged(metadata);
            }
        };
    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    protected GlanceViewExtensionType getPreviewType() {
        return GlanceViewExtensionType.MEDIA_PLAYER_PREVIEW;
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

        // marquee
        song.setSelected(true);

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

        activity.addDestroyListener(() -> {
            try {
                activity.unregisterReceiver(receiver);

                disable();
            } catch (Exception exception) {
                Log.e(TAG, "Receiver not registered");
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
        if (callback != null && NotificationManagerCompat
                .getEnabledListenerPackages(activity).contains(activity.getPackageName())) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                    new ComponentName(activity, NotificationService.class));

            for (MediaController controller : controllers) {
                controller.unregisterCallback(callback);
            }
        }

        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void update() {
        if (activity == null || !NotificationManagerCompat.getEnabledListenerPackages(activity)
                .contains(activity.getPackageName()) ||
                !activity.getSettings().getBoolean(PREFERENCE_ENTRY, false)) {
            disable();

            return;
        }

        List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                new ComponentName(activity, NotificationService.class));

        if (controllers.size() != 0) {
            for (MediaController controller : controllers) {
                if (this.controllers == null ||
                        !this.controllers.contains(controller)) {
                    controller.registerCallback(callback);
                }
            }

            session = controllers.get(0);

            updateSession();
        } else {
            disable();
        }

        Bundle data = new Bundle();
        data.putBoolean(ENABLED_KEY, isEnabled());
        sendDataToPreview(data);

        this.controllers = controllers;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void updateSession() {
        if (session == null) {
            disable();
        } else {
            MediaMetadata metadata = session.getMetadata();

            if (metadata != null) {
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

                        MediaController.TransportControls transportControls = session.getTransportControls();

                        transportControls.skipToPrevious();

                        skipUpdate = true;

                        view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_left));
                    }
                });

                skip.setOnClickListener(view -> {
                    if (session != null) {
                        Vibrations.getInstance().vibrate();

                        MediaController.TransportControls controls = session.getTransportControls();

                        controls.skipToNext();

                        skipUpdate = true;

                        view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_right));
                    }
                });

                forward.setOnClickListener(view -> {
                    if (session != null) {
                        Vibrations.getInstance().vibrate();

                        MediaController.TransportControls controls = session.getTransportControls();
                        PlaybackState state = session.getPlaybackState();

                        if (state != null) {
                            long position = state.getPosition() + SEEK_TIME;

                            if (position < metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)) {
                                controls.seekTo(position);
                            } else {
                                controls.skipToNext();
                            }
                        } else {
                            controls.skipToNext();
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

                if (!title.contentEquals(song.getText())) {
                    String finalTitle = title;

                    song.animate().alpha(0)
                            .setDuration(Animation.MEDIUM.getDuration())
                            .withEndAction(() -> {
                                song.setText(finalTitle);

                                song.post(() -> song.animate()
                                        .alpha(1)
                                        .setDuration(Animation.MEDIUM.getDuration())
                                );
                            });
                }

                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

                if (artist == null) {
                    artist = activity.getResources()
                            .getString(R.string.unknown_artist);
                } else {
                    artist = artist.trim();
                }

                if (!artist.contentEquals(this.artist.getText())) {
                    String finalArtist = artist.trim();

                    this.artist.animate().alpha(0)
                            .setDuration(Animation.MEDIUM.getDuration())
                            .withEndAction(() -> {
                                this.artist.setText(finalArtist);

                                this.artist.post(() -> this.artist.animate()
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
                    Bitmap cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);

                    if (cover == null ||
                            cover.getWidth() < MIN_BITMAP_SIZE ||
                            cover.getHeight() < MIN_BITMAP_SIZE) {
                        cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                    }

                    if (cover == null ||
                            cover.getWidth() < MIN_BITMAP_SIZE ||
                            cover.getHeight() < MIN_BITMAP_SIZE) {
                        cover = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                    }

                    updateCover(cover);
                }

                updatePlaybackState();
            } else {
                disable();
            }
        }
    }

    public boolean updateCover(String stringUri) {
        if (stringUri != null) {
            InputStream inputStream;

            try {
                Uri uri = Uri.parse(stringUri);

                if (uri != null && "content".equals(uri.getScheme())) {
                    inputStream = activity.getContentResolver()
                            .openInputStream(uri);
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
        if (bitmap != null) {
            DrawableCrossFadeFactory factory =
                    new DrawableCrossFadeFactory.Builder()
                            .setCrossFadeEnabled(true).build();

            if (!activity.isDestroyed()) {
                Glide.with(activity)
                        .load(bitmap)
                        .apply(RequestOptions.bitmapTransform(
                                new MultiTransformation<>(new BlurBitmapTransformation(5),
                                        new AccentBitmapTransformation())))
                        .placeholder(cover.getDrawable())
                        .transition(DrawableTransitionOptions.withCrossFade(factory))
                        .into(cover);
            }
        }
    }

    private void updateSlider(MediaMetadata metadata) {
        if (session != null) {
            PlaybackState playbackState = session.getPlaybackState();

            if (playbackState != null && !skipUpdate) {
                float progress = (float) playbackState.getPosition() /
                        metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

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

        Bundle data = new Bundle();
        data.putBoolean(ENABLED_KEY, isEnabled());
        sendDataToPreview(data);

        hide();
    }
}
