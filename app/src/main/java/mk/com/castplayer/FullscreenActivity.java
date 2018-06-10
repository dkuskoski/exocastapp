package mk.com.castplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.View;
import android.widget.Button;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class FullscreenActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String BIP_BOP_1 =
            "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8";

    private static final String SAMPLE_STREAM1 =
            "https://bitdash-a.akamaihd.net/content/MI201109210084_1/" +
                    "m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8";
    private static final String SAMPLE_STREAM2 = "https://github.com/mediaelement/" +
            "mediaelement-files/blob/master/big_buck_bunny.mp4?raw=true";
    private static final String SAMPLE_STREAM3 = "https://ia802508.us.archive.org/5/items/" +
            "testmp3testfile/mpthreetest.mp3";
    private static final String SAMPLE_STREAM4 =
            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8";
    private static final String SAMPLE_STREAM5 =
            "https://mnmedias.api.telequebec.tv/m3u8/29880.m3u8";
    private static final String SAMPLE_STREAM6 =
            "http://184.72.239.149/vod/smil:BigBuckBunny.smil/playlist.m3u8";
    private static final String SAMPLE_STREAM7 =
            "http://www.streambox.fr/playlists/test_001/stream.m3u8";
    private static final String SAMPLE_STREAM8 =
            "https://ia800406.us.archive.org/7/items/5.1SurroundSoundTestFilesVariousFormatsAACAC3MP4DTSWAV/5.1%20Surround%20Sound%20AAC%20Test.mp4";

    private static final String SAMPLE_IMG = "https://images.pexels.com/photos/" +
            "248797/pexels-photo-248797.jpeg";
    private PlayerView mPlayerView;
    private ExoPlayerUtil mExoPlayerUtil;
    private ExoPlayer mPlayer;
    private Uri uri;
    private LoopingMediaSource loopingSource;
    private MediaRouteButton mMediaRouteButton;
    private PlayerControlView mCastControlView;
    private long mPlayerPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mPlayerView = findViewById(R.id.playerView);
        mMediaRouteButton = findViewById(R.id.mediaRouteButton);
        mCastControlView = findViewById(R.id.castControlView);
        mExoPlayerUtil = new ExoPlayerUtil();
        mPlayer = mExoPlayerUtil.createPlayer(getApplicationContext(), mPlayerView);

        mPlayerView.setUseController(true);

        mExoPlayerUtil.prepareChromeCast(mMediaRouteButton, mCastControlView);

        setButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mExoPlayerUtil.getCurrentPlayer() == mPlayer ||
                mExoPlayerUtil.getCurrentPlayer() == null) {
            play(SAMPLE_STREAM1);
            mPlayer.seekTo(mPlayerPosition);
        }
    }

    private void play(@NonNull String url) {
        setArtowrk();
        mExoPlayerUtil.createMediaSourceForChromeCast(url, "Parkour", "Santorini", SAMPLE_IMG);
        if(mExoPlayerUtil.getCurrentPlayer() == null
                || mExoPlayerUtil.getCurrentPlayer() == mPlayer) {
            mPlayer.stop();
            if (url.contains("m3u8")) {
                playHLS(Uri.parse(url));
            } else {
                playMP4(Uri.parse(url));
            }
        } else {
            mExoPlayerUtil.playOnCastPlayer();
        }
    }

    private void setArtowrk() {
        mPlayerView.setDefaultArtwork(Bitmap.createScaledBitmap(BitmapFactory
                .decodeResource(getResources(), R.drawable.sample), 500, 200, false));
        mPlayerView.setUseArtwork(true);
    }

    private void playMP4(Uri uri) {
        DefaultDataSourceFactory dataSourceFactory =
                new DefaultDataSourceFactory(this,
                        com.google.android.exoplayer2.util.Util
                                .getUserAgent(this, "playerMP4"),
                        mExoPlayerUtil.getBandwidthMeter());
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        loopingSource = new LoopingMediaSource(videoSource);

        mPlayer.prepare(loopingSource);
    }

    private void playHLS(Uri uri) {
        this.uri = uri;

        try {
            DefaultDataSourceFactory dataSourceFactory =
                    new DefaultDataSourceFactory(this,
                            com.google.android.exoplayer2.util.Util
                                    .getUserAgent(this, "playerHLS"),
                            mExoPlayerUtil.getBandwidthMeter());

            MediaSource videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri);
            loopingSource = new LoopingMediaSource(videoSource);

            mPlayer.prepare(loopingSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            } else {
                mPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    }

    private void setButtons() {
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            Button b = (Button) v;
            switch (b.getText().toString()) {
                case "1":
                    play(SAMPLE_STREAM1);
                    break;
                case "2":
                    play(SAMPLE_STREAM2);
                    break;
                case "3":
                    play(SAMPLE_STREAM3);
                    break;
                case "4":
                    play(SAMPLE_STREAM4);
                    break;
                case "5":
                    play(SAMPLE_STREAM5);
                    break;
                case "6":
                    play(SAMPLE_STREAM6);
                    break;
                case "7":
                    play(SAMPLE_STREAM7);
                    break;
                case "8":
                    play(SAMPLE_STREAM8);
                    break;
                default:
                    play(SAMPLE_STREAM1);
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlayerPosition = mPlayer.getCurrentPosition();
        mPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExoPlayerUtil.releasePlayers();
    }
}
