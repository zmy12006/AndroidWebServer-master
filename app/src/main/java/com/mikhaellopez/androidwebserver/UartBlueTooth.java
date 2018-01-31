package com.mikhaellopez.androidwebserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by zmy12_000 on 1/13/2016.
 * 封装一下，如果能实现连接多个蓝牙串口更好
 * 如果不能连接过个蓝牙，那么收发数据接口都希望
 * 能够在UI线程里面操作，那就要求收发都是消息处理，且独立线程
 * 包括初始化这个类，也要可以在任何地方都可以
 *
 * 消息接收可以采用接口类实现
 */
public class UartBlueTooth {
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothSocket socket = null;

    private String macAddress = "";

    private int connetTime = 0;

    private boolean connecting = true;
    private boolean connected = false;

    public void Connect(String macAddr)
    {
        Log.i("info", "OnButtonclick....");

        ConnectThread blueThread = new ConnectThread(macAddr);//new ConnectThread("00:06:71:00:40:16"); //hc 4
        //ConnectThread blueThread = new ConnectThread("98:D3:31:40:48:83"); //hc 5 切记这里地址是大写D，但是从at指令得到是小写d，所以一切还是有android系统提供的地址为标准 98:D3:31:40:48:83 //ConnectThread blueThread = new ConnectThread("98:d3:31:40:48:83");

        Log.i("info", "connectthread start.........");
        blueThread.start();
    }

    private class ConnectThread extends Thread {

        //private Handler handler;

        public ConnectThread(String mac) {
            macAddress = mac;
        }

        public void run() {
            connecting = true;
            connected = false;
            if(mBluetoothAdapter == null){
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }

            //--------------------------------------------------------------
            if(!mBluetoothAdapter.isEnabled())//判断蓝牙设备是否已开起
            {
                //开起蓝牙设备
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivity(intent);
            }

            Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
            for(Iterator<BluetoothDevice> iterator = devices.iterator();iterator.hasNext();)
            {
                BluetoothDevice device = iterator.next();

                Log.i("info", device.getName() + "--" + device.getAddress() + "..................");

            }
            //---------------------------------------------------------------


            Log.i("info", mBluetoothAdapter + "start get device......................");
            try {
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddress);
                mBluetoothAdapter.cancelDiscovery();
            }
            catch ( Exception e )
            {
                Log.i("info", e.toString() + ".............................................");
            }
            Log.i("info", "device is:" + mBluetoothDevice + "...............");
            mBluetoothAdapter.cancelDiscovery();
            initSocket();
            connetTime = 1;
            //adapter.cancelDiscovery();
            while (!connected && connetTime <= 10) {
                try {
                    socket.connect();
                    connected = true;
                } catch (IOException e1) {
                    connetTime++;
                    connected = false;
                    Log.e("exception", "Socket", e1);
                    // 关闭 socket
                    try {
                        socket.close();
                        socket = null;
                    } catch (IOException e2) {
                        //TODO: handle exception
                        Log.e("exception", "Socket", e2);
                    }
                } finally {
                    connecting = false;
                }
                //connectDevice();
            }



            //new Thread(new MyTimer()).start();

            BluetoothSocketListener bluetoothListener = new BluetoothSocketListener(socket);
            bluetoothListener.run();


        }


        private class BluetoothSocketListener implements Runnable {

            private BluetoothSocket socket;
            //private TextView textView;
            //private Handler handler;
            String msg = "";

            public BluetoothSocketListener(BluetoothSocket socket) {
                this.socket = socket;
            }

            public void ProcessMsg( String msg )
            {

                String[] vecMsg = msg.split(":");
                Log.i("msg", "messge len:" + vecMsg.length + "...................val:"+vecMsg[0] + "," +vecMsg[1]);
                String cmdStr = vecMsg[0];
                if ( vecMsg.length == 2 ) {
                    Log.i("msg", "star speak....................................." + (cmdStr == "temp"));
                    for (int i = 0; i < cmdStr.length() && i < "temp".length(); i++) {
                        Log.i("msg", "compare letter:" + cmdStr.getBytes()[i] + "," + "temp".getBytes()[i] + ".......................");
                    }
                /*try {
                    cmdStr = new String(vecMsg[0].getBytes(), "utf8");
                }catch (Exception e) {

                }*/
                }

            }

            public void ProcessByte( byte[] buff, int len )
            {
                if ( buff == null )
                    return;

                int idx = 0;
                while ( idx < 1024 && idx < len ) {
                    if ( buff[idx] == 0 )
                        break;

                    //Log.i("msg", "msg:" + msg + "......................");
                    if( buff[idx] == 10 || buff[idx] == 13 )
                    {

                        if ( msg != "" ) {
                            //speechText = (EditText) findViewById(R.id.editText);
                            //speechText.setText( msg );
                            //mActivety.OnBluetoothMsg(msg);
                            //handler.obtainMessage(0,msg);
                            //mTTS.Speak( msg );
                            ProcessMsg(msg);
                        }

                        msg = "";
                    }
                    else
                    {
                        msg += (char)buff[idx];
                    }

                    idx ++;
                }
            }

            public void run() {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                try {
                    InputStream instream = socket.getInputStream();
                    int bytesRead = -1;
                    String message = "";
                    while (true) {
                        message = "";
                        bytesRead = instream.read(buffer);

                        if (bytesRead != -1) {
                            while ((bytesRead==bufferSize)&&(buffer[bufferSize-1] != 0)) {
                                ProcessByte(buffer, bytesRead);
                                message = message + new String(buffer, 0, bytesRead);
                                bytesRead = instream.read(buffer);

                            }
                            ProcessByte(buffer, bytesRead);
                            message = message + new String(buffer, 0, bytesRead);
                            Log.i("msg", message);
                            //handler.post(new MessagePoster(textView, message));
                            //JniTestHelper.sendToUI(message);
                            socket.getInputStream();
                        }
                    }
                } catch (IOException e) {
                    Log.d("BLUETOOTH_COMMS", e.getMessage());
                }
            }
        }

        /**
         * 取得BluetoothSocket
         */
        private void initSocket() {
            BluetoothSocket temp = null;
            try {
                Method m = mBluetoothDevice.getClass().getMethod(
                        "createRfcommSocket", new Class[] { int.class });
                temp = (BluetoothSocket) m.invoke(mBluetoothDevice, 1);//这里端口为1
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            socket = temp;
        }
    }

    private void sendMessage(BluetoothSocket socket, String msg) {
        //OutputStream outStream;
        try {
            //outStream = socket.getOutputStream();
            PrintWriter outStream = new PrintWriter(socket.getOutputStream(),true);
            byte[] byteString = (msg + " ").getBytes();
            byteString[byteString.length - 1] = 0;
            //outStream.write(byteString);
            outStream.write(msg);
            outStream.flush();
            Log.i("info", "sendmessage:" + msg + "....................");
        } catch (IOException e) {
            Log.i("BLUETOOTH_COMMS", e.getMessage());
        }
    }

    private void sendMessageEx(BluetoothSocket socket, String msg) {
        //OutputStream outStream;
        try {
            //outStream = socket.getOutputStream();
            PrintWriter outStream = new PrintWriter(socket.getOutputStream(),true);
            byte[] byteString = (msg + " ").getBytes();
            byteString[byteString.length - 1] = 0;
            //outStream.write(byteString);
            outStream.write(msg);
            outStream.flush();
            Log.i("info", "sendmessage:" + msg + "....................");
        } catch (IOException e) {
            Log.i("BLUETOOTH_COMMS", e.getMessage());
        }
    }
}
