package com.lazy.mediasessiontest.service;

import android.support.v4.media.session.PlaybackStateCompat;

/**
 * author : xu
 * date : 2021/2/24 16:43
 * description :
 */
public abstract class PlaybackInfoListener {

    public abstract void onPlaybackStateChange(PlaybackStateCompat state);

    public void onPlaybackCompleted() {
    }
}
