package com.lazy.mediasessiontest.service;

/**
 * author : xu
 * date : 2021/2/24 16:41
 * description :媒体浏览器服务   音乐播放器服务
 */

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.lazy.mediasessiontest.service.notifications.MediaNotificationManager;
import com.lazy.mediasessiontest.service.players.MediaPlayerAdapter;
import com.lazy.mediasessiontest.service.contentcatalogs.MusicLibrary;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = MusicService.class.getSimpleName();
    /**
     * 媒体会话
     */
    private MediaSessionCompat mSession;
    /**
     * mediaPlayer 播放器
     */
    private PlayerAdapter mPlayback;
    /**
     * 通知
     */
    private MediaNotificationManager mMediaNotificationManager;
    private boolean mServiceInStartedState;

    @Override
    public void onCreate() {
        super.onCreate();
        //创建媒体会话
        mSession = new MediaSessionCompat(this, "MusicService");
        MediaSessionCallback mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);
        // 服务里  持有一个播放器
        mPlayback = new MediaPlayerAdapter(this, new MediaPlayerListener());
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotificationManager");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mPlayback.stop();
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }
    /**
     * 返回内容层次结构的根节点。如果该方法返回 null，则会拒绝连接。
     *
     * @param clientPackageName
     * @param clientUid
     * @param rootHints
     * @return
     */
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        return new BrowserRoot(MusicLibrary.getRoot(), null);
    }

    @Override
    public void onLoadChildren(
            @NonNull final String parentMediaId,
            @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(MusicLibrary.getMediaItems());
    }

    /**
     * 媒体会话回调： TransportControls 控制 LemonMediaPlayer  进而控制mediaPlayer
     */
    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.e("MediaSessionCallback", "----------------onMediaButtonEvent");
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.e("MediaSessionCallback", "----------------onAddQueueItem");
            mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.e("MediaSessionCallback", "----------------onRemoveQueueItem");
            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }

        @Override
        public void onPrepare() {
            Log.e("MediaSessionCallback", "----------------onPrepare");
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = MusicLibrary.getMetadata(MusicService.this, mediaId);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            Log.e("MediaSessionCallback", "----------------onPlay");
            if (!isReadyToPlay()) {
                // Nothing to play.
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);
            Log.d(TAG, "onPlayFromMediaId: MediaSession active");
        }

        @Override
        public void onPause() {
            Log.e("MediaSessionCallback", "----------------onPause");
            mPlayback.pause();
            Log.d(TAG, "TransportControls给 会话的回调: MediaSession onPause");
        }

        @Override
        public void onStop() {
            Log.e("MediaSessionCallback", "----------------onStop");
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            Log.e("MediaSessionCallback", "----------------onSkipToNext");
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Log.e("MediaSessionCallback", "----------------onSkipToPrevious");
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            Log.e("MediaSessionCallback", "----------------onSeekTo");
            mPlayback.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            return (!mPlaylist.isEmpty());
        }
    }

    /**
     * LemonMediaPlayer Callback: LemonMediaPlayer state -> MediaBrowserService.
     * 媒体播放器回调    播放状态 给媒体会话  和  媒体浏览器服务
     */
    public class MediaPlayerListener extends PlaybackInfoListener {

        private final ServiceManager mServiceManager;

        MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }
        /**
         * 播放器状态改变 通知 Notification  和 媒体回话改变状态
         *
         * @param state
         */
        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }

        class ServiceManager {
            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification =
                        mMediaNotificationManager.getNotification(mPlayback.getCurrentMedia(), state, getSessionToken());
                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                }

                Log.e(TAG, "------------------moveServiceToStartedState" + state.getState());
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());
                mMediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                Log.e(TAG, "------------------updateNotificationForPause" + state.getState());
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
            }
        }

    }

}