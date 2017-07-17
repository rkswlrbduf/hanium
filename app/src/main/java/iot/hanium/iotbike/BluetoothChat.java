package iot.hanium.iotbike;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class BluetoothChat extends Activity {
//TESTING
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView mTitle;
    private Button mScanButton;
    private Button mCaliButton;

    private final int mMaxValue = 100;
    private LineChart acLineChart;
    private LineChart gyLineChart;
    private LineDataSet axLineDataSet;
    private LineDataSet ayLineDataSet;
    private LineDataSet gxLineDataSet;
    private LineDataSet gyLineDataSet;
    private LineDataSet gzLineDataSet;
    private LineData acLineData;
    private LineData gyLineData;

    private String axMessage = null;
    private String ayMessage = null;
    private String gxMessage = null;
    private String gyMessage = null;
    private String gzMessage = null;

    private float axCal = 0;
    private float ayCal = 0;

    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        acLineChart = (LineChart) findViewById(R.id.acChart);
        gyLineChart = (LineChart) findViewById(R.id.gyChart);

        acLineData = new LineData();
        gyLineData = new LineData();
        axLineDataSet = new LineDataSet(null, "AX");
        ayLineDataSet = new LineDataSet(null, "AY");
        gxLineDataSet = new LineDataSet(null, "GX");
        gyLineDataSet = new LineDataSet(null, "GY");
        gzLineDataSet = new LineDataSet(null, "GZ");

        acLineData.addDataSet(axLineDataSet);
        acLineData.addDataSet(ayLineDataSet);
        gyLineData.addDataSet(gxLineDataSet);
        gyLineData.addDataSet(gyLineDataSet);
        gyLineData.addDataSet(gzLineDataSet);

        acLineChart.setData(acLineData);
        gyLineChart.setData(gyLineData);

        setChartColor(axLineDataSet, Color.BLACK);
        setChartColor(ayLineDataSet, Color.WHITE);

        setLineChart(acLineChart);
        setLineChart(gyLineChart);

    }

    private void setupChat() {
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });

        mCaliButton = (Button) findViewById(R.id.button_cali);
        mCaliButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                axCal = Float.parseFloat(axMessage);
                ayCal = Float.parseFloat(ayMessage);
            }
        });

        mChatService = new BluetoothChatService(this, mHandler);
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    switch (msg.arg2) {
                        case 0:
                            try {
                                byte[] readAxBuf = (byte[]) msg.obj;
                                axMessage = new String(readAxBuf, 0, msg.arg1);
                                if (axLineDataSet.getEntryCount() == 2)
                                    axCal = Float.parseFloat(axMessage);
                                if (axLineDataSet.getEntryCount() == mMaxValue) {
                                    acLineData.removeXValue(0);
                                    axLineDataSet.removeEntry(0);

                                    for (Entry entry : axLineDataSet.getYVals()) {
                                        entry.setXIndex(entry.getXIndex() - 1);
                                    }
                                }
                                acLineData.addXValue("");
                                acLineData.addEntry(new Entry(Float.parseFloat(axMessage) - axCal, axLineDataSet.getEntryCount()), 0);
                            } catch (Exception e) {
                            }
                            break;
                        case 1:
                            try {
                                byte[] readAyBuf = (byte[]) msg.obj;
                                ayMessage = new String(readAyBuf, 0, msg.arg1);
                                if (ayLineDataSet.getEntryCount() == 2)
                                    ayCal = Float.parseFloat(ayMessage);
                                if (ayLineDataSet.getEntryCount() == mMaxValue) {
                                    ayLineDataSet.removeEntry(0);

                                    for (Entry entry : ayLineDataSet.getYVals()) {
                                        entry.setXIndex(entry.getXIndex() - 1);
                                    }
                                }
                                acLineData.addEntry(new Entry(Float.parseFloat(ayMessage) - ayCal, ayLineDataSet.getEntryCount()), 1);
                            } catch (Exception e) {
                            }

                            acLineChart.notifyDataSetChanged();
                            acLineChart.invalidate();
                            break;
                        case 2:
                            try {
                                byte[] readgxBuf = (byte[]) msg.obj;
                                gxMessage = new String(readgxBuf, 0, msg.arg1);
                                if (gxLineDataSet.getEntryCount() == mMaxValue) {
                                    gyLineData.removeXValue(0);
                                    gxLineDataSet.removeEntry(0);

                                    for (Entry entry : gxLineDataSet.getYVals()) {
                                        entry.setXIndex(entry.getXIndex() - 1);
                                    }
                                }
                                gyLineData.addXValue("");
                                gyLineData.addEntry(new Entry(Float.parseFloat(gxMessage), gxLineDataSet.getEntryCount()), 0);
                            } catch (Exception e) {
                            }
                            break;
                        case 3:
                            try {
                                byte[] readgyBuf = (byte[]) msg.obj;
                                gyMessage = new String(readgyBuf, 0, msg.arg1);
                                if (gyLineDataSet.getEntryCount() == mMaxValue) {
                                    gyLineDataSet.removeEntry(0);

                                    for (Entry entry : gyLineDataSet.getYVals()) {
                                        entry.setXIndex(entry.getXIndex() - 1);
                                    }
                                }
                                gyLineData.addEntry(new Entry(Float.parseFloat(gyMessage), gyLineDataSet.getEntryCount()), 1);
                            } catch (Exception e) {
                            }
                            break;
                        case 4:
                            try {
                                byte[] readgzBuf = (byte[]) msg.obj;
                                gzMessage = new String(readgzBuf, 0, msg.arg1);
                                if (gzLineDataSet.getEntryCount() == mMaxValue) {
                                    gzLineDataSet.removeEntry(0);

                                    for (Entry entry : gzLineDataSet.getYVals()) {
                                        entry.setXIndex(entry.getXIndex() - 1);
                                    }
                                }
                                gyLineData.addEntry(new Entry(Float.parseFloat(gzMessage), gzLineDataSet.getEntryCount()), 2);
                            } catch (Exception e) {
                            }

                            setChartColor(gxLineDataSet, Color.RED);
                            setChartColor(gyLineDataSet, Color.GREEN);
                            setChartColor(gzLineDataSet, Color.BLUE);

                            Log.d("TAG", gyLineDataSet.getEntryCount() + "");

                            gyLineChart.notifyDataSetChanged();
                            gyLineChart.invalidate();

                            break;
                        default:
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void setLineChart(LineChart lineChart) {

        lineChart.setTouchEnabled(true);

        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.GRAY);

        lineChart.setPinchZoom(true);

        Legend L = lineChart.getLegend();
        L.setForm(Legend.LegendForm.LINE);

        XAxis xl = lineChart.getXAxis();
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaxValue(20000f);
        leftAxis.setAxisMinValue(-20000f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    private void setChartColor(LineDataSet lineDataSet, int Color) {
        lineDataSet.setColor(Color);
        lineDataSet.setCircleColor(Color);
        lineDataSet.setDrawCubic(true);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) mChatService.stop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}