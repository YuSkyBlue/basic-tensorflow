package com.bluesky.basic_tensorflow
import android.content.res.AssetManager
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class HangulClassifier private constructor() {
    private lateinit var tfInterface: TensorFlowInferenceInterface
    private lateinit var inputName: String
    private lateinit var keepProbName: String
    private lateinit var outputName: String
    private var imageDimension = 0
    private lateinit var labels: List<String>
    private lateinit var output: FloatArray
    private lateinit var outputNames: Array<String>

    companion object {
        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager, modelPath: String, labelFile: String,
            inputDimension: Int, inputName: String, keepProbName: String,
            outputName: String
        ): HangulClassifier {
            val classifier = HangulClassifier()

            classifier.inputName = inputName
            classifier.keepProbName = keepProbName
            classifier.outputName = outputName
            classifier.labels = readLabels(assetManager, labelFile)
            classifier.tfInterface = TensorFlowInferenceInterface(assetManager, modelPath)
            val numClasses = classifier.labels.size
            classifier.imageDimension = inputDimension
            classifier.outputNames = arrayOf(outputName)
            classifier.output = FloatArray(numClasses)

            return classifier
        }

        @Throws(IOException::class)
        private fun readLabels(am: AssetManager, fileName: String): List<String> {
            val reader = BufferedReader(InputStreamReader(am.open(fileName)))
            val labels = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                labels.add(line!!)
            }
            reader.close()
            return labels
        }
    }

    fun classify(pixels: FloatArray): Array<String> {
        tfInterface.feed(inputName, pixels, 1, imageDimension.toLong(), imageDimension.toLong(), 1)
        tfInterface.feed(keepProbName, floatArrayOf(1f))
        tfInterface.run(outputNames)
        tfInterface.fetch(outputName, output)

        val map = TreeMap<Float, Int>()
        for (i in output.indices) {
            map[output[i]] = i
        }
        output.sort()

        val topLabels = Array(5) { "" }
        for (i in output.size downTo output.size - 5 + 1) {
            topLabels[output.size - i] = labels[map[output[i - 1]]!!]
        }
        return topLabels
    }
}
