package com.nearbyconnections.driver;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {

    private final String END_POINT_ID = "Driver A";
    private ProgressDialog pickupDialog;
    private EditText cabNumberEditText;
    private static final String TAG = MainActivity.class.getSimpleName();


    private ConnectionLifecycleCallback mConnectionLifecycleCallback;
    private PayloadCallback mPayloadCallback;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initListeners();
    }

    private void initViews() {
        cabNumberEditText = findViewById(R.id.cab_no_edit_text);
    }

    private void initListeners() {
        mConnectionLifecycleCallback =
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                        Log.e("Testing", "Connection Initiated");
                        Nearby.getConnectionsClient(MainActivity.this).acceptConnection(endpointId, mPayloadCallback);
                    }

                    @Override
                    public void onConnectionResult(String endpointId, ConnectionResolution result) {
                        switch (result.getStatus().getStatusCode()) {
                            case ConnectionsStatusCodes.STATUS_OK:
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(endpointId,
                                        Payload.fromBytes(("cabno:"+cabNumberEditText.getText().toString()).getBytes(UTF_8)));
                                Log.e("Testing", "Connection Success");
                                Log.e("Testing", "Payload Send"+("cabno:"+cabNumberEditText.getText().toString()));
                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onDisconnected(String endpointId) {

                    }
                };

        mPayloadCallback =
                new PayloadCallback() {
                    @Override
                    public void onPayloadReceived(String endpointId, Payload payload) {
                        String payloadString = new String(payload.asBytes(), UTF_8);
                        Log.e("Testing", "Payload Recieved"+payloadString);
                        pickupDialog.dismiss();
                        if(payloadString.contains("randomNumber")) {
                            int randomNumber = Integer.parseInt(payloadString.split(":")[1]);
                            if (randomNumber % 2 == 0){
                                Toast.makeText(MainActivity.this, "Pickup Successful", Toast.LENGTH_LONG).show();
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(endpointId,
                                        Payload.fromBytes("status:Success".getBytes(UTF_8)));
                            } else {
                                Nearby.getConnectionsClient(MainActivity.this).sendPayload(endpointId,
                                        Payload.fromBytes("status:Failed".getBytes(UTF_8)));
                                Toast.makeText(MainActivity.this, "Pickup Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                        Log.e("Testing", "Payload Update"+update.getTotalBytes());
                    }
                };

    }

    public void pickUpUser(View view) {
        pickupDialog = new ProgressDialog(this);
        pickupDialog.setCancelable(false);
        pickupDialog.setMessage("Picking Customer");
        pickupDialog.show();
        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        END_POINT_ID,
                        "xxx",
                        mConnectionLifecycleCallback,
                        new AdvertisingOptions(Strategy.P2P_CLUSTER));
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

}
