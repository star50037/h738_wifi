package com.example.user.h738_wifi;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    boolean receiveThreadRunning = false;
    //For debugging, always a good idea to have defined
    private static String TAG = "TCPClient";

    private String severIp = null;
    private int serverPort = 0;
    private Socket connectionSocket = null;
    private ReceiveRunnable receiveRunnable;
    private Thread receiveThread;
    private String msg_data;
    private int msd_data_lng = 0;
    //  layout item
    private EditText etIP;
    private EditText etPort;
    private TextView V1, V2, V3, V1_value, V2_value, V3_value;
    private Button Link, Stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etIP = findViewById(R.id.IP);
        etPort = findViewById(R.id.Port);
        Link = findViewById(R.id.btn_link);
        Stop = findViewById(R.id.btn_stop);
        V1 = findViewById(R.id.text_V1);
        V1_value = findViewById(R.id.V1_value);
        V2 = findViewById(R.id.text_V2);
        V2_value = findViewById(R.id.V2_value);
        V3 = findViewById(R.id.text_V3);
        V3_value = findViewById(R.id.V3_value);
        Link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new ConnectRunnable()).start();
            }
        });
        Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });
    }

    /**
     * Returns true if TCPClient is connected, else false
     *
     * @return Boolean
     */
    public boolean isConnected() {
        return connectionSocket != null && connectionSocket.isConnected() && !connectionSocket.isClosed();
    }

    /**
     * Close connection to server
     */
    public void Disconnect() {
        stopThreads();
        try {
            connectionSocket.close();
            Log.d(TAG, "Disconnected!");
        } catch (IOException e) {
            Log.d(TAG, "cant Disconnected!");
        }
    }


    private void stopThreads() {
        if (receiveThread != null)
            receiveThread.interrupt();
    }


    public class ConnectRunnable implements Runnable {
        public void run() {
            severIp = etIP.getText().toString();
            serverPort = Integer.parseInt(etPort.getText().toString());
            try {
                Log.d(TAG, "C: Connecting...");
                //Create a new instance of Socket
                connectionSocket = new Socket(severIp, serverPort);
                //Start connecting to the server with 5000ms timeout
                connect();
            } catch (Exception e) {
                Log.e(TAG, "Error");
                //Catch the exception that socket.connect might throw
            }
            Log.d(TAG, "Connetion thread stopped");
        }
    }

    public void connect() {
        try {
            startReceiving();
        } catch (Exception e) {
            Log.e("Receive", "S: Error", e);
        }
    }

    private void startReceiving() {
        receiveRunnable = new ReceiveRunnable(connectionSocket);
        receiveThread = new Thread(receiveRunnable);
        receiveThread.start();
    }

    public class ReceiveRunnable implements Runnable {
        private Socket RcvSock;
        private InputStream input;

        ReceiveRunnable(Socket server) {
            RcvSock = server;
            try {
                input = RcvSock.getInputStream();
            } catch (Exception e) {
                Log.d(TAG, "cant rcv!");
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if (!receiveThreadRunning)
                    receiveThreadRunning = true;
                try {
                    byte data[] = new byte[16];
                    int read;
                    InputStream bis = new BufferedInputStream(input);
                    read = bis.read(data);
                    if (read > 0) {
                        msg_data = new String(data);
                        Log.d(TAG, msg_data);
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                } catch (Exception e) {
                    Disconnect();
                }
            }
        }
    }


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    msd_data_lng = msg_data.length();
                    if (msd_data_lng >= 12) {
                        int msg_v1 = msg_data.indexOf("V1");
                        viewshow(msg_v1, msg_data, V1_value);
                        int msg_v2 = msg_data.indexOf("V2");
                        viewshow(msg_v2, msg_data, V2_value);
                        int msg_v3 = msg_data.indexOf("V3");
                        viewshow(msg_v3, msg_data, V3_value);
                    }
                    break;
            }
        }
    };

    public void viewshow(int Int, String data, TextView item) {
        if (Int != -1) {
            String string;
            string = data.substring(Int + 3, Int + 6);
            string = string.substring(0, 3);
            double v2 = (Double.parseDouble(string)) / 10;
            string = Double.toString(v2);
            item.setText(string);
        }

    }

}
