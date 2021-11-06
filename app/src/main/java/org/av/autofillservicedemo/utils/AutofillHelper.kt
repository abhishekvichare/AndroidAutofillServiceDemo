package org.av.autofillservicedemo.utils

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.service.autofill.SaveInfo
import android.util.ArrayMap
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import org.av.autofillservicedemo.dao.ParsedStructure
import java.util.*

object AutofillHelper {
    private val TAG = "AutofillHelper"

    fun parseStructure(structure: AssistStructure): ParsedStructure? {
        val map = getAutofillableFields(structure)
        if (map.isNullOrEmpty() || checkIfDataContainInMap(map)) {
            return null
        }

        return ParsedStructure(
            usernameId = getEmailUserNameFromMap(map)!! as AutofillId,
            passwordId = map[View.AUTOFILL_HINT_PASSWORD.lowercase()] as AutofillId,
            saveType = storeSaveType(map)
        )
    }

    /**
     * Parses the [AssistStructure] representing the activity being autofilled, and returns a
     * map of autofillable fields (represented by their autofill ids) mapped by the hint associate
     * with them.
     *
     *
     * An autofillable field is a [ViewNode] whose [.getHint] method
     */
    private fun getAutofillableFields(structure: AssistStructure): Map<String, AutofillId?> {
        val fields: MutableMap<String, AutofillId?> = ArrayMap()
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            addAutofillableFields(fields, node)
        }
        return fields
    }

    /**
     * Adds any autofillable view from the [ViewNode] and its descendants to the map.
     */
    private fun addAutofillableFields(
        fields: MutableMap<String, AutofillId?>,
        node: ViewNode
    ) {
        val hints = node.autofillHints
        if (hints != null) {
            // We're simple, we only care about the first hint
            val hint = hints[0].lowercase(Locale.getDefault())
            val id = node.autofillId
            if (!fields.containsKey(hint)) {
                Log.d(
                    TAG,
                    "Setting hint '$hint' on $id"
                )
                fields[hint] = id
            } else {
                Log.d(
                    TAG,
                    "Ignoring hint '" + hint + "' on " + id
                            + " because it was already set"
                )
            }
        }
        val childrenSize = node.childCount
        for (i in 0 until childrenSize) {
            addAutofillableFields(fields, node.getChildAt(i))
        }
    }

    /**
     * Traverse structure for storing data
     */
    fun traverseStructure(structure: AssistStructure): Map<String, String>? {
        val fields: MutableMap<String, String> = ArrayMap()

        val windowNodes: List<AssistStructure.WindowNode> =
            structure.run {
                (0 until windowNodeCount).map { getWindowNodeAt(it) }
            }

        windowNodes.forEach { windowNode: AssistStructure.WindowNode ->
            val viewNode: ViewNode? = windowNode.rootViewNode
            traverseNode(viewNode, fields)
        }

        fields.forEach { (key, value) -> Log.d(TAG, "$key -> $value") }
        if (fields.isNullOrEmpty() || checkIfDataContainInMap(fields)) {
            return null
        }
        return fields
    }

    /**
     * Traverse all the elements to store details in the map like username, email, password
     */
    private fun traverseNode(viewNode: ViewNode?, fields: MutableMap<String, String>) {
        if (viewNode?.autofillHints?.isNotEmpty() == true) {
            // We're simple, we only care about the first hint
            val hint = viewNode.autofillHints!![0].lowercase(Locale.getDefault())
            val id = viewNode.text.toString()
            if (!fields.containsKey(hint)) {
                //Log.d(TAG,"Setting text '$hint' on $id")
                fields[hint] = id
            } else {
                //  Log.d(TAG,"Ignoring text '$hint' on  $id  because it was already set")
            }
        }

        val children: List<ViewNode>? =
            viewNode?.run {
                (0 until childCount).map { getChildAt(it) }
            }

        children?.forEach { childNode: ViewNode ->
            traverseNode(childNode, fields)
        }
    }

    private fun checkIfDataContainInMap(map: Map<String, Any?>): Boolean {
        val keys = map.keys
        return !((keys.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase()) || keys.contains(
            View.AUTOFILL_HINT_USERNAME.lowercase()
        )) && keys.contains(View.AUTOFILL_HINT_PASSWORD.lowercase()))
    }

    /**
     * Return value against username or email key from given map.
     * The value can be AutoFillId if called while suggesting values or
     * String if called while saving the values
     */
    private fun getEmailUserNameFromMap(map: Map<String, Any?>): Any? {
        val keys = map.keys
        return when {
            keys.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase()) -> {
                map[View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase()]
            }
            keys.contains(View.AUTOFILL_HINT_USERNAME.lowercase()) -> {
                map[View.AUTOFILL_HINT_USERNAME.lowercase()]
            }
            else -> {
                null
            }
        }
    }

    /**
     * Store the save types in order to save the details later when user navigates from the screen.
     */
    private fun storeSaveType(map: Map<String, AutofillId?>): Int {
        var saveType = SaveInfo.SAVE_DATA_TYPE_GENERIC
        map.forEach {
            when (it.key) {
                View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase() -> {
                    saveType = saveType or SaveInfo.SAVE_DATA_TYPE_USERNAME
                }
                View.AUTOFILL_HINT_USERNAME.lowercase() -> {
                    saveType = saveType or SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS
                }
                View.AUTOFILL_HINT_PASSWORD.lowercase() -> {
                    saveType = saveType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                }
            }
        }
        return saveType
    }

}