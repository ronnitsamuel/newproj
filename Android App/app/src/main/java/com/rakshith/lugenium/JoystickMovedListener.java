package com.rakshith.lugenium;

/**
 * Created by Rakshith on 19-09-2015.
 */
public interface JoystickMovedListener {
    void OnMoved(int pan, int tilt);
    void OnReleased();
}