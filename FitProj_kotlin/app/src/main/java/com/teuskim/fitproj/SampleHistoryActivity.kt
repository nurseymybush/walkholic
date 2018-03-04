/*
 * Copyright (C) 2016 Google, Inc.
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
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataUpdateRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import com.teuskim.fitproj.logger.Log
import com.teuskim.fitproj.logger.LogView
import com.teuskim.fitproj.logger.LogWrapper
import com.teuskim.fitproj.logger.MessageOnlyLogFilter
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getTimeInstance
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate a user
 * with Google Play Services and how to properly represent data in a [DataSet].
 */
class SampleHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_history_activity)
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging()

        val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .build()
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions)
        } else {
            insertAndReadData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                insertAndReadData()
            }
        }
    }

    /**
     * Inserts and reads data by chaining [Task] from [.insertData] and [ ][.readHistoryData].
     */
    private fun insertAndReadData() {
        insertData()
                .continueWithTask { readHistoryData() }
    }

    /** Creates a [DataSet] and inserts it into user's Google Fit history.  */
    private fun insertData(): Task<Void> {
        // Create a new dataset and insertion request.
        val dataSet = insertFitnessData()

        // Then, invoke the History API to insert the data.
        Log.i(TAG, "Inserting the dataset in the History API.")
        return Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .insertData(dataSet)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // At this point, the data has been inserted and can be read.
                        Log.i(TAG, "Data insert was successful!")
                    } else {
                        Log.e(TAG, "There was a problem inserting the dataset.", task.exception)
                    }
                }
    }

    /**
     * Asynchronous task to read the history data. When the task succeeds, it will print out the data.
     */
    private fun readHistoryData(): Task<DataReadResponse> {
        // Begin by creating the query.
        val readRequest = queryFitnessData()

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResponse ->
                    // For the sake of the sample, we'll print the data so we can see what we just
                    // added. In general, logging fitness information should be avoided for privacy
                    // reasons.
                    printData(dataReadResponse)
                }
                .addOnFailureListener { e -> Log.e(TAG, "There was a problem reading the data.", e) }
    }

    /**
     * Creates and returns a [DataSet] of step count data for insertion using the History API.
     */
    private fun insertFitnessData(): DataSet {
        Log.i(TAG, "Creating a new data insert request.")

        // [START build_insert_data_request]
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = cal.timeInMillis

        // Create a data source
        val dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setStreamName(TAG + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build()

        // Create a data set
        val stepCountDelta = 950
        val dataSet = DataSet.create(dataSource)
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        val dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta)
        dataSet.add(dataPoint)
        // [END build_insert_data_request]

        return dataSet
    }
    // [END parse_dataset]

    /**
     * Deletes a [DataSet] from the History API. In this example, we delete all step count data
     * for the past 24 hours.
     */
    private fun deleteData() {
        Log.i(TAG, "Deleting today's step count data.")

        // [START delete_dataset]
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        //  Create a delete request object, providing a data type and a time interval
        val request = DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build()

        // Invoke the History API with the HistoryClient object and delete request, and then
        // specify a callback that will check the result.
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .deleteData(request)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Successfully deleted today's step count data.")
                    } else {
                        Log.e(TAG, "Failed to delete today's step count data.", task.exception)
                    }
                }
    }

    /**
     * Updates and reads data by chaning [Task] from [.updateData] and [ ][.readHistoryData].
     */
    private fun updateAndReadData() {
        updateData()
                .continueWithTask { readHistoryData() }
    }

    /**
     * Creates a [DataSet],then makes a [DataUpdateRequest] to update step data. Then
     * invokes the History API with the HistoryClient object and update request.
     */
    private fun updateData(): Task<Void> {
        // Create a new dataset and update request.
        val dataSet = updateFitnessData()
        var startTime: Long = 0
        var endTime: Long = 0

        // Get the start and end times from the dataset.
        for (dataPoint in dataSet.dataPoints) {
            startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
            endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS)
        }

        // [START update_data_request]
        Log.i(TAG, "Updating the dataset in the History API.")

        val request = DataUpdateRequest.Builder()
                .setDataSet(dataSet)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        // Invoke the History API to update data.
        return Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .updateData(request)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // At this point the data has been updated and can be read.
                        Log.i(TAG, "Data update was successful.")
                    } else {
                        Log.e(TAG, "There was a problem updating the dataset.", task.exception)
                    }
                }
    }

    /** Creates and returns a [DataSet] of step count data to update.  */
    private fun updateFitnessData(): DataSet {
        Log.i(TAG, "Creating a new data update request.")

        // [START build_update_data_request]
        // Set a start and end time for the data that fits within the time range
        // of the original insertion.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        cal.add(Calendar.MINUTE, 0)
        val endTime = cal.timeInMillis
        cal.add(Calendar.MINUTE, -50)
        val startTime = cal.timeInMillis

        // Create a data source
        val dataSource = DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setStreamName(TAG + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build()

        // Create a data set
        val stepCountDelta = 1000
        val dataSet = DataSet.create(dataSource)
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        val dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta)
        dataSet.add(dataPoint)
        // [END build_update_data_request]

        return dataSet
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.sample_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_delete_data) {
            deleteData()
            return true
        } else if (id == R.id.action_update_data) {
            clearLogView()
            updateAndReadData()
        }
        return super.onOptionsItemSelected(item)
    }

    /** Clears all the logging message in the LogView.  */
    private fun clearLogView() {
        val logView = findViewById<LogView>(R.id.sample_logview)
        logView.text = ""
    }

    /** Initializes a custom log class that outputs both to in-app targets and logcat.  */
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

        // Fixing this lint error adds logic without benefit.

        //        logView.setTextAppearance(R.style.Log);

        logView.setBackgroundColor(Color.WHITE)
        msgFilter.next = logView
        Log.i(TAG, "Ready.")
    }

    companion object {
        val TAG = "BasicHistoryApi"
        // Identifier to identify the sign in activity.
        private val REQUEST_OAUTH_REQUEST_CODE = 1

        /** Returns a [DataReadRequest] for all step count changes in the past week.  */
        fun queryFitnessData(): DataReadRequest {
            // [START build_read_data_request]
            // Setting a start and end date using a range of 1 week before this moment.
            val cal = Calendar.getInstance()
            val now = Date()
            cal.time = now
            val endTime = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            val startTime = cal.timeInMillis

            val dateFormat = getDateInstance()
            Log.i(TAG, "Range Start: " + dateFormat.format(startTime))
            Log.i(TAG, "Range End: " + dateFormat.format(endTime))

// [END build_read_data_request]

            return DataReadRequest.Builder()
                    // The data request can specify multiple data types to return, effectively
                    // combining multiple data queries into one call.
                    // In this example, it's very unlikely that the request is for several hundred
                    // datapoints each consisting of a few steps and a timestamp.  The more likely
                    // scenario is wanting to see how many steps were walked per day, for 7 days.
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                    // bucketByTime allows for a time span, whereas bucketBySession would allow
                    // bucketing by "sessions", which would need to be defined in code.
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
        }

        /**
         * Logs a record of the query result. It's possible to get more constrained data sets by
         * specifying a data source or data type, but for demonstrative purposes here's how one would dump
         * all the data. In this sample, logging also prints to the device screen, so we can see what the
         * query returns, but your app should not log fitness information as a privacy consideration. A
         * better option would be to dump the data you receive to a local data directory to avoid exposing
         * it to other applications.
         */
        fun printData(dataReadResult: DataReadResponse) {
            // [START parse_read_data_result]
            // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
            // as buckets containing DataSets, instead of just DataSets.
            if (dataReadResult.buckets.size > 0) {
                Log.i(
                        TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
                for (bucket in dataReadResult.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        dumpDataSet(dataSet)
                    }
                }
            } else if (dataReadResult.dataSets.size > 0) {
                Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
                for (dataSet in dataReadResult.dataSets) {
                    dumpDataSet(dataSet)
                }
            }
            // [END parse_read_data_result]
        }

        // [START parse_dataset]
        private fun dumpDataSet(dataSet: DataSet) {
            Log.i(TAG, "Data returned for Data type: " + dataSet.dataType.name)
            val dateFormat = getTimeInstance()

            for (dp in dataSet.dataPoints) {
                Log.i(TAG, "Data point:")
                Log.i(TAG, "\tType: " + dp.dataType.name)
                Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)))
                Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))
                for (field in dp.dataType.fields) {
                    Log.i(TAG, "\tField: " + field.name + " Value: " + dp.getValue(field))
                }
            }
        }
    }
}
