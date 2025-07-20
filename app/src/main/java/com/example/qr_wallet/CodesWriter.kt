package com.example.qr_wallet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CodesWriter {
    fun addCode(context: Context, code: String, name: String = "Scanned QR code") {
        val file = File(context.filesDir, "codes.json")
        val jsonString = if (file.exists()) file.readText() else "{\"codes\": []}"
        val jsonObject = JSONObject(jsonString)
        val codesArray = jsonObject.getJSONArray("codes")
        val newCode = JSONObject()
        newCode.put("code", code)
        newCode.put("name", name)
        codesArray.put(newCode)
        jsonObject.put("codes", codesArray)
        file.writeText(jsonObject.toString(4))
    }
}

