package mk.com.castplayer;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

public class ExoPlayerUtil implements Player.EventListener {

    private static final String TAG = ExoPlayerUtil.class.getSimpleName();
    private static final String EXO_PLAYER_NOT_INITIALIZED_ERROR = "ExoPlayer must be initialized" +
            " before prepareChromeCast is called";
    private DefaultBandwidthMeter bandwidthMeter;
    private ExoPlayer mPlayer;
    private CastContext mCastContext;
    private CastPlayer mCastPlayer;
    private Player currentPlayer;
    private MediaRouteButton mMediaRouteButton;
    private PlayerControlView mCastControlView;
    private MediaQueueItem[] mMediaItems;
    private PlayerView mPlayerView;
    private Context mContext;

    public ExoPlayer createPlayer(@NonNull Context mContext,
                                  @NonNull final PlayerView mPlayerView) {
        this.mPlayerView = mPlayerView;
        this.mContext = mContext;
        // Create a default TrackSelector
        bandwidthMeter = new DefaultBandwidthMeter();

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // Create the player
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);

        // Bind the player to the view.
        mPlayerView.setPlayer(mPlayer);

        mPlayer.setPlayWhenReady(true);

        mPlayer.addListener(this);

        return mPlayer;
    }

    public void prepareChromeCast(@NonNull final MediaRouteButton mMediaRouteButton,
                                  @NonNull final PlayerControlView mCastControlView) {

        this.mCastControlView = mCastControlView;
        this.mMediaRouteButton = mMediaRouteButton;

        if (mPlayer == null) {
            throw new RuntimeException(EXO_PLAYER_NOT_INITIALIZED_ERROR);
        }

        setMediatRouteButton();

        createCastPlayer();
    }

    private void createCastPlayer() {
        mCastPlayer = new CastPlayer(mCastContext);
        mCastPlayer.setPlayWhenReady(true);

        mCastControlView.setPlayer(mCastPlayer);

        mCastPlayer.setSessionAvailabilityListener(new CastPlayer.SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                setCurrentPlayer(mCastPlayer);
                mCastPlayer.loadItems(mMediaItems, 0,
                        mPlayer.getCurrentPosition(), CastPlayer.REPEAT_MODE_OFF);
                Log.d(TAG, "CastPlayer onCastSessionAvailable: ");
            }

            @Override
            public void onCastSessionUnavailable() {
                try {
                    setCurrentPlayer(mPlayer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "CastPlayer onCastSessionUnavailable: ");
            }
        });

        CastSession mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
    }

    private void setMediatRouteButton() {
        CastButtonFactory.setUpMediaRouteButton(mContext, mMediaRouteButton);

        mCastContext = CastContext.getSharedInstance(mContext);

        if (mCastContext.getCastState() != CastState.NO_DEVICES_AVAILABLE)
            mMediaRouteButton.setVisibility(View.VISIBLE);

        mCastContext.addCastStateListener(new CastStateListener() {
            @Override
            public void onCastStateChanged(int state) {
                if (state == CastState.NO_DEVICES_AVAILABLE)
                    mMediaRouteButton.setVisibility(View.GONE);
                else {
                    if (mMediaRouteButton.getVisibility() == View.GONE)
                        mMediaRouteButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setCurrentPlayer(Player currentPlayer) {
        if (currentPlayer == this.currentPlayer) {
            return;
        }

        if (currentPlayer == mCastPlayer) {
            mPlayer.setPlayWhenReady(false);
            mPlayerView.setUseController(false);
            mCastControlView.setVisibility(View.VISIBLE);
        } else {
            mPlayerView.setUseController(true);
            mCastControlView.setVisibility(View.GONE);
            try {
                mCastPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.currentPlayer = currentPlayer;
    }

    public void createMediaSourceForChromeCast(String mVideoUrl, String mTitle,
                                               String mName, String mPreviewUrl) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mTitle);
        movieMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, mName);
        movieMetadata.addImage(new WebImage(Uri.parse(mPreviewUrl)));
        MediaInfo mediaInfo = new MediaInfo.Builder(mVideoUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(MimeTypes.VIDEO_UNKNOWN)
                .setMetadata(movieMetadata).build();

        mMediaItems = new MediaQueueItem[]{new MediaQueueItem.Builder(mediaInfo).build()};
    }

    public void playOnCastPlayer() {
        mCastPlayer.loadItems(mMediaItems, 0, 0, CastPlayer.REPEAT_MODE_OFF);
    }

    public DefaultBandwidthMeter getBandwidthMeter() {
        return bandwidthMeter;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    public void releasePlayers() {
        if (mCastPlayer != null) {
            mCastPlayer.addListener(null);
            mCastPlayer.stop();
            mCastPlayer.setSessionAvailabilityListener(null);
            mCastPlayer.release();
            mCastControlView.setPlayer(null);
        }
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
        }
    }
}
