package com.bluesky.basic_tensorflow
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pluspark.basic_tensorflow.R
import java.util.HashMap

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val LABEL_FILE = "2350-common-hangul.txt"
    private val MODEL_FILE = "optimized_hangul_tensorflow.pb"

    private lateinit var classifier: HangulClassifier
    private lateinit var paintView: PaintView
    private lateinit var alt1: Button
    private lateinit var alt2: Button
    private lateinit var alt3: Button
    private lateinit var alt4: Button
    private lateinit var altLayout: LinearLayout
    private lateinit var resultText: EditText
    private lateinit var translationText: TextView
    private var currentTopLabels: Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        paintView = findViewById(R.id.paintView)

        val drawHereText = findViewById<TextView>(R.id.drawHere)
        paintView.setDrawText(drawHereText)

        val clearButton = findViewById<Button>(R.id.buttonClear)
        clearButton.setOnClickListener(this)

        val classifyButton = findViewById<Button>(R.id.buttonClassify)
        classifyButton.setOnClickListener(this)

        val backspaceButton = findViewById<Button>(R.id.buttonBackspace)
        backspaceButton.setOnClickListener(this)

        val spaceButton = findViewById<Button>(R.id.buttonSpace)
        spaceButton.setOnClickListener(this)

        val submitButton = findViewById<Button>(R.id.buttonSubmit)
        submitButton.setOnClickListener(this)

        altLayout = findViewById(R.id.altLayout)
        altLayout.visibility = View.INVISIBLE

        alt1 = findViewById(R.id.alt1)
        alt1.setOnClickListener(this)
        alt2 = findViewById(R.id.alt2)
        alt2.setOnClickListener(this)
        alt3 = findViewById(R.id.alt3)
        alt3.setOnClickListener(this)
        alt4 = findViewById(R.id.alt4)
        alt4.setOnClickListener(this)

        translationText = findViewById(R.id.translationText)
        resultText = findViewById(R.id.editText)

        loadModel()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.buttonClear -> clear()
            R.id.buttonClassify -> {
                classify()
                paintView.reset()
                paintView.invalidate()
            }
            R.id.buttonBackspace -> {
                backspace()
                altLayout.visibility = View.INVISIBLE
                paintView.reset()
                paintView.invalidate()
            }
            R.id.buttonSpace -> space()
            R.id.buttonSubmit -> {
                altLayout.visibility = View.INVISIBLE
                translate()
            }
            R.id.alt1, R.id.alt2, R.id.alt3, R.id.alt4 -> useAltLabel(view.tag.toString().toInt())
        }
    }

    private fun backspace() {
        val len = resultText.length()
        if (len > 0) {
            resultText.text.delete(len - 1, len)
        }
    }

    private fun space() {
        resultText.append(" ")
    }

    private fun clear() {
        paintView.reset()
        paintView.invalidate()
        resultText.setText("")
        translationText.text = ""
        altLayout.visibility = View.INVISIBLE
    }

    private fun classify() {
        val pixels = paintView.getPixelData()
        currentTopLabels = classifier.classify(pixels)
        resultText.append(currentTopLabels?.get(0))
        altLayout.visibility = View.VISIBLE
        alt1.text = currentTopLabels?.get(1)
        alt2.text = currentTopLabels?.get(2)
        alt3.text = currentTopLabels?.get(3)
        alt4.text = currentTopLabels?.get(4)
    }

    private fun translate() {
        val text = resultText.text.toString()
        if (text.isEmpty()) {
            return
        }

        val postData = HashMap<String, String>()
        postData["text"] = text
        postData["source"] = "ko"
        postData["target"] = "en"
        val apikey = ""//resources.getString(R.string.apikey)
        val url = ""//resources.getString(R.string.url)
        val translator = HangulTranslator(postData, translationText, apikey, url)
        translator.execute()
    }

    private fun useAltLabel(index: Int) {
        backspace()
        resultText.append(currentTopLabels?.get(index))
    }

    override fun onResume() {
        paintView.onResume()
        super.onResume()
    }

    override fun onPause() {
        paintView.onPause()
        super.onPause()
    }

    private fun loadModel() {
        Thread {
            try {
                classifier = HangulClassifier.create(assets, MODEL_FILE, LABEL_FILE, PaintView.FEED_DIMENSION, "input", "keep_prob", "output")
            } catch (e: Exception) {
                throw RuntimeException("Error loading pre-trained model.", e)
            }
        }.start()
    }
}
