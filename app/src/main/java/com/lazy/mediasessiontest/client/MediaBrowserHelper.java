package com.lazy.mediasessiontest.client;


import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

/**
 * Helper class for a MediaBrowser that handles connecting, disconnecting,
 * and basic browsing with simplified callbacks.
 */
/**
 * author : xu
 * date : 2021/2/24 16:34
 * description : 将媒体浏览器 与媒体浏览器服务链接  为界面创建媒体控制器
 */
public class MediaBrowserHelper {

    private static final String TAG = MediaBrowserHelper.class.getSimpleName();

    private final Context mContext;
    /**
     * 媒体浏览器服务class   其实就是 带有音乐播放器的服务
     */
    private final Class<? extends MediaBrowserServiceCompat> mMediaBrowserServiceClass;

    /**
     * 媒体控制器回调命令
     */
    private final List<MediaControllerCompat.Callback> mCallbackList = new ArrayList<>();
    /**
     * 自定义媒体浏览器 链接回调
     * 媒体浏览器 MediaBrowserCompat 与 媒体浏览器服务MediaBrowserServiceCompat  链接的回调
     */
    private final MediaBrowserConnectionCallback mMediaBrowserConnectionCallback;
    /**
     * 媒体控制器回调
     */
    private final MediaControllerCallback mMediaControllerCallback;
    /**
     * 自定义媒体浏览器  订阅数据回调
     */
    private final MediaBrowserSubscriptionCallback mMediaBrowserSubscriptionCallback;

    /**
     * 媒体浏览器 客户端    浏览由MediaBrowserServiceCompat 提供的媒体内容
     * 功能 与媒体浏览器服务 链接
     * 为界面创建媒体控制器MediaController
     */
    private MediaBrowserCompat mMediaBrowser;
    /**
     * 媒体控制器
     */
    @Nullable
    private MediaControllerCompat mMediaController;

    public MediaBrowserHelper(Context context,
                              Class<? extends MediaBrowserServiceCompat> serviceClass) {
        mContext = context;
        mMediaBrowserServiceClass = serviceClass;
        // 链接回调
        mMediaBrowserConnectionCallback = new MediaBrowserConnectionCallback();
        // 控制器回调
        mMediaControllerCallback = new MediaControllerCallback();
        // MediaBrowser 订阅数据回调
        mMediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
    }

    /**
     * 媒体浏览器 与 媒体浏览器服务建立链接
     */
    public void onStart() {
        if (mMediaBrowser == null) {
            mMediaBrowser = new MediaBrowserCompat(
                    mContext, new ComponentName(mContext, mMediaBrowserServiceClass),
                    mMediaBrowserConnectionCallback, null);
            mMediaBrowser.connect();
        }
        Log.d(TAG, "onStart: Creating MediaBrowser, and connecting");
    }

