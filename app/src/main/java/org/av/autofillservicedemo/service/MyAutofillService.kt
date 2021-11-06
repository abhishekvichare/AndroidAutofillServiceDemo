package org.av.autofillservicedemo.service

import android.R
import android.app.assist.AssistStructure
import android.os.Bundle
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import org.av.autofillservicedemo.dao.ParsedStructure
import org.av.autofillservicedemo.utils.AutofillHelper

class MyAutofillService : AutofillService() {
    private val TAG = this::class.java.simpleName

    override fun onFillRequest(
        request: FillRequest,
        CancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {

        /**
         * Get the existing save type information from the request so that it can be
         * appended to this request for storing data
         */
        var saveType = SaveInfo.SAVE_DATA_TYPE_GENERIC
        var clientState = request.clientState
        if (clientState != null) {
            saveType = clientState.getInt(
                "saveType",
                saveType
            )
        }

        val context: List<FillContext> = request.fillContexts
        val structure: AssistStructure = context[context.size - 1].structure

        // Traverse the structure looking for nodes to fill out.
        val parsedStructure: ParsedStructure? = AutofillHelper.parseStructure(structure)

        if (parsedStructure == null) {
            callback.onFailure("Error")
            return
        }

        // Build the presentation of the datasets
        val usernamePresentation = RemoteViews(packageName, R.layout.simple_list_item_1)
        usernamePresentation.setTextViewText(R.id.text1, "my_username")
        val passwordPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        passwordPresentation.setTextViewText(R.id.text1, "Password for my_username")

        if (clientState == null) {
            // Initial request
            clientState = Bundle()
        }

        saveType = parsedStructure.saveType

        clientState.putInt(
            "saveType",
            saveType
        )

        // Add a dataset to the response
        val fillResponse: FillResponse = FillResponse.Builder()
            .addDataset(
                Dataset.Builder()
                    .setValue(
                        parsedStructure.usernameId,
                        AutofillValue.forText("abc@def.com"),
                        usernamePresentation
                    )
                    .setValue(
                        parsedStructure.passwordId,
                        AutofillValue.forText("abc"),
                        passwordPresentation
                    )
                    .build()
            )
            .setClientState(clientState)
            .setSaveInfo(
                SaveInfo.Builder(
                    saveType,
                    arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)
                ).build()
            )
            .build()


        // If there are no errors, call onSuccess() and pass the response
        callback.onSuccess(fillResponse)

    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val context = request.fillContexts
        val structure = context[context.size - 1].structure
        Log.d(TAG, "Save request for ${structure.activityComponent.packageName}")
        val mapOfData = AutofillHelper.traverseStructure(structure)
        if (mapOfData != null) {
            Log.d(TAG, mapOfData.toString())
            // Write your own logic to store the data in.
            // Please make sure to encrypt the sensitive data before storing
        }

        callback.onSuccess()
    }
}