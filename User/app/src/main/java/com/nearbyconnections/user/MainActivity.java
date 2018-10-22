package com.nearbyconnections.user;

import android.Manifest;
import android.app.Dialog;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private String endPoint = "";

    private PayloadCallback mPayloadCallback;
    private EndpointDiscoveryCallback endpointDiscoveryCallback;
    private ConnectionLifecycleCallback mConnectionLifecycleCallback;
    private Dialog pickMeUpDialog;
    private TextView cabNameTextView;
    private ProgressBar progressBar;

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
        initDialog();
        initListeners();
    }

    private void initDialog() {
        pickMeUpDialog = new Dialog(this);
        pickMeUpDialog.setContentView(R.layout.pickme_dialog_layout);
        cabNameTextView = pickMeUpDialog.findViewById(R.id.cab_no_textview);
        progressBar = pickMeUpDialog.findViewById(R.id.progressBar);
        cabNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int randomNumber = (int) ((Math.random() * ((10 - 1) + 1)) + 1);
                Nearby.getConnectionsClient(MainActivity.this).sendPayload(endPoint,
                        Payload.fromBytes(("randomNumber:"+randomNumber).getBytes(UTF_8)));
                Log.d(TAG, "Driver tapped");
                Log.e("Testing", "Payload Sent"+("randomNumber:"+randomNumber));
                pickMeUpDialog.dismiss();
            }
        });
        pickMeUpDialog.setCancelable(false);
    }

    private void  initListeners(){

        mConnectionLifecycleCallback =
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                        endPoint = endpointId;
                        Nearby.getConnectionsClient(MainActivity.this).acceptConnection(endpointId, mPayloadCallback);
                        Log.e("Testing", "Connection initiated");
                    }

                    @Override
                    public void onConnectionResult(String endpointId, ConnectionResolution result) {
                        switch (result.getStatus().getStatusCode()) {
                            case ConnectionsStatusCodes.STATUS_OK:
                                Log.e("Testing", "Connection success");
                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onDisconnected(String endpointId) {
                        System.out.print("");
                    }
                };

        endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                Log.d(TAG, "onEndpointFound: endpoint found, connecting");
                Nearby.getConnectionsClient(MainActivity.this).requestConnection(
                        "UserName",
                        endpointId,
                        mConnectionLifecycleCallback)
                        .addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unusedResult) {
                                        Log.e("Testing", "PEnd point discovered");
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        System.out.print("");
                                    }
                                });
            }

            @Override
            public void onEndpointLost(String endpointId) {
                System.out.print("");
            }
        };

        mPayloadCallback =
                new PayloadCallback() {
                    @Override
                    public void onPayloadReceived(String endpointId, Payload payload) {
                        String payloadString = new String(payload.asBytes(), UTF_8);
                        Log.e("Testing", "Payload Recieved"+payloadString);
                        if(payloadString.contains("cabno")) {
                            String cabNo = payloadString.split(":")[1];
                            cabNameTextView.setText(cabNo);
                            progressBar.setVisibility(View.GONE);
                            cabNameTextView.setVisibility(View.VISIBLE);
                        } else if(payloadString.contains("status")){
                            String status = payloadString.split(":")[1];
                            Toast.makeText(MainActivity.this, "Pickup Status" + status, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                        Log.e("Testing", "Payload Update"+update.getTotalBytes());
                    }
                };


    }

    public void pickMeUp(View view) {
        pickMeUpDialog.show();
        Nearby.getConnectionsClient(this)
                .startDiscovery(
                        "xxx",
                        endpointDiscoveryCallback,
                        new DiscoveryOptions(Strategy.P2P_CLUSTER));
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
