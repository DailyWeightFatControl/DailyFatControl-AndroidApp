package comdailyweightfatcontrol.httpsgithub.dailyfatcontrol_androidapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.FloatProperty;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import comdailyweightfatcontrol.httpsgithub.dailyfatcontrol_androidapp.adapter.IQDeviceAdapter;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQSendMessageListener;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


//public class MainActivity extends AppCompatActivity
//        implements NavigationView.OnNavigationItemSelectedListener {
public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener, OnChartGestureListener {

    private ConnectIQ mConnectIQ;
    private IQDevice mIQDevice;
    private IQDeviceAdapter mAdapter;
    private boolean mSdkReady = false;
    public static final String MY_APP = "3F3D83A85F584671A551EA1316623AD7";
    private IQApp mConnectIQApp = new IQApp(MY_APP);
    public static final int HISTORIC_HR_COMMAND = 104030201;
    public static final int USER_DATA_COMMAND = 204030201;
    public static TextView connectStatus;
    public static TextView textViewCalories1;
    public static TextView textViewCalories2;
    public static String PREFERENCES = "MainSharedPreferences";
    public static SharedPreferences Prefs;
    public static long mMidNightToday;
    private long mGraphInitialDate;
    private long mGraphFinalDate;
    public static final long SECONDS_24H = 24*60*60;
    private double caloriesEERMax = 0;
    private double caloriesActiveMax = 0;

    public static SharedPreferences getPrefs() {
        return Prefs;
    }

    private ConnectIQ.IQDeviceEventListener mDeviceEventListener = new ConnectIQ.IQDeviceEventListener() {

        @Override
        public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus status) {
            if (status == IQDeviceStatus.CONNECTED) {
                connectStatus.setText("connected");
            } else if (status == IQDeviceStatus.NOT_CONNECTED) {
                connectStatus.setText("not connected");
            } else if (status == IQDeviceStatus.NOT_PAIRED) {
                connectStatus.setText("not paired");
            } else if (status == IQDeviceStatus.UNKNOWN) {
                connectStatus.setText("unknown");
            }

            device.setStatus(status);
        }

    };

    ConnectIQ.ConnectIQListener mListenerSDKInitialize = new ConnectIQ.ConnectIQListener() {
        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus errStatus) {
//            if( null != mTextView )
//                mTextView.setText(R.string.initialization_error + errStatus.name());
            mSdkReady = false;
        }

        @Override
        public void onSdkReady() {
            mSdkReady = true;

            // verifiy if Garmin device is saved on SharedPreferences
            SharedPreferences mPrefs = getPrefs();
            Gson gson = new Gson();
            String json = mPrefs.getString("garmin_device", "");
            mIQDevice = gson.fromJson(json, IQDevice.class);

            if (mIQDevice != null) {
                // Get our instance of ConnectIQ.  Since we initialized it
                // in our MainActivity, there is no need to do so here, we
                // can just get a reference to the one and only instance.
                mConnectIQ = ConnectIQ.getInstance();
                try {
                    mConnectIQ.registerForDeviceEvents(mIQDevice, new IQDeviceEventListener() {

                        @Override
                        public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
                            if (status == IQDeviceStatus.CONNECTED) {
                                connectStatus.setText("connected");
                            } else if (status == IQDeviceStatus.NOT_CONNECTED) {
                                connectStatus.setText("not connected");
                            } else if (status == IQDeviceStatus.NOT_PAIRED) {
                                connectStatus.setText("not paired");
                            } else if (status == IQDeviceStatus.UNKNOWN) {
                                connectStatus.setText("unknown");
                            }
                        }
                    });
                } catch (InvalidStateException e) {
//                Log.wtf(TAG, "InvalidStateException:  We should not be here!");
                }

                try {
                    mConnectIQ.registerForDeviceEvents(mIQDevice, mDeviceEventListener);
                } catch (InvalidStateException e) {
                    // This generally means you forgot to call initialize(), but since
                    // we are in the callback for initialize(), this should never happen
                }

                // Let's check the status of our application on the device.
                try {
                    mConnectIQ.getApplicationInfo(MY_APP, mIQDevice, new ConnectIQ.IQApplicationInfoListener() {

                        @Override
                        public void onApplicationInfoReceived(IQApp app) {

                            //mConnectIQApp = app;

                            // This is a good thing. Now we can show our list of message options.
//                        String[] options = getResources().getStringArray(R.array.send_message_display);
//                        String[] options = null;
//
//                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(DeviceActivity.this, android.R.layout.simple_list_item_1, options);
//                        setListAdapter(adapter);

                        }

                        @Override
                        public void onApplicationNotInstalled(String applicationId) {
                            // The Comm widget is not installed on the device so we have
                            // to let the user know to install it.
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle(R.string.missing_widget);
                            dialog.setMessage(R.string.missing_widget_message);
                            dialog.setPositiveButton(android.R.string.ok, null);
                            dialog.create().show();
                        }

                    });
                } catch (InvalidStateException e1) {
                } catch (ServiceUnavailableException e1) {
                }

                // Let's register to receive messages from our application on the device.
                try {
                    mConnectIQ.registerForAppEvents(mIQDevice, mConnectIQApp, new IQApplicationEventListener() {

                        @Override
                        public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, ConnectIQ.IQMessageStatus status) {

                            // We know from our Comm sample widget that it will only ever send us strings, but in case
                            // we get something else, we are simply going to do a toString() on each object in the
                            // message list.
                            StringBuilder builder = new StringBuilder();

                            ArrayList<Integer> theMessage = new ArrayList<Integer>();
                            ArrayList<Measurement> measurementList = new ArrayList<Measurement>();

                            theMessage = (ArrayList<Integer>) message.get(0);
                            if(theMessage.get(0) == HISTORIC_HR_COMMAND) {

                                // Get the user data to store on each measurement
                                SharedPreferences mPrefs = MainActivity.getPrefs();
                                int birthYear = mPrefs.getInt("BIRTH_YEAR", 0);
                                int gender = mPrefs.getInt("GENDER", 0);
                                int height = mPrefs.getInt("HEIGHT", 0);
                                int weight = mPrefs.getInt("WEIGHT", 0);
                                int activityClass = mPrefs.getInt("ACTIVITY_CLASS", 0);

                                Iterator<Integer> iteratorTheMessage = theMessage.iterator();
                                iteratorTheMessage.next(); // command ID
                                iteratorTheMessage.next(); // random

                                while (iteratorTheMessage.hasNext()) {
                                    Measurement measurement = new Measurement();
                                    measurement.setDate(iteratorTheMessage.next());
                                    measurement.setHRValue(iteratorTheMessage.next());

                                    // now set all the values on the measurement
                                    measurement.setUserBirthYear(birthYear);
                                    measurement.setUserGender(gender);
                                    measurement.setUserHeight(height);
                                    measurement.setUserWeight(weight);
                                    measurement.setUserActivityClass(activityClass);

                                    measurementList.add(measurement);
                                }

                                // reverse the list order, to get the values in date ascending order
                                Collections.reverse(measurementList);

                                // finally write the measurement list to database
                                new DataBase(getApplication().getApplicationContext()).DataBaseWriteMeasurement(measurementList);

                                refreshGraphs();

                            } else if (theMessage.get(0) == USER_DATA_COMMAND) {

                                // Store the UserData on Preferences
                                SharedPreferences.Editor editor = getPrefs().edit();
                                Iterator<Integer> iteratorTheMessage = theMessage.iterator();
                                iteratorTheMessage.next(); // command ID
                                iteratorTheMessage.next(); // random
                                editor.putInt("BIRTH_YEAR", iteratorTheMessage.next());
                                editor.putInt("GENDER", iteratorTheMessage.next());
                                editor.putInt("HEIGHT", iteratorTheMessage.next());
                                editor.putInt("WEIGHT", iteratorTheMessage.next());
                                editor.putInt("ACTIVITY_CLASS", iteratorTheMessage.next());
                                editor.commit();
                            }

                            if (message.size() > 0) {
                                for (Object o : message) {
                                    builder.append(o.toString());
                                    builder.append("\r\n");
                                }
                            } else {
                                builder.append("Received an empty message from the application");
                            }

                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle(R.string.received_message);
                            dialog.setMessage(builder.toString());
                            dialog.setPositiveButton(android.R.string.ok, null);
                            dialog.create().show();
                        }
                    });

//                        // Start by sending the USER_DATA_COMMAND, which is needed to get the user information at app start
//                        ArrayList<Integer> command = new ArrayList<Integer>();
//                        command.add(USER_DATA_COMMAND);
//                        Random r = new Random();
//                        command.add(r.nextInt(2^30));
//                        sendMessage(command);

                } catch (InvalidStateException e) {
                    Toast.makeText(getApplication().getApplicationContext(), "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show();
                }
            } else {
                // Since there is no IQDevice saved on SharedPreferences, start the activity for user
                // select the IQDevice
                Intent intent = new Intent(getApplication().getApplicationContext(), ConnectActivity.class);
                startActivity(intent);
            }
        }

        @Override
        public void onSdkShutDown() {
            mSdkReady = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Button buttonNext = (Button) findViewById(R.id.next_button);
        Button buttonPrevious = (Button) findViewById(R.id.previous_button);
        Button buttonLogFood = (Button) findViewById(R.id.button_log_food);
        final TextView dateTitle = (TextView) findViewById(R.id.date_title);
        textViewCalories1 = (TextView) findViewById(R.id.textViewCalories1);
        textViewCalories2 = (TextView) findViewById(R.id.textViewCalories2);
        connectStatus = (TextView) findViewById(R.id.status_connection);

        // Calc and set graph initial and final dates (midnight today and rightnow)
        Calendar rightNow = Calendar.getInstance();
        long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
        long rightNowMillis = rightNow.getTimeInMillis() + offset;
        long sinceMidnightToday = rightNowMillis % (24 * 60 * 60 * 1000);
        long midNightToday = rightNowMillis - sinceMidnightToday;
        long now = rightNowMillis / 1000; // now in seconds
        midNightToday /= 1000; // now in seconds
        mMidNightToday = midNightToday;
        mGraphInitialDate = midNightToday;
        mGraphFinalDate = now;

        buttonLogFood.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplication().getApplicationContext(), LogFoodMainActivity.class);
                startActivity(intent);
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // do something when the button is clicked
                // Yes we will handle click here but which button clicked??? We don't know

                mGraphInitialDate += SECONDS_24H; // seconds

                if (mGraphInitialDate == mMidNightToday) { // today
                    buttonNext.setVisibility(View.INVISIBLE);
                    dateTitle.setText("today");

                    Calendar rightNow = Calendar.getInstance();
                    long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
                    long rightNowMillis = rightNow.getTimeInMillis() + offset;
                    mGraphFinalDate = rightNowMillis / 1000; // seconds

                } else if (mGraphInitialDate == (mMidNightToday - SECONDS_24H)) { // yesterday
                    buttonNext.setVisibility(View.VISIBLE);
                    dateTitle.setText("yesterday");
                    mGraphFinalDate = mGraphInitialDate + SECONDS_24H; // seconds

                } else { // other days
                    buttonNext.setVisibility(View.VISIBLE);
                    SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
                    String dateString = formatter.format(new Date(mGraphInitialDate * 1000L));
                    dateTitle.setText(dateString);
                    mGraphFinalDate = mGraphInitialDate + SECONDS_24H; // seconds
                }

                refreshGraphs();
            }
        });

        buttonPrevious.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // do something when the button is clicked
                // Yes we will handle click here but which button clicked??? We don't know

                mGraphInitialDate -= SECONDS_24H; // seconds

                if (mGraphInitialDate == (mMidNightToday - SECONDS_24H)) { // yesterday
                    buttonNext.setVisibility(View.VISIBLE);
                    dateTitle.setText("yesterday");
                    mGraphFinalDate = mGraphInitialDate + SECONDS_24H; // seconds

                } else { // other days
                    buttonNext.setVisibility(View.VISIBLE);
                    SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
                    String dateString = formatter.format(new Date(mGraphInitialDate * 1000L));
                    dateTitle.setText(dateString);
                    mGraphFinalDate = mGraphInitialDate + SECONDS_24H; // seconds
                }

                refreshGraphs();
            }
        });