    /**
     * 解除链接
     */
    public void onStop() {
        // 解除控制器回调
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
        }
        // 解除媒体浏览器链接
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }
        resetState();
        Log.d(TAG, "onStop: Releasing MediaController, Disconnecting from MediaBrowser");
    }

    /**
     * 链接后调用
     * Called after connecting with a {@link MediaBrowserServiceCompat}.
     * <p>
     * Override to perform processing after a connection is established.
     *
     * @param mediaController {@link MediaControllerCompat} associated with the connected
     *                        MediaSession.
     */
    protected void onConnected(@NonNull MediaControllerCompat mediaController) {
    }

    /**
     * 加载可浏览文件后调用 Called after loading a browsable {@link MediaBrowserCompat.MediaItem}
     *
     * @param parentId The media ID of the parent item.
     * @param children List (possibly empty) of child items.
     */
    protected void onChildrenLoaded(@NonNull String parentId,
                                    @NonNull List<MediaBrowserCompat.MediaItem> children) {
    }

    /**
     * 媒体浏览器  与  媒体浏览器服务 断开链接 Called when the {@link MediaBrowserServiceCompat} connection is lost.
     */
    protected void onDisconnected() {
    }

    /**
     * 获取当前 媒体控制 器 MediaControllerCompat
     *
     * @return
     */
    @NonNull
    protected final MediaControllerCompat getMediaController() {
        if (mMediaController == null) {
            throw new IllegalStateException("MediaController is null!");
        }
        return mMediaController;
    }

    /**
     * 重置  暂停  播放状态
     */
    private void resetState() {
        performOnAllCallbacks(new CallbackCommand() {
            @Override
            public void perform(@NonNull MediaControllerCompat.Callback callback) {
                callback.onPlaybackStateChanged(null);
            }
        });
        Log.d(TAG, "resetState: ");
    }

    /**
     * 获取媒体控制控件   TransportControls 方法向您的服务的媒体会话发送回调。确保您为每个控件定义了相应的
     * MediaSessionCompat.Callback 方法。
     * ui 控件 通过 这个 TransportControls  （媒体控制控件）控制方法  给媒体会话回调
     *
     * @return
     */
    public MediaControllerCompat.TransportControls getTransportControls() {
        if (mMediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!");
            throw new IllegalStateException("MediaController is null!");
        }
        return mMediaController.getTransportControls();
    }

    /**
     * 注册 页面ui里的媒体控制器回调
     *
     * @param callback
     */
    public void registerCallback(MediaControllerCompat.Callback callback) {
        if (callback != null) {
            mCallbackList.add(callback);

            // 更新最新数据   或者播放状态
            if (mMediaController != null) {
                // 获取媒体控制器里的最新数据 回调给ui
                final MediaMetadataCompat metadata = mMediaController.getMetadata();
                if (metadata != null) {
                    callback.onMetadataChanged(metadata);
                }
                // 获取最新播放状态   回调给ui
                final PlaybackStateCompat playbackState = mMediaController.getPlaybackState();
                if (playbackState != null) {
                    callback.onPlaybackStateChanged(playbackState);
                }
            }
        }
    }

    /**
     * 执行所有回调命令
     *
     * @param command
     */
    private void performOnAllCallbacks(@NonNull CallbackCommand command) {
        for (MediaControllerCompat.Callback callback : mCallbackList) {
            if (callback != null) {
                command.perform(callback);
            }
        }
    }

    /**
     * 定义一个单纯执行媒体控制回调命令 的 接口
     */
    private interface CallbackCommand {
        void perform(@NonNull MediaControllerCompat.Callback callback);
    }

    /**
     * 当MediaBrowser成功连接到MediaBrowserService时，接收来自它的回调
     */
    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens as a result of onStart().
        @Override
        public void onConnected() {
            try {
                // 为媒体会话创建媒体控制器
                mMediaController =
                        new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallback);

                // 将现有MediaSession状态同步到UI。
                mMediaControllerCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaControllerCallback.onPlaybackStateChanged(
                        mMediaController.getPlaybackState());
                // 链接成功后 将控制器 抛出
                MediaBrowserHelper.this.onConnected(mMediaController);
            } catch (RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }
            //  查询中包含的媒体项的信息 指定的id和订阅在更改时接收更新。
            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
        }
    }

    /**
     * 当MediaBrowserService加载新媒体时，接收来自MediaBrowser的回调
     * that is ready for playback.
     */
    public class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        /**
         * 在加载或更新子项列表时调用。
         *
         * @param parentId
         * @param children
         */
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            MediaBrowserHelper.this.onChildrenLoaded(parentId, children);
        }
    }

    /**
     * 接收来自MediaController的回调并更新UI状态，
     */
    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        /**
         * 元数据改变时回调
         *
         * @param metadata
         */
        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            // 通过控制器 传给ui  层 改变ui
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(@NonNull MediaControllerCompat.Callback callback) {
                    callback.onMetadataChanged(metadata);
                }
            });
        }
        /**
         * 控制器播放状态改变时 回调给ui层
         *
         * @param state
         */
        @Override
        public void onPlaybackStateChanged(@Nullable final PlaybackStateCompat state) {
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(@NonNull MediaControllerCompat.Callback callback) {
                    callback.onPlaybackStateChanged(state);
                }
            });
        }

        // 如果MediaBrowserService在活动处于前台时被终止，并且调用了onStart（）（而不是onStop（）），则可能会发生这种情况。
        @Override
        public void onSessionDestroyed() {
            // 重置播放状态
            resetState();
            onPlaybackStateChanged(null);
            // 断开链接
            MediaBrowserHelper.this.onDisconnected();
        }
    }
}
