package com.bluesky.basic_tensorflow

import android.os.AsyncTask
import android.util.Base64
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HangulTranslator(
    private val postData: Any?,
    private val view: TextView,
    private val apikey: String,
    private val url: String
) : AsyncTask<String, Void, String>() {
    // Constructor for JSONObject
    constructor(
        postData: JSONObject?,
        view: TextView,
        apikey: String,
        url: String
    ) : this(postData as Any?, view, apikey, url)

    // Constructor for Map<String, String>
    constructor(
        postData: Map<String, String>?,
        view: TextView,
        apikey: String,
        url: String
    ) : this(postData?.let { JSONObject(it) }, view, apikey, url)

    override fun doInBackground(vararg params: String): String {
        var result = ""
        try {
            val url = URL("$url/v3/translate?version=2019-01-08")
            val urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.doInput = true
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.requestMethod = "POST"

            // Set authorization header.
            val authString = "apikey:$apikey"
            val base64Bytes = Base64.encode(authString.toByteArray(), Base64.DEFAULT)
            val base64String = String(base64Bytes)
            urlConnection.setRequestProperty("Authorization", "Basic $base64String")

            postData?.let {
                val writer = OutputStreamWriter(urlConnection.outputStream)
                writer.write(it.toString())
                writer.flush()
                writer.close()
            }

            val statusCode = urlConnection.responseCode
            if (statusCode == 200) {
                val streamReader = InputStreamReader(urlConnection.inputStream)
                val bufferedReader = BufferedReader(streamReader)
                var inputLine: String?
                val response = StringBuilder()

                while (bufferedReader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                streamReader.close()
                bufferedReader.close()
                result = response.toString()
            } else {
                println("Error translating. Response Code: $statusCode")
            }
        } catch (e: Exception) {
            println(e.message)
        }
        return result
    }

    override fun onPostExecute(result: String) {
        if (result.isEmpty()) {
            return
        }
        try {
            val json = JSONObject(result)
            val array = json.optJSONArray("translations")
            val translation = array.optJSONObject(0).opt("translation")
            view.text = translation.toString()
        } catch (e: Exception) {
            println(e.message)
        }
    }
}