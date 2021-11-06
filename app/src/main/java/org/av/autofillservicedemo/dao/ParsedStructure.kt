package org.av.autofillservicedemo.dao

import android.view.autofill.AutofillId

data class ParsedStructure(
    var usernameId: AutofillId,
    var passwordId: AutofillId,
    var saveType: Int
)