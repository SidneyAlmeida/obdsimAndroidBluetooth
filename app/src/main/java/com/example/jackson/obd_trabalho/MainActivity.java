package com.example.jackson.obd_trabalho;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final Integer ENABLE_BT = 1;

    public static final int MSG_READ = 2;
    public static final int MSG_WRITE = 3;

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //address map blueetooth do PC
    public static final String address = "70:2C:1F:08:FF:3B";

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;

    AcceptThread acceptThread;
    ConnectedThread connectedThread;
    ExecuteThread executeThread;

    private TextView txtRPM;
    private TextView txtVelocidade;
    private TextView txtBorboleta;
    private TextView txtFluxoAr;
    private TextView txtTemperatura;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtRPM=(TextView) findViewById(R.id.txtRMP);
        txtVelocidade=(TextView) findViewById(R.id.txtVelocidade);
        txtBorboleta=(TextView) findViewById(R.id.txtBorboleta);
        txtFluxoAr=(TextView) findViewById(R.id.txtFluxoAr);
        txtTemperatura=(TextView) findViewById(R.id.txtTemperatura);

    }
    @Override
    protected void onResume()
    {

        super.onResume();
        configureOBD();

    }

    private void configureOBD()
    {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth não suportado", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT);
        }


        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice == null) {
            Toast.makeText(getApplicationContext(), "Disposistivo nulo nao encontrado! ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Erro ao criar Socket", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            bluetoothSocket.connect();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Erro ao conectar ao Socket", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        manageConnectedSocket(bluetoothSocket);
        executeThread = new ExecuteThread();
        executeThread.start();
    }

    private void manageConnectedSocket(BluetoothSocket socket)
    {

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        String msg = "AT E0\r";
        connectedThread.write(msg.getBytes());

        msg = "AT L0\r";
        connectedThread.write(msg.getBytes());

        msg = "AT ST 00\r";
        connectedThread.write(msg.getBytes());

        msg = "AT SP 0\r";
        connectedThread.write(msg.getBytes());

        msg = "AT Z\r";
        connectedThread.write(msg.getBytes());

    }

    Handler mHandler = new Handler()
    {

        @Override
        public void handleMessage(Message msg)
        {

            //Log.i(TAG, "handleMessage");
            switch (msg.what) {
                case MSG_READ:

                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    String[] linhas = readMessage.split("\\r\\n|\\r|\\n", -1);

                    for (int i = 0; i < linhas.length; i++) {
                        String linha = linhas[i];
                        String[] retorno = linha.trim().split(" ");

                        try {
                            if (retorno[0].trim().equals("41")) {

                                if (retorno[1].trim().equals("05")) {
                                    int temp = Integer.parseInt(retorno[2].trim(), 16);
                                    temp = temp - 40;
                                    if(temp >=100){
                                        txtTemperatura.setTextColor(Color.RED);
                                    }else if(temp>=50 && temp<=120){
                                        txtTemperatura.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(temp <50){
                                        txtTemperatura.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtTemperatura.setText("Temperatura: " + temp + "Cº");

                                }
                                if (retorno[1].trim().equals("0C")) {
                                    int p1 = Integer.parseInt(retorno[2].trim(), 16);
                                    int p2 = Integer.parseInt(retorno[3].trim(), 16);
                                    double rpm = (p1 * 256 + p2) / 4;

                                    if(rpm >=12000){
                                        txtRPM.setTextColor(Color.RED);
                                    }else if(rpm>=6000 && rpm<=12000){
                                        txtRPM.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(rpm <6000){
                                        txtRPM.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtRPM.setText("RPM: " + rpm);

                                }
                                if (retorno[1].trim().equals("0D")) {
                                    int vel = Integer.parseInt(retorno[2].trim(), 16);

                                    if(vel >=100){
                                        txtVelocidade.setTextColor(Color.RED);
                                    }else if(vel>=60 && vel<=100){
                                        txtVelocidade.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(vel <60){
                                        txtVelocidade.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtVelocidade.setText("Velocidade: " + vel + " km/h");
                                    //speedometer.setSpeed(vel, 1000, 300);
                                }
                                if (retorno[1].trim().equals("11")) {
                                    int borb = Integer.parseInt(retorno[2].trim(), 16);
                                    borb = borb * 100 / 255;

                                    if(borb >=80){
                                        txtBorboleta.setTextColor(Color.RED);
                                    }else if(borb>=45 && borb<=79){
                                        txtBorboleta.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(borb <45){
                                        txtBorboleta.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtBorboleta.setText("Borboleta: " + borb + "%");

                                }
                                if (retorno[1].trim().equals("10")) {
                                    int p1 = Integer.parseInt(retorno[2].trim(), 16);
                                    int p2 = Integer.parseInt(retorno[3].trim(), 16);
                                    double fluxo = (p1 * 256 + p2) / 100;

                                    if(fluxo >400){
                                        txtFluxoAr.setTextColor(Color.RED);
                                    }else if(fluxo>200 && fluxo<=400){
                                        txtFluxoAr.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(fluxo <=200){
                                        txtFluxoAr.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtFluxoAr.setText("Fluxo de Ar: " + fluxo);
                                }
                            }
                            if (retorno[0].trim().equals("05") || retorno[0].trim().equals("0C") || retorno[0].trim().equals("0D") || retorno[0].trim().equals("11") || retorno[0].trim().equals("10")) {
                                if (retorno[0].trim().equals("05")) {
                                    int temp = Integer.parseInt(retorno[1].trim(), 16);
                                    temp = temp - 40;

                                    if(temp >=180){
                                        txtTemperatura.setTextColor(Color.RED);
                                    }else if(temp>=100 && temp<179){
                                        txtTemperatura.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(temp <100){
                                        txtTemperatura.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtTemperatura.setText("Temperatura: " + temp + "Cº");
                                }
                                if (retorno[0].trim().equals("0C")) {
                                    int p1 = Integer.parseInt(retorno[1].trim(), 16);
                                    int p2 = Integer.parseInt(retorno[2].trim(), 16);
                                    double rpm = (p1 * 256 + p2) / 4;

                                    if(rpm >=100){
                                        txtVelocidade.setTextColor(Color.RED);
                                    }else if(rpm>=60 && rpm<=100){
                                        txtVelocidade.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(rpm <60){
                                        txtVelocidade.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtRPM.setText("RPM: " + rpm);
                                }
                                if (retorno[0].trim().equals("0D")) {
                                    int vel = Integer.parseInt(retorno[1].trim(), 16);

                                    if(vel >=100){
                                        txtVelocidade.setTextColor(Color.RED);
                                    }else if(vel>=60 && vel<=100){
                                        txtVelocidade.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(vel <60){
                                        txtVelocidade.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtVelocidade.setText("Velocidade: " + vel + " km/h");
                                    //speedometer.setSpeed(vel, 1000, 300);
                                }
                                if (retorno[0].trim().equals("11")) {
                                    int borb = Integer.parseInt(retorno[1].trim(), 16);
                                    borb = borb * 100 / 255;

                                    if(borb >=80){
                                        txtBorboleta.setTextColor(Color.RED);
                                    }else if(borb>=45 && borb<=79){
                                        txtBorboleta.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(borb <45){
                                        txtBorboleta.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtBorboleta.setText("Borboleta: " + borb + "%");
                                }
                                if (retorno[0].trim().equals("10")) {
                                    int p1 = Integer.parseInt(retorno[1].trim(), 16);
                                    int p2 = Integer.parseInt(retorno[2].trim(), 16);
                                    double fluxo = (p1 * 256 + p2) / 100;

                                    if(fluxo >400){
                                        txtFluxoAr.setTextColor(Color.RED);
                                    }else if(fluxo>200 && fluxo<=400){
                                        txtFluxoAr.setTextColor(Color.parseColor("#E0BC00"));
                                    }else if(fluxo <=200){
                                        txtFluxoAr.setTextColor(Color.parseColor("#048A15"));
                                    }

                                    txtFluxoAr.setText("Fluxo de Ar: " + fluxo);
                                }
                            }
                        } catch (Exception e)
                        {

                            Log.e( "ERRO", e.getMessage() );

                        }

                    }
                    break;

                case MSG_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    //Toast.makeText(getApplicationContext(), "Entrou no handler: MSG_WRITE:" + writeMessage, Toast.LENGTH_SHORT).show();
                    break;
            }

        }

    };


    private class ExecuteThread extends Thread {
        public void run() {
            while (true) {
                try {
                    String msg = "";
                    msg = "01 05\r";
                    connectedThread.write(msg.getBytes());
                    Thread.sleep(200);
                    msg = "01 0C\r";
                    connectedThread.write(msg.getBytes());
                    Thread.sleep(200);
                    msg = "01 0D\r";
                    connectedThread.write(msg.getBytes());
                    Thread.sleep(200);
                    msg = "01 11\r";
                    connectedThread.write(msg.getBytes());
                    Thread.sleep(200);
                    msg = "01 10\r";
                    connectedThread.write(msg.getBytes());

                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("obdbt", MY_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MSG_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e("ERROR",  e.getMessage());
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MSG_WRITE, -1, -1, bytes)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("ERROR",  e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("ERROR",  e.getMessage());
            }
        }
    }


}
