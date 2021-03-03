//package com.lazy.mediasessiontest.newnotification;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.os.Build;
//import android.support.v4.media.MediaDescriptionCompat;
//import android.support.v4.media.MediaMetadataCompat;
//import android.support.v4.media.session.MediaSessionCompat;
//import android.support.v4.media.session.PlaybackStateCompat;
//import android.util.Log;
//
//import com.lemoncar.reader.R;
//import com.lemoncar.reader.activity.SoundActivity;
//import com.lemoncar.reader.utils.LogUtil;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.RequiresApi;
//import androidx.core.app.NotificationCompat;
//import androidx.core.content.ContextCompat;
//
///**
// * author : xu
// * date : 2021/2/24 16:50
// * description :   目前可以用的
// */
//public class MediaNotificationManagerTwo extends BroadcastReceiver {
//
//    public static final int NOTIFICATION_ID = 412;
//
//    private static final String TAG = MediaNotificationManagerTwo.class.getSimpleName();
//    private static final String CHANNEL_ID = "com.example.android.musicplayer.channel";
//    private static final int REQUEST_CODE = 501;
//
//    private final SoundService mService;
//    public static final String ACTION_PAUSE = "com.example.android.uamp.pause";
//    public static final String ACTION_PLAY = "com.example.android.uamp.play";
//    public static final String ACTION_PREV = "com.example.android.uamp.prev";
//    public static final String ACTION_NEXT = "com.example.android.uamp.next";
//    public static final String ACTION_STOP = "com.example.android.uamp.stop";
//    public static final String ACTION_STOP_CASTING = "com.example.android.uamp.stop_cast";
//    private final NotificationManager mNotificationManager;
//    private final PendingIntent mPlayIntent;
//    private final PendingIntent mPauseIntent;
//    private final PendingIntent mPreviousIntent;
//    private final PendingIntent mNextIntent;
//    private final PendingIntent mStopIntent;
//    private final PendingIntent mStopCastIntent;
//
//    public MediaNotificationManagerTwo(SoundService service) {
//        mService = service;
//        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
//        String pkg = mService.getPackageName();
//        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//        mStopIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//        mStopCastIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
//                new Intent(ACTION_STOP_CASTING).setPackage(pkg),
//                PendingIntent.FLAG_CANCEL_CURRENT);
//
//        // Cancel all notifications to handle the case where the Service was killed and
//        // restarted by the system.
//        mNotificationManager.cancelAll();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_NEXT);
//        filter.addAction(ACTION_PAUSE);
//        filter.addAction(ACTION_PLAY);
//        filter.addAction(ACTION_PREV);
//        filter.addAction(ACTION_STOP_CASTING);
//        mService.registerReceiver(this, filter);
//    }
//
//    public void onDestroy() {
//        Log.d(TAG, "onDestroy: ");
//        mService.unregisterReceiver(this);
//    }
//
//    public NotificationManager getNotificationManager() {
//        return mNotificationManager;
//    }
//
//    public Notification getNotification(MediaMetadataCompat metadata, @NonNull PlaybackStateCompat state,
//                                        MediaSessionCompat.Token token) {
//        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
//        MediaDescriptionCompat description = metadata.getDescription();
//        NotificationCompat.Builder builder = buildNotification(state, token, isPlaying, description);
//        return builder.build();
//    }
//
//    private int addActions(PlaybackStateCompat mPlaybackState, final NotificationCompat.Builder notificationBuilder) {
//        Log.d(TAG, "updatePlayPauseAction");
//
//        int playPauseButtonPosition = 0;
//        // If skip to previous action is enabled
//        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
//            notificationBuilder.addAction(R.mipmap.notice_left, "Previous", mPreviousIntent);
//
//            // If there is a "skip to previous" button, the play/pause button will
//            // be the second one. We need to keep track of it, because the MediaStyle notification
//            // requires to specify the index of the buttons (actions) that should be visible
//            // when in compact view.
//            playPauseButtonPosition = 1;
//        }
//
//        // Play or pause button, depending on the current state.
//        final String label;
//        final int icon;
//        final PendingIntent intent;
//        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
//            label = "Pause";
//            icon = R.mipmap.notice_start;
//            intent = mPauseIntent;
//        } else {
//            label = "Play";
//            icon = R.mipmap.notice_stop;
//            intent = mPlayIntent;
//        }
//        notificationBuilder.addAction(new NotificationCompat.Action(icon, label, intent));
//
//        // 下一节
//        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
//            notificationBuilder.addAction(R.mipmap.notice_right,
//                    "Next", mNextIntent);
//        }
//
//        return playPauseButtonPosition;
//    }
//
//
//    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state, MediaSessionCompat.Token token,
//                                                         boolean isPlaying, MediaDescriptionCompat description) {
//        LogUtil.loge(TAG, "---------description:" + description.getSubtitle());
//        String fetchArtUrl = null;
//        Bitmap art = null;
//        if (description.getIconUri() != null) {
//            // This sample assumes the iconUri will be a valid URL formatted String, but
//            // it can actually be any valid Android Uri formatted String.
//            // async fetch the album art icon
//            String artUrl = description.getIconUri().toString();
//            art = com.lemoncar.reader.service.AlbumArtCache.getInstance().getBigImage(artUrl);
//            if (art == null) {
//                fetchArtUrl = artUrl;
//                // use a placeholder art while the remote art is being downloaded
//                art = BitmapFactory.decodeResource(mService.getResources(),
//                        R.mipmap.logo);
//            }
//        }
//
//        // Create the (mandatory) notification channel when running on Android Oreo.
//        if (isAndroidOOrHigher()) {
//            createChannel();
//        }
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, CHANNEL_ID);
//        final int playPauseButtonPosition = addActions(state, builder);
//        builder.setStyle(
//                new androidx.media.app.NotificationCompat.MediaStyle()
//                        .setMediaSession(token)
//                        .setShowActionsInCompactView(playPauseButtonPosition)
//                        // For backwards compatibility with Android L and earlier.
//                        .setShowCancelButton(true)
//                        .setCancelButtonIntent(mStopIntent))
//                .setColor(ContextCompat.getColor(mService, R.color.lemon_2E5088))
//                .setSmallIcon(R.mipmap.logo_round)
//                // Pending intent that is fired when user clicks on notification.
//                .setContentIntent(createContentIntent())
//                // Title - Usually Song name.
//                .setContentTitle(description.getTitle())
//                // Subtitle - Usually Artist name.
//                .setContentText(description.getSubtitle())
//                .setLargeIcon(art)
//                // When notification is deleted (when playback is paused and notification can be
//                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
//                .setDeleteIntent(mStopIntent)
//                // Show controls on lock screen even when user hides sensitive content.
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//
//        if (fetchArtUrl != null) {
//            fetchBitmapFromURLAsync(description.getIconUri().toString(), state, fetchArtUrl, builder);
//        }
//        return builder;
//    }
//
//    private void fetchBitmapFromURLAsync(String desIconUrl, PlaybackStateCompat state, final String bitmapUrl, final NotificationCompat.Builder builder) {
//        com.lemoncar.reader.service.AlbumArtCache.getInstance().fetch(bitmapUrl, new com.lemoncar.reader.service.AlbumArtCache.FetchListener() {
//            @Override
//            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
//                if (desIconUrl != null && desIconUrl.equals(artUrl)) {
//                    // If the media is still the same, update the notification:
//                    Log.d(TAG, "fetchBitmapFromURLAsync: set bitmap to " + artUrl);
//                    builder.setLargeIcon(bitmap);
//                    addActions(state, builder);
//                    mNotificationManager.notify(NOTIFICATION_ID, builder.build());
//                }
//            }
//        });
//    }
//
//    // Does nothing on versions of Android earlier than O.
//    @RequiresApi(Build.VERSION_CODES.O)
//    private void createChannel() {
//        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
//            // The user-visible name of the channel.
//            CharSequence name = "MediaSession";
//            // The user-visible description of the channel.
//            String description = "MediaSession and MediaPlayer";
//            int importance = NotificationManager.IMPORTANCE_LOW;
//            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
//            // Configure the notification channel.
//            mChannel.setDescription(description);
//            mChannel.enableLights(true);
//            // Sets the notification light color for notifications posted to this
//            // channel, if the device supports this feature.
//            mChannel.setLightColor(Color.RED);
//            mChannel.enableVibration(true);
//            mChannel.setVibrationPattern(
//                    new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
//            mNotificationManager.createNotificationChannel(mChannel);
//            Log.d(TAG, "createChannel: New channel created");
//        } else {
//            Log.d(TAG, "createChannel: Existing channel reused");
//        }
//    }
//
//    private boolean isAndroidOOrHigher() {
//        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
//    }
//
//    private PendingIntent createContentIntent() {
//        Intent openUI = new Intent(mService, SoundActivity.class);
//        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        final String action = intent.getAction();
//        Log.d(TAG, "Received intent with action " + action);
//        switch (action) {
//            case ACTION_PAUSE:
//                mService.getMMediaController().getTransportControls().pause();
//                break;
//            case ACTION_PLAY:
//                mService.getMMediaController().getTransportControls().play();
//                break;
//            case ACTION_NEXT:
//                mService.getMMediaController().getTransportControls().skipToNext();
//                break;
//            case ACTION_PREV:
//                mService.getMMediaController().getTransportControls().skipToPrevious();
//                break;
//            case ACTION_STOP_CASTING:
//                Intent i = new Intent(context, SoundService.class);
//                i.setAction(SoundService.ACTION_CMD);
//                i.putExtra(SoundService.CMD_NAME, SoundService.CMD_STOP_CASTING);
//                mService.startService(i);
//                break;
//            default:
//                Log.w(TAG, "Unknown intent ignored. Action=" + action);
//        }
//    }
//}
