package com.codex.sleepmonitor.service

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class RollingPcmBuffer(
    private val capacitySamples: Int
) {
    private val buffer = ShortArray(capacitySamples)
    private var writeIndex = 0
    private var size = 0

    fun append(samples: ShortArray, length: Int) {
        for (index in 0 until length.coerceAtMost(samples.size)) {
            buffer[writeIndex] = samples[index]
            writeIndex = (writeIndex + 1) % capacitySamples
            if (size < capacitySamples) {
                size++
            }
        }
    }

    fun snapshot(maxSamples: Int = size): ShortArray {
        val outputSize = maxSamples.coerceAtMost(size)
        val start = (writeIndex - outputSize + capacitySamples) % capacitySamples
        val output = ShortArray(outputSize)
        for (index in 0 until outputSize) {
            output[index] = buffer[(start + index) % capacitySamples]
        }
        return output
    }
}

object WavWriter {
    fun writePcm16Mono(file: File, sampleRate: Int, samples: ShortArray) {
        file.parentFile?.mkdirs()
        DataOutputStream(FileOutputStream(file)).use { output ->
            val dataSize = samples.size * 2
            val chunkSize = 36 + dataSize

            output.writeBytes("RIFF")
            output.writeIntLE(chunkSize)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(1)
            output.writeIntLE(sampleRate)
            output.writeIntLE(sampleRate * 2)
            output.writeShortLE(2)
            output.writeShortLE(16)
            output.writeBytes("data")
            output.writeIntLE(dataSize)

            samples.forEach { sample ->
                output.writeShortLE(sample.toInt())
            }
        }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte(value shr 8 and 0xFF)
        writeByte(value shr 16 and 0xFF)
        writeByte(value shr 24 and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte(value shr 8 and 0xFF)
    }
}
