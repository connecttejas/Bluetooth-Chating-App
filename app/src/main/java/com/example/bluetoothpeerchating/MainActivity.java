package com.example.bluetoothpeerchating;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button listen,listDevices,send;
    TextView mode,msgText;
    ListView listView;
    EditText editText;
    BluetoothAdapter adapter;
    BluetoothDevice[]   btArray;
    SendReceive sendReceive;


    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    int REQUEST_ENABLE_BLUETOOTH= 1;
    private static  final String APP_NAME = "BlueChat";
    private static final UUID MY_UUID   = UUID.fromString("ec99bea3-3e7c-4e90-9068-8fc77cfad215");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        fvbDefine();
        adapter =   BluetoothAdapter.getDefaultAdapter();
        enableBluetooth();

        implementListeners();



    }

    Handler handler =   new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {

            switch (message.what){
                case STATE_LISTENING:
                    mode.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    mode.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    mode.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    mode.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) message.obj;
                    String tempMsg  =   new String(readBuff,0,message.arg1);
                    msgText.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void enableBluetooth() {
        if(!adapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Enabling Bluetooth",Toast.LENGTH_SHORT);
            Intent enableIntent   =   new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }
    }
    private void implementListeners() {

        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> pairedDevices  =   adapter.getBondedDevices();
                List<String> names  =   new ArrayList<>();
                btArray =   new BluetoothDevice[pairedDevices.size()];
                int index=0;
                if (pairedDevices.size() >0){
                    for (BluetoothDevice device : pairedDevices){

                        btArray[index]=device;
                        names.add(String.valueOf(device.getName())+" , "+String.valueOf(device.getAddress()));
                        index++;
                    }
                }
                listView.setAdapter(new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,names));




            }
        });

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass     =   new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass =    new ClientClass(btArray[i]);
                clientClass.start();

                // mode.setText("Connecting");
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string   = String.valueOf(editText.getText());
                sendReceive.write(string.getBytes());
                editText.setText("");

            }
        });
    }
    private void permissionCheck() {
        // Quick permission check
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {

            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }

    }
    private void fvbDefine() {
        listen  = findViewById(R.id.buttonListen);
        listDevices =   findViewById(R.id.buttonListDevices);
        mode =   findViewById(R.id.textViewMode);
        listView =   findViewById(R.id.listView);
        editText =   findViewById(R.id.editText);
        send =   findViewById(R.id.buttonSend);
        msgText =   findViewById(R.id.textViewMsg);
    }

    private class ServerClass extends Thread{
        BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket=adapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket=null;

            while(socket==null){

                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=serverSocket.accept();
                } catch (Exception e) {  e.printStackTrace();
                    Message message =       Message.obtain();
                    message.what    =   STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }


                if (socket!=null){
                    Message message =       Message.obtain();
                    message.what    =   STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive =   new SendReceive(socket);
                    sendReceive.start();
                    break;

                }

            }
        }
    }

    private class ClientClass   extends     Thread{
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1){
            device=device1;
            try {
                socket  =   device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive =   new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }

        }

    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;


        private SendReceive(BluetoothSocket socket) {
            this.bluetoothSocket = socket;

            InputStream tempIn  =   null;
            OutputStream tempOut = null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        @Override
        public void run() {

            byte[] buffer   =   new byte[1024];
            int bytes;
            while(true){
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public  void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}