//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.setDrawerListener(toggle);
//        toggle.syncState();
//
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);

        Prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE);

        //mTextView = (TextView)findViewById(R.id.text_view);garmin_device

        // Here we are specifying that we want to use a WIRELESS bluetooth connection.
        // We could have just called getInstance() which would by default create a version
        // for WIRELESS, unless we had previously gotten an instance passing TETHERED
        // as the connection type.
        mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);

        // Initialize the SDK
        mConnectIQ.initialize(this, true, mListenerSDKInitialize);

        refreshGraphs();
    }

    @Override
    public void onResume() {
        super.onResume();


    }

//    @Override
//    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        ArrayList<Integer> command = new ArrayList<Integer>();

        if (id == R.id.connect) {
            // Handle the connect action
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.update) {
            command.add(HISTORIC_HR_COMMAND);
            Random r = new Random();
            command.add(r.nextInt(2^30));
            long date = new DataBase(getApplication().getApplicationContext()).DataBaseGetLastMeasurementDate();
            command.add((int) date);
            sendMessage(command);
            return true;

        } else if (id == R.id.create_food) {
            // Handle the connect action
            Intent intent = new Intent(this, CreateFoodActivity.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.user_profile) {
            command.add(USER_DATA_COMMAND);
            Random r = new Random();
            command.add(r.nextInt(2^30));
            sendMessage(command);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//    }

    void sendMessage(ArrayList<Integer> command) {
        try {
            mConnectIQ.sendMessage(mIQDevice, mConnectIQApp, command, new IQSendMessageListener() {

                @Override
                public void onMessageStatus(IQDevice device, IQApp app, IQMessageStatus status) {
                }

            });
        } catch (InvalidStateException e) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show();
        } catch (ServiceUnavailableException e) {
            Toast.makeText(this, "ConnectIQ service is unavailable. Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show();
        }
    }

    void refreshGraphs() {
        GraphData graphDataObj = new GraphData(getApplication().getApplicationContext());
        List<Entry> graphDataCaloriesEER = graphDataObj.prepareCaloriesEER(mGraphInitialDate, mGraphFinalDate);
        caloriesEERMax = graphDataObj.getmMaxCaloriesEER();
        List<Entry> graphDataCaloriesActive = graphDataObj.prepareCaloriesActive(mGraphInitialDate, mGraphFinalDate, caloriesEERMax);
        List<Entry> graphDataCaloriesConsumed = graphDataObj.prepareCaloriesConsumed(mGraphInitialDate, mGraphFinalDate);

        if (graphDataCaloriesActive != null && graphDataCaloriesEER != null && graphDataCaloriesConsumed != null) {
            caloriesActiveMax = graphDataObj.getmMaxCaloriesActive();
            textViewCalories1.setText("total calories: " + Integer.toString((int) (caloriesEERMax + caloriesActiveMax)));
            textViewCalories2.setText("active calories: " + Integer.toString((int) caloriesActiveMax));

            // add entries to Calories Active dataset
            LineDataSet dataSetCaloriesActive = new LineDataSet(graphDataCaloriesActive, "Active calories");
            dataSetCaloriesActive.setColor(Color.rgb(0, 172 , 117));
            dataSetCaloriesActive.setCubicIntensity(1f);
            dataSetCaloriesActive.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            dataSetCaloriesActive.setFillColor(Color.rgb(0, 229, 154));
            dataSetCaloriesActive.setFillAlpha(66);
            dataSetCaloriesActive.setDrawFilled(true);
            dataSetCaloriesActive.setDrawHighlightIndicators(true);
            dataSetCaloriesActive.setHighlightLineWidth(2f);
            dataSetCaloriesActive.setDrawValues(false);
            dataSetCaloriesActive.setLineWidth(2f);
            dataSetCaloriesActive.setDrawCircles(false);

            // add entries to Calories EER dataset
            LineDataSet dataSetCaloriesEER = new LineDataSet(graphDataCaloriesEER, "No active calories");
            dataSetCaloriesEER.setColor(Color.rgb(0, 172 , 117));
            dataSetCaloriesEER.setMode(LineDataSet.Mode.LINEAR);
            dataSetCaloriesEER.setFillColor(Color.rgb(0, 172, 117));
            dataSetCaloriesEER.setFillAlpha(66);
            dataSetCaloriesEER.setDrawFilled(true);
            dataSetCaloriesEER.setHighlightEnabled(false);
            dataSetCaloriesEER.setDrawValues(false);
            dataSetCaloriesEER.setLineWidth(0);
            dataSetCaloriesEER.setDrawCircles(false);

            // add entries to Calories consumed dataset
            LineDataSet dataSetCaloriesConsumed = new LineDataSet(graphDataCaloriesConsumed, "Calories consumed");
            dataSetCaloriesConsumed.setColor(Color.rgb(200, 200 , 0));
            dataSetCaloriesConsumed.setMode(LineDataSet.Mode.LINEAR);
            dataSetCaloriesConsumed.setFillColor(Color.rgb(255, 255, 0));
            dataSetCaloriesConsumed.setFillAlpha(66);
            dataSetCaloriesConsumed.setDrawFilled(true);
            dataSetCaloriesConsumed.setHighlightEnabled(false);
            dataSetCaloriesConsumed.setDrawValues(true);
            dataSetCaloriesConsumed.setLineWidth(2f);
            dataSetCaloriesConsumed.setDrawCircles(true);

            LineData lineData = new LineData(dataSetCaloriesEER, dataSetCaloriesActive, dataSetCaloriesConsumed);
//            LineData lineData = new LineData(dataSetCaloriesEER, dataSetCaloriesActive);
//            LineData lineData = new LineData(dataSetCaloriesConsumed);

            // in this example, a LineChart is initialized from xml
            LineChart mChart = (LineChart) findViewById(R.id.chart_calories_active);

            // enable touch gestures
            mChart.setTouchEnabled(true);
//            mChart.setOnChartGestureListener(this);
            mChart.setOnChartValueSelectedListener(this);

            mChart.setBackgroundColor(Color.WHITE);
            mChart.setDrawGridBackground(false);
            mChart.setDrawBorders(true);

            mChart.setDoubleTapToZoomEnabled(false);

            // enable scaling and dragging
//            chart.setDragEnabled(true);
//            chart.setScaleEnabled(true);
            mChart.setScaleXEnabled(true);
            mChart.setScaleYEnabled(false);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.GRAY);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(23.983f); //23h59m
            xAxis.setGranularity(0.016666667f); //1m
            xAxis.setValueFormatter(new IAxisValueFormatter() {

                private SimpleDateFormat mFormat = new SimpleDateFormat("H'h'mm");

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if ((value % 1) == 0) {
                        mFormat = new SimpleDateFormat("H'h'");
                    } else {
                        mFormat = new SimpleDateFormat("H'h'mm");
                    }
                    return mFormat.format(new Date((long) ((value-1) *60*60*1000)));
                }
            });

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setEnabled(true);
            leftAxis.setGranularity(1);
            leftAxis.setAxisMinimum(0);
            leftAxis.setDrawTopYLabelEntry(true);

            // adjust max y axis value
            if ((caloriesEERMax + caloriesActiveMax) > 3250) {
                leftAxis.resetAxisMaximum();
            } else {
                leftAxis.setAxisMaximum(800);
            }

            leftAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value < (caloriesEERMax/10)) {
                        return "";
                    } else {
                        value = value - ((float) caloriesEERMax / 10);
                    }

                    return Integer.toString((int) value);
                }
            });


            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(true);
            rightAxis.setGranularity(1);
            rightAxis.setAxisMinimum(0);
            rightAxis.setDrawTopYLabelEntry(true);
            rightAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value < (caloriesEERMax/10)) {
                        value *= 10;
                    } else {
                        value = (value - (float) (caloriesEERMax/10)) + (float) caloriesEERMax;
                    }

                    return Integer.toString((int) value);
                }
            });

            // adjust max y axis value
            if ((caloriesEERMax + caloriesActiveMax) > 3250) {
                rightAxis.resetAxisMaximum();
            } else {
                rightAxis.setAxisMaximum(800);
            }


            // no description text
            mChart.getDescription().setEnabled(false);

            mChart.setAutoScaleMinMaxEnabled(true);
            mChart.setData(lineData);
