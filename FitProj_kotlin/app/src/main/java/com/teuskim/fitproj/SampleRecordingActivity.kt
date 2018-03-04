/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teuskim.fitproj

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.teuskim.fitproj.logger.Log
import com.teuskim.fitproj.logger.LogView
import com.teuskim.fitproj.logger.LogWrapper
import com.teuskim.fitproj.logger.MessageOnlyLogFilter


/**
 * This sample demonstrates how to use the Recording API of the Google Fit platform to subscribe
 * to data sources, query against existing subscriptions, and remove subscriptions. It also
 * demonstrates how to authenticate a user with Google Play Services.
 */
class SampleRecordingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_recording_activity)
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging()

        val fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_ACTIVITY_SAMPLES).build()

        // Check if the user has permissions to talk to Fitness APIs, otherwise authenticate the
        // user and request required permissions.
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions)
        } else {
            subscribe()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                subscribe()
            }
        }
    }

    /**
     * Subscribes to an available [DataType]. Subscriptions can exist across application
     * instances (so data is recorded even after the application closes down).  When creating
     * a new subscription, it may already exist from a previous invocation of this app.  If
     * the subscription already exists, the method is a no-op.  However, you can check this with
     * a special success code.
     */
    fun subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener { Log.i(TAG, "Successfully subscribed!") }
                .addOnFailureListener { Log.i(TAG, "There was a problem subscribing.") }
        // [END subscribe_to_datatype]
    }

    /**
     * Fetches a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    private fun dumpSubscriptionsList() {
        // [START list_current_subscriptions]
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .listSubscriptions(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener { subscriptions ->
                    for (sc in subscriptions) {
                        val dt = sc.dataType
                        Log.i(TAG, "Active subscription for data type: " + dt.name)
                    }
                }
        // [END list_current_subscriptions]
    }

    /**
     * Cancels the ACTIVITY_SAMPLE subscription by calling unsubscribe on that [DataType].
     */
    private fun cancelSubscription() {
        val dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLES.toString()
        Log.i(TAG, "Unsubscribing from data type: " + dataTypeStr)

        // Invoke the Recording API to unsubscribe from the data type and specify a callback that
        // will check the result.
        // [START unsubscribe_from_datatype]
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener { Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr) }
                .addOnFailureListener {
                    // Subscription not removed
                    Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr)
                }
        // [END unsubscribe_from_datatype]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.sample_recording, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_cancel_subs) {
            cancelSubscription()
            return true
        } else if (id == R.id.action_dump_subs) {
            dumpSubscriptionsList()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Initializes a custom log class that outputs both to in-app targets and logcat.
     */
    private fun initializeLogging() {
        // Wraps Android's native log framework.
        val logWrapper = LogWrapper()
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.logNode = logWrapper
        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter
        // On screen logging via a customized TextView.
        val logView = findViewById<LogView>(R.id.sample_logview)

        logView.setBackgroundColor(Color.WHITE)
        msgFilter.next = logView
        Log.i(TAG, "Ready")
    }

    companion object {
        val TAG = "BasicRecordingApi"

        private val REQUEST_OAUTH_REQUEST_CODE = 1
    }
}
