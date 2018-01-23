package com.wordpress.getaufansepta.snake;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class Snake extends AppCompatActivity {
    private SnakeView mSnakeView;
    private static String ICICLE_KEY = "snake-view";
    public static AdView adview1,adView2;
    public static LinearLayout linearbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature( Window.FEATURE_NO_TITLE );
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_snake);

        mSnakeView = (SnakeView) findViewById(R.id.snake);
        mSnakeView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            mSnakeView.setMode(SnakeView.READY);
        } else {
            // We are being restored
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                mSnakeView.restoreState(map);
            } else {
                mSnakeView.setMode(SnakeView.PAUSE);
            }
        }

        adview1 = (AdView)findViewById(R.id.adview1);
        adView2 = (AdView)findViewById(R.id.adview2);
        linearbox = (LinearLayout)findViewById(R.id.linearbox);

        //view ads
        AdRequest adview = new AdRequest.Builder().build();
        adview1.loadAd(adview);
        adView2.loadAd(adview);

        /*linearbox.addView(adview1);
        linearbox.addView(adView2);*/


        //Handle game controller
        setControll();

    }

    private void setControll() {
        mSnakeView.setOnTouchListener(new OnSwipeTouchListener(Snake.this){
            @Override
            public void onSwipeTop() {
                //Toast.makeText(Snake.this,"top",Toast.LENGTH_SHORT).show();
                if (SnakeView.mMode == SnakeView.READY || SnakeView.mMode == SnakeView.LOSE){
                    mSnakeView.initNewGame();
                    mSnakeView.setMode(SnakeView.RUNNING);
                    mSnakeView.update();
                }
                if (SnakeView.mMode == SnakeView.PAUSE){
                    mSnakeView.setMode(SnakeView.RUNNING);
                    mSnakeView.update();
                }
                if (SnakeView.mDirection != SnakeView.SOUTH ){
                    SnakeView.mNextDirection = SnakeView.NORTH;
                }
            }
            @Override
            public void onSwipeBottom() {
                //Toast.makeText(Snake.this,"bottom",Toast.LENGTH_SHORT).show();
                if (SnakeView.mDirection != SnakeView.NORTH){
                    SnakeView.mNextDirection = SnakeView.SOUTH;
                }
            }
            @Override
            public void onSwipeLeft() {
                //Toast.makeText(Snake.this,"left",Toast.LENGTH_SHORT).show();
                if (SnakeView.mDirection != SnakeView.EAST){
                    SnakeView.mNextDirection = SnakeView.WEST;
                }
            }

            @Override
            public void onSwipeRight() {
                //Toast.makeText(Snake.this,"right",Toast.LENGTH_SHORT).show();
                if (SnakeView.mDirection != SnakeView.WEST){
                    SnakeView.mNextDirection = SnakeView.EAST;
                }
            }

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mSnakeView.setMode(SnakeView.PAUSE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the game state
        super.onSaveInstanceState(outState);
        outState.putBundle(ICICLE_KEY, mSnakeView.saveState());
    }


}