//            chart.animateY(200);
            mChart.invalidate(); // refresh

            //-------------------------

            //        List<Entry> graphData1 = graphDataObj.prepareHRHigher90(mGraphInitialDate, mGraphFinalDate);
            //
            //        // add entries to dataset
            //        dataSet = new LineDataSet(graphData1, "HR >= 90");
            //        dataSet.setColor(Color.rgb(0, 0, 0));
            //
            //        dataSet.setCircleRadius(1);
            //        dataSet.setFillColor(Color.argb(150, 51, 181, 229));
            //        dataSet.setFillAlpha(255);
            //        dataSet.setDrawFilled(true);
            //
            //        lineData = new LineData(dataSet);
            //
            //        // in this example, a LineChart is initialized from xml
            //        chart = (LineChart) findViewById(R.id.chart_hr_higher_90);
            //
            //        chart.setBackgroundColor(Color.WHITE);
            //        chart.setDrawGridBackground(true);
            //        chart.setDrawBorders(true);
            //
            //        xAxis = chart.getXAxis();
            //        xAxis.setPosition(XAxisPosition.BOTTOM);
            //        xAxis.setTextColor(Color.GRAY);
            //        xAxis.setDrawAxisLine(false);
            //        xAxis.setDrawGridLines(true);
            //        xAxis.setGridLineWidth(1);
            //        xAxis.setGridLineWidth(1);
            //        xAxis.setAxisMaximum(24f);
            //
            //        leftAxis = chart.getAxisLeft();
            //        leftAxis.setAxisMinimum(0f);
            //        rightAxis = chart.getAxisRight();
            //        rightAxis.setAxisMinimum(0f);
            //
            //        // no description text
            //        chart.getDescription().setEnabled(false);
            //
            //        chart.setAutoScaleMinMaxEnabled(false);
            //        chart.setData(lineData);
            //        chart.invalidate(); // refresh

            //        ------------------------

            //        List<Entry> graphData2 = graphDataObj.prepareHR(mGraphInitialDate, mGraphFinalDate);
            //
            //        // add entries to dataset
            //        dataSet = new LineDataSet(graphData2, "HR");
            //        dataSet.setColor(Color.rgb(0, 0, 0));
            //
            //        dataSet.setCircleRadius(1);
            //        dataSet.setFillColor(Color.argb(150, 51, 181, 229));
            //        dataSet.setFillAlpha(255);
            //        dataSet.setDrawFilled(true);
            ////        dataSet.setDrawCircles(false);
            //
            //        lineData = new LineData(dataSet);
            //
            //        // in this example, a LineChart is initialized from xml
            //        chart = (LineChart) findViewById(R.id.chart_hr);
            //
            //        chart.setBackgroundColor(Color.WHITE);
            //        chart.setDrawGridBackground(true);
            //        chart.setDrawBorders(true);
            //
            //        xAxis = chart.getXAxis();
            //        xAxis.setPosition(XAxisPosition.BOTTOM);
            //        xAxis.setTextColor(Color.GRAY);
            //        xAxis.setDrawAxisLine(false);
            //        xAxis.setDrawGridLines(true);
            //        xAxis.setGridLineWidth(1);
            //        xAxis.setGridLineWidth(1);
            //        xAxis.setAxisMaximum(24f);
            //
            //        leftAxis = chart.getAxisLeft();
            //        leftAxis.setAxisMinimum(0f);
            //        rightAxis = chart.getAxisRight();
            //        rightAxis.setAxisMinimum(0f);
            //
            //        // no description text
            //        chart.getDescription().setEnabled(false);
            //
            //        chart.setAutoScaleMinMaxEnabled(false);
            //        chart.setData(lineData);
            //        chart.invalidate(); // refresh

        }
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        // write the selected value of Y value (calories on the point selected)

        float date = e.getX();
        SimpleDateFormat mFormat;
        mFormat = new SimpleDateFormat("H'h'mm");
        String dateString = mFormat.format(new Date((long) ((date-1) *60*60*1000)));

        float value = 0;
        if (e.getY() < (caloriesEERMax/10)) {
            value = e.getY()*10;
        } else {
//            value = (float) (e.getY()*10 - caloriesEERMax);
            value = (float) (caloriesEERMax + (e.getY() - (caloriesEERMax / 10)));
//            value = value + (float) caloriesEERMax;
        }

        textViewCalories1.setText(dateString + " total calories " + Integer.toString((int) (value)));
        textViewCalories2.setText("active calories " + Integer.toString(((int) value) - (int) (caloriesEERMax)));
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        textViewCalories1.setText("total calories: " + Integer.toString((int) (caloriesEERMax + caloriesActiveMax)));
        textViewCalories2.setText("active calories: " + Integer.toString((int) caloriesActiveMax));
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onNothingSelected() {
        textViewCalories1.setText("total calories: " + Integer.toString((int) (caloriesEERMax + caloriesActiveMax)));
        textViewCalories2.setText("active calories: " + Integer.toString((int) caloriesActiveMax));
    }
}