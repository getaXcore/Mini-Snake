package com.wordpress.getaufansepta.snake;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Taufan Septaufani on 12-Jan-18.
 */

public class SnakeView extends TileView {
    private static final String TAG = "SnakeView";

    /**
     * Current mode of application: READY to run, RUNNING, or you have already
     * lost. static final ints are used instead of an enum for performance
     * reasons.
     */
    public static int mMode = 1;
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;
    public static final int LOSE = 3;

    /**
     * Current direction the snake is headed.
     */
    public static int mDirection = 1;
    public static int mNextDirection = 1;
    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST = 3;
    public static final int WEST = 4;

    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    private static final int TILE_GREEN = 1;
    private static final int TILE_ORANGE = 2;
    private static final int TILE_OLDGREEN = 3;

    /**
     * mScore: used to track the number of apples captured mMoveDelay: number of
     * milliseconds between snake movements. This will decrease as apples are
     * captured.
     * version 1.0 mMoveDelay = 600
     */
    private long mScore = 0;
    private long mMoveDelay = 200;
    /**
     * mLastMove: tracks the absolute time when the snake last moved, and is
     * used to determine if a move should be made based on mMoveDelay.
     */
    private long mLastMove;

    /**
     * mStatusText: text shows to the user in some run states
     */
    private TextView mStatusText;

    /**
     * mSnakeTrail: a list of Coordinates that make up the snake's body
     * mAppleList: the secret location of the juicy apples the snake craves.
     */
    private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
    private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();

    /**
     * Everyone needs a little randomness in their life
     */
    private static final Random RNG = new Random();

    /**
     * Create a simple handler that we can use to cause animation to happen. We
     * set ourselves as a target and we can use the sleep() function to cause an
     * update/invalidate to occur at a later date.
     */
    private RefreshHandler mRedrawHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            SnakeView.this.update();
            SnakeView.this.invalidate();
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

    /**
     * Constructs a SnakeView based on inflation from XML
     *
     * @param context
     * @param attrs
     */
    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSnakeView();
    }

    public SnakeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSnakeView();
    }

    private void initSnakeView() {
        setFocusable(true);

        Resources r = this.getContext().getResources();

        resetTiles(4);
        loadTile(TILE_OLDGREEN, r.getDrawable(R.drawable.oldgreen));
        loadTile(TILE_ORANGE, r.getDrawable(R.drawable.orange));
        loadTile(TILE_GREEN, r.getDrawable(R.drawable.green));

    }

    /**
     * version 1.0 default position in initNewGame()
     * mSnakeTrail.add(new Coordinate(7, 7))
     * mSnakeTrail.add(new Coordinate(6, 7))
     * mSnakeTrail.add(new Coordinate(5, 7))
     * mSnakeTrail.add(new Coordinate(4, 7))
     * mSnakeTrail.add(new Coordinate(3, 7))
     * mSnakeTrail.add(new Coordinate(2, 7))
     * in the corner
     *
     * mNextDirection = NORTH
     */

    public void initNewGame() {
        mSnakeTrail.clear();
        mAppleList.clear();

        // For now we're just going to load up a short default eastbound snake
        // that's just turned north

        mSnakeTrail.add(new Coordinate(30,30));
        mSnakeTrail.add(new Coordinate(29,30));
        mSnakeTrail.add(new Coordinate(28,30));
        mSnakeTrail.add(new Coordinate(27,30));
        mSnakeTrail.add(new Coordinate(26,30));
        mSnakeTrail.add(new Coordinate(25,30));
        mNextDirection = SOUTH;

        // Two apples to start with
        addRandomApple();
        addRandomApple();

        mMoveDelay = 200;
        mScore = 0;
    }

    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array
     * of ints before we can stuff them into a map for flattening and storage.
     *
     * @param cvec
     *            : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates as
     *         [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * Save game state so that the user does not lose anything if the game
     * process is killed while we are in the background.
     *
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
        map.putInt("mDirection", Integer.valueOf(mDirection));
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));

        return map;
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a
     * ArrayList of Coordinate objects
     *
     * @param rawArray
     *            : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Restore game state if our process is being relaunched
     *
     * @param icicle
     *            a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setMode(PAUSE);

        mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mDirection = icicle.getInt("mDirection");
        mNextDirection = icicle.getInt("mNextDirection");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
    }

    /*
     * handles key events in the game. Update the direction our snake is
     * traveling based on the DPAD. Ignore events that would cause the snake to
     * immediately turn back on itself.
     *
     * (non-Javadoc)
     *
     * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mMode == READY | mMode == LOSE) {
        /*
         * At the beginning of the game, or the end of a previous one,
         * we should start a new game.
         */
                initNewGame();
                setMode(RUNNING);
                update();
                return (true);
            }

            if (mMode == PAUSE) {
        /*
         * If the game is merely paused, we should just continue where
         * we left off.
         */
                setMode(RUNNING);
                update();
                return (true);
            }

            if (mDirection != SOUTH) {
                mNextDirection = NORTH;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mDirection != NORTH) {
                mNextDirection = SOUTH;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mDirection != EAST) {
                mNextDirection = WEST;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mDirection != WEST) {
                mNextDirection = EAST;
            }
            return (true);
        }

        return super.onKeyDown(keyCode, msg);
    }

    /**
     * Sets the TextView that will be used to give information (such as "Game
     * Over" to the user.
     *
     * @param newView
     */
    public void setTextView(TextView newView) {
        mStatusText = newView;
    }

    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the
     * like) as well as sets the visibility of textview for notification
     *
     * @param newMode
     */
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;

        if (newMode == RUNNING & oldMode != RUNNING) {
            mStatusText.setVisibility(View.INVISIBLE);
            update();

            //disable ads
            Snake.linearbox.removeView(Snake.adview1);
            Snake.linearbox.removeView(Snake.adView2);
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";
        if (newMode == PAUSE) {
            str = res.getText(R.string.mode_pause);

            //show ads
            Snake.linearbox.removeView(Snake.adview1);
            Snake.linearbox.removeView(Snake.adView2);
            Snake.linearbox.addView(Snake.adview1);
            Snake.linearbox.addView(Snake.adView2);
        }
        if (newMode == READY) {
            str = res.getText(R.string.mode_ready);
        }
        if (newMode == LOSE) {
            str = res.getString(R.string.mode_lose_prefix) + mScore
                    + res.getString(R.string.mode_lose_suffix);

            //show ads
            Snake.linearbox.addView(Snake.adview1);
            Snake.linearbox.addView(Snake.adView2);
        }

        mStatusText.setText(str);
        mStatusText.setVisibility(View.VISIBLE);
    }

    /**
     * Selects a random location within the garden that is not currently covered
     * by the snake. Currently _could_ go into an infinite loop if the snake
     * currently fills the garden, but we'll leave discovery of this prize to a
     * truly excellent snake-player.
     *
     */
    private void addRandomApple() {
        Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 1 + RNG.nextInt(mXTileCount - 2);
            int newY = 1 + RNG.nextInt(mYTileCount - 2);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mAppleList.add(newCoord);
    }

    /**
     * Handles the basic update loop, checking to see if we are in the running
     * state, determining if a move should be made, updating the snake's
     * location.
     */
    public void update() {
        if (mMode == RUNNING) {
            long now = System.currentTimeMillis();

            if (now - mLastMove > mMoveDelay) {
                clearTiles();
                updateWalls();
                updateSnake();
                updateApples();
                mLastMove = now;
            }
            mRedrawHandler.sleep(mMoveDelay);
        }

    }

    /**
     * Draws some walls.
     *
     */
    private void updateWalls() {
        for (int x = 0; x < mXTileCount; x++) {
            setTile(TILE_OLDGREEN, x, 0);
            setTile(TILE_OLDGREEN, x, mYTileCount - 1);
        }
        for (int y = 1; y < mYTileCount - 1; y++) {
            setTile(TILE_OLDGREEN, 0, y);
            setTile(TILE_OLDGREEN, mXTileCount - 1, y);
        }
    }

    /**
     * Draws some apples.
     *
     */
    private void updateApples() {
        for (Coordinate c : mAppleList) {
            setTile(TILE_ORANGE, c.x, c.y);
        }
    }

    /**
     * Figure out which way the snake is going, see if he's run into anything
     * (the walls, himself, or an apple). If he's not going to die, we then add
     * to the front and subtract from the rear in order to simulate motion. If
     * we want to grow him, we don't subtract from the rear.
     *
     */
    private void updateSnake() {
        boolean growSnake = false;

        // grab the snake by the head
        Coordinate head = mSnakeTrail.get(0);
        Coordinate newHead = new Coordinate(1, 1);

        //play sound when moving
        //MediaPlayer mp = MediaPlayer.create(getContext(),R.raw.movetune);
        //mp.setLooping(false);
        //mp.stop();

        mDirection = mNextDirection;

        switch (mDirection) {
            case EAST: {
                newHead = new Coordinate(head.x + 1, head.y);
                //mp.start();
                break;
            }
            case WEST: {
                newHead = new Coordinate(head.x - 1, head.y);
                //mp.start();
                break;
            }
            case NORTH: {
                newHead = new Coordinate(head.x, head.y - 1);
                //mp.start();
                break;
            }
            case SOUTH: {
                newHead = new Coordinate(head.x, head.y + 1);
                //mp.start();
                break;
            }
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
                || (newHead.y > mYTileCount - 2)) {
            setMode(LOSE);
            return;

        }

        // Look for collisions with itself
        int snakelength = mSnakeTrail.size();
        for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
            Coordinate c = mSnakeTrail.get(snakeindex);
            if (c.equals(newHead)) {
                setMode(LOSE);
                return;
            }
        }

        // Look for apples
        int applecount = mAppleList.size();
        for (int appleindex = 0; appleindex < applecount; appleindex++) {
            Coordinate c = mAppleList.get(appleindex);
            if (c.equals(newHead)) {
                mAppleList.remove(c);
                addRandomApple();

                mScore++;
                mMoveDelay *= 0.9;

                growSnake = true;
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(0, newHead);
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.remove(mSnakeTrail.size() - 1);
        }

        int index = 0;
        for (Coordinate c : mSnakeTrail) {
            if (index == 0) {
                setTile(TILE_ORANGE, c.x, c.y);
            } else {
                setTile(TILE_GREEN, c.x, c.y);
            }
            index++;
        }


    }

    /**
     * Simple class containing two integer values and a comparison function.
     * There's probably something I should use instead, but this was quick and
     * easy to build.
     *
     */
    private class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }
}
