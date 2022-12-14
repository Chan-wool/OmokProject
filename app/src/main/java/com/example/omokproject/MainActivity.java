package com.example.omokproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.Buffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView map_elem;
    GridLayout map;
    TextView[][] textViews = new TextView[13][13];
    int giveId = 0;

    int myTurn;
    public boolean gameStart = false;
    public boolean gameClose = false;

    String host;
    int port;
    boolean isEntered = true;

    ValueHandler handler = new ValueHandler(); //??????????????? ??? ???????????? ???????????? ??????
    Socket socket;
    //netWork ?????????
    PrintWriter writer; //??????
    BufferedReader reader; //??????

    private static final int REQUEST_PHONE_STATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        host = intent.getStringExtra("host");
        port = intent.getIntExtra("port",0);

        //ServerSocket??????
        final ClientThread thread = new ClientThread();
        thread.start();


        //region ????????? ?????????
        map = findViewById(R.id.gridLayoutMain);
        map.setBackgroundColor(Color.parseColor("#E5CC94"));
        for(int i=0;i<13;i++) {
            for(int j=0; j<13; j++){
                map_elem = new TextView(getApplicationContext());
                map_elem.setText(" ");
                Resources res = getResources();
                Drawable shape = ResourcesCompat.getDrawable(res, R.drawable.border, getTheme());
                map_elem.setBackground(shape);
                map_elem.setMaxWidth(70);
                map_elem.setMaxHeight(70);
                map_elem.setBackground(shape);
                map_elem.setId(giveId);
                map.addView(map_elem);

                textViews[i][j] = map_elem;
                textViews[i][j] = findViewById(giveId++);
                final int finalI = i;
                final int finalJ = j;

                //region Tile ?????? ?????????
                textViews[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!gameStart){
                            Log.d("Game", "gameStart:"+String.valueOf(gameStart));
                            Toast.makeText(MainActivity.this,"????????? ?????? ???????????? ???????????????.",Toast.LENGTH_SHORT).show();
                        }else{
                            new Thread(){
                                @Override
                                public void run() {
                                    thread.sendValue("VALUE:"+finalI+","+finalJ+":"+myTurn);
                                }
                            }.start();
                        }


                    }
                });
                //endregion

            }
        }
        //endregion

    }



    //??????????????? ?????? ??????
    class ClientThread extends Thread {

        @Override
        public void run() {
            try {
                socket = new Socket(host, port);

                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // ???????????? ???
                if(isEntered){
                    sendValue("ENTER:Client["+getLocalIpAddress()+"] is Entered");
                }

                while(true){
                    //????????? ????????? handler??? ??????
                    String input = reader.readLine();
                    Log.d("ClientThread","Received data: "+input);
                    Bundle bundle = new Bundle();
                    bundle.putString("value",input);
                    Message msg = new Message();
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }

            } catch (IOException e){
                e.printStackTrace();
                closeConnection();
            } catch (Exception e){
                e.printStackTrace();
                closeConnection();
            } finally {
                closeConnection();
            }
        }

        //????????? ????????? ??????
        public void sendValue(String data){
            try{
                //writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                Log.d("ClientStream", data);
                Log.d("ClientStream","Send to server ...");
                writer.println(data);
                writer.flush();

            } catch (Exception e){
                e.printStackTrace();
            }
        }

        //????????????
        public void closeConnection(){
            try {
                writer.close();
                reader.close();
                socket.close();
                Log.d("ClientStream", "Close connection from server");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    class ValueHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            String inputData = msg.getData().getString("value");
            String protocol = inputData.split(":")[0]; //protocol ??????


            switch (protocol){
                case "ORDER":
                    myTurn = Integer.parseInt(inputData.split(":")[1]);
                    Log.d("Input","myTurn: "+myTurn);
                    break;
                case "VALUE":
                    String value = inputData.split(":")[1];
                    int i = Integer.parseInt(value.split(",")[0]);
                    int j = Integer.parseInt(value.split(",")[1]);
                    int turn = Integer.parseInt(inputData.split(":")[2]);
                    if(turn == 0) {
                        textViews[i][j].setBackgroundColor(Color.BLACK);
                    } else {
                        textViews[i][j].setBackgroundColor(Color.WHITE);
                    }
                    break;
                case "START":
                    gameStart = true;
                    Log.d("Game", "gameStart: "+String.valueOf(gameStart));

                    Toast.makeText(MainActivity.this,"????????? ???????????????.",Toast.LENGTH_SHORT).show();
                    break;
                case "RESULT":
                    gameClose = true;
                    Log.d("Game","????????? ?????????????????????");
                    String result = inputData.split(":")[1];
                    if(result.startsWith("Win")){//????????????
                        if(myTurn == 0){
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                            final View view = factory.inflate(R.layout.alertvictory, null);
                            builder.setTitle("??? ??????!")
                                    .setMessage("??? ?????????????????????! ????????? ????????????")
                                    .setView(view)
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent mainIntent = new Intent(getApplicationContext(), StartActivity.class);
                                            startActivity(mainIntent);
                                        }
                                    })
                                    .show();
                            try {
                                socket.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }else{
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                            final View view = factory.inflate(R.layout.alertclose, null);
                            builder.setTitle("??? ??????!")
                                    .setMessage("??? ?????????????????????! ????????? ????????????")
                                    .setView(view)
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent mainIntent = new Intent(getApplicationContext(), StartActivity.class);
                                            startActivity(mainIntent);
                                        }
                                    })
                                    .show();
                            try {
                                socket.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }else{
                        if(myTurn == 0){
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                            final View view = factory.inflate(R.layout.alertvictory, null);
                            builder.setTitle("??? ??????!")
                                    .setMessage("??? ?????????????????????! ????????? ????????????")
                                    .setView(view)
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent mainIntent = new Intent(getApplicationContext(), StartActivity.class);
                                            startActivity(mainIntent);
                                        }
                                    })
                                    .show();
                            try {
                                socket.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }else{
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                            final View view = factory.inflate(R.layout.alertclose, null);
                            builder.setTitle("??????....")
                                    .setMessage("?????? ????????????....! ????????? ????????????")
                                    .setView(view)
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent mainIntent = new Intent(getApplicationContext(), StartActivity.class);
                                            startActivity(mainIntent);
                                        }
                                    })
                                    .show();
                            try {
                                socket.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                default:
                    Toast.makeText(MainActivity.this, inputData,Toast.LENGTH_SHORT).show();

            }

//            int i = Integer.parseInt(value.split(",")[0]);
//            int j = Integer.parseInt(value.split(",")[1]);
//
//            if(myColor == "black"){
//                textViews[i][j].setBackgroundColor(Color.WHITE);
//                pan[i][j] = whiteTiles;
//            } else if (myColor == "white"){
//                textViews[i][j].setBackgroundColor(Color.BLACK);
//                pan[i][j] = blackTiles;
//            }

        }

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("??????...?????????...?")
                .setMessage("??????....???????????????...? ?????? ?????? :(")
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finishAffinity();
                        System.runFinalization();
                        System.exit(0);
                    }
                })
                .setNegativeButton("??????", null)
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

}