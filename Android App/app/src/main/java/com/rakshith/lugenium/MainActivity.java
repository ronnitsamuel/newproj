package com.rakshith.lugenium;

import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;

public class MainActivity extends AppCompatActivity {

    private CheckBox checkBox;
    private boolean wPressed = false; // These are used for keydown and keyup events. More explained in the joystick listener event
    private boolean aPressed = false;
    private boolean sPressed = false;
    private boolean dPressed = false;
    private Vibrator vibrator; // hehe ;)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        checkBox = (CheckBox) findViewById(R.id.checkBox1);
        Data.loadData(this);
        View[] views = new View[]{
                findViewById(R.id.button5)
        };
        for(View view : views) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        switch (v.getId()) {
                            case R.id.button5:
                                Data.send("keyboard.keydown.f");
                                break;
                        }
                    }
                    else if(event.getAction() == MotionEvent.ACTION_UP){
                        switch (v.getId()) {
                            case R.id.button5:
                                Data.send("keyboard.keyup.f");
                                break;
                        }
                    }
                    return false;
                }
            });
        }
        JoystickView keyboardJoystick = (JoystickView) findViewById(R.id.keyboard_joystick);
        assert keyboardJoystick != null;
        keyboardJoystick.setOnJoystickMovedListener(new JoystickMovedListener() {
            @Override
            public void OnMoved(int x, int y) {
                if (x >= 3) {
                    // This is to ensure that the keydown event is not sent multiple times
                    // Similar stuff for other buttons
                    // This ensures that the keydown / keyup button is sent only once and not multiple times as long as the button is held
                    if (!dPressed) {
                        Data.send("keyboard.keydown.d");
                        dPressed = true;
                    }
                } else if (x <= -3) {
                    if (!aPressed) {
                        Data.send("keyboard.keydown.a");
                        aPressed = true;
                    }
                } else {
                    if (aPressed)
                        Data.send("keyboard.keyup.a");
                    if (dPressed)
                        Data.send("keyboard.keyup.d");
                }
                if (y >= 3) {
                    if (!sPressed) {
                        Data.send("keyboard.keyup.w");
                        wPressed = false;
                        Data.send("keyboard.keydown.s");
                        sPressed = true;
                    }
                } else if (y <= -3) {
                    if (!wPressed) {
                        Data.send("keyboard.keyup.s");
                        sPressed = false;
                        Data.send("keyboard.keydown.w");
                        vibrator.vibrate(50);
                        wPressed = true;
                    }
                } else {
                    if (wPressed)
                        Data.send("keyboard.keyup.w");
                    if (sPressed)
                        Data.send("keyboard.keyup.s");
                }
            }

            @Override
            public void OnReleased() {
                if (wPressed)
                    Data.send("keyboard.keyup.w");
                if (aPressed)
                    Data.send("keyboard.keyup.a");
                if (sPressed)
                    Data.send("keyboard.keyup.s");
                if (dPressed)
                    Data.send("keyboard.keyup.d");
                wPressed = false;
                aPressed = false;
                sPressed = false;
                dPressed = false;
            }
        });

        JoystickView mouseJoystick = (JoystickView) findViewById(R.id.mouse_joystick);
        assert mouseJoystick != null;
        mouseJoystick.setOnJoystickMovedListener(new JoystickMovedListener() {
            @Override
            public void OnMoved(int x, int y) {
                Data.send("mouse.move." + x + "." + y);
            }

            @Override
            public void OnReleased() {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!Data.isConnected()) {
            Data.attemptConnecting();
            checkBox.setChecked(Data.isConnected());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Data.isConnected()) {
            Data.attemptConnecting();
            checkBox.setChecked(Data.isConnected());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void buttonClicked(View view) {
        //switch (view.getId()) {
        //    case R.id.button1:
        //        Data.send("keyboard.keydown.w");
        //        break;
        //    case R.id.button2:
        //        Data.send("a");
        //        break;
        //    case R.id.button3:
        //        Data.send("s");
        //        break;
        //    case R.id.button4:
        //        Data.send("d");
        //        break;
        //    case R.id.button5:
        //        Data.send("f");
        //        break;
        //}
    }

    public void connect(View view) {
        Data.attemptConnecting();
        checkBox.setChecked(Data.isConnected());
        if(Data.isConnected())
            Data.sendData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Intent intent = new Intent(this, SettingsActivity.class);
            //startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}