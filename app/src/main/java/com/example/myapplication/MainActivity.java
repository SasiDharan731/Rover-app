/*
 * Author: Sasidharan
 * Created on: Nov 12-2020
 * Description: A simple rover app
 */

package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    //Giving basic network credentials, IP and Port number.
    public static final int SERVER_PORT = 2112;
    public static final String SERVER_IP = "192.168.1.75";

    //Handler for changing textView outside of main thread.
    private Handler handler = new Handler();

    //Variable for the connection thread class
    private ConnectionThread connectionThread;

    //XML layouts
    private TextView connection_status;
    private Button connect_to_server;

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Hooks
        connect_to_server = findViewById(R.id.connect);
        ImageView up = findViewById(R.id.up);
        ImageView down = findViewById(R.id.down);
        ImageView left = findViewById(R.id.left);
        ImageView right = findViewById(R.id.right);
        WebView camera = findViewById(R.id.camera_view);

        //The WebView for the camera OnBoard
        camera.setWebViewClient(new WebViewClient());
        camera.getSettings().setLoadsImagesAutomatically(true);
        camera.getSettings().setJavaScriptEnabled(true);

        //The local IP of the pi
        camera.loadUrl("http://192.168.1.75/html/");

        //Making the WebView to NoScroll
        camera.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        //This is the button for starting the connection thread to the pi
        connect_to_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToRobot();
            }
        });

        // The move front button
        up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        moveFront();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stop();
                        return true;
                }
                return false;
            }
        });

        // The move back button
        down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        moveBack();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stop();
                        return true;
                }
                return false;
            }
        });

        // The move left button
        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        moveLeft();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stop();
                        return true;
                }
                return false;
            }
        });

        // The move right button
        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        moveRight();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stop();
                        return true;
                }
                return false;
            }
        });
    }

    // Killing the thread if user decides to close the app
    @Override
    protected void onStop() {
        super.onStop();
        if(connectionThread != null){
            handler.post(new Runnable() {
                @SuppressLint({"ResourceAsColor", "SetTextI18n"})
                @Override
                public void run() {
                    // Changing the textView's text and color to red
                    connection_status = findViewById(R.id.connection_status);
                    connection_status.setText("Not Connected");
                    connection_status.setTextColor(Color.parseColor("#FF2A2A"));
                    connect_to_server.setEnabled(true);
                }
            });

            // Sending a disconnect message as "d" to the pi to close the socket connection
            connectionThread.sendMessage("d");

            // Completely closing the connection by setting this thread to null
            connectionThread = null;
        }
    }

    //Function for connecting to the robot
    private void connectToRobot() {
        connectionThread = new ConnectionThread();
        Thread thread = new Thread(connectionThread);
        thread.start();
    }

    // This is for stopping the robot
    private void stop() {
        if(connectionThread != null){
            connectionThread.sendMessage("stop");
            connect_to_server.setEnabled(false);
        }
    }

    // ___________________________ ***** Start of Methods responsible for moving the rover ***** ___________________________ //

    private void moveFront() {
        if(connectionThread != null){
            connectionThread.sendMessage("front");
            connect_to_server.setEnabled(false);
        }
    }

    private void moveBack(){
        if(connectionThread != null){
            connectionThread.sendMessage("back");
            connect_to_server.setEnabled(false);
        }
    }

    private void moveLeft(){
        if(connectionThread != null){
            connectionThread.sendMessage("left");
            connect_to_server.setEnabled(false);
        }
    }

    private void moveRight(){
        if(connectionThread != null){
            connectionThread.sendMessage("right");
        }
    }

    // ___________________________ ***** End of Methods responsible for moving the rover ***** ___________________________ //



    // This is the connection thread class for connecting to the pi and sending commands to it
    class ConnectionThread implements Runnable {

        private Socket socket;

        @Override
        public void run() {

            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);

                // Checking if the connection is established
                if(socket.isConnected()) {
                    // Changing the textView to green and telling the user they are connected to the rover
                    handler.post(new Runnable() {

                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            connection_status = findViewById(R.id.connection_status);
                            connection_status.setText("Connected");
                            connection_status.setTextColor(Color.parseColor("#2AFF2A"));
                            connect_to_server.setEnabled(false);
                        }
                    });
                }else{
                    // Changing the textView to red and telling the user they are not connected to the rover
                    handler.post(new Runnable() {

                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            connection_status = findViewById(R.id.connection_status);
                            connection_status.setText("Not Connected");
                            connection_status.setTextColor(Color.parseColor("#2AFF2A"));
                            connect_to_server.setEnabled(true);
                        }
                    });
                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        void sendMessage(final String message) {
            // This is the thread for sending message to the pi
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (socket != null) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    // For warning the users when going out of the app as the thread will be killed
    @Override
    public void onBackPressed() {
        // Building an alert for going back
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // Warning dialog message
        builder.setMessage("Are you sure that you want to exit?, \n" +
                "Robot will be disconnected.");

        // The cancel button labeled as 'No'
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        // The confirm exit button labeled as 'Yes'
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}