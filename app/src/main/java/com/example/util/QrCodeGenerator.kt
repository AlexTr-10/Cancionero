package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import java.util.BitSet

/**
 * Pure Kotlin QR Code Generator (Versions 1-4, Byte Mode, Low/Medium ECC).
 * Generates scanable QR Code Bitmaps for URLs (e.g. https://wa.me/...) and text without external dependencies.
 */
object QrCodeGenerator {

    /**
     * Generates a square Bitmap QR Code for the given string content.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val matrix = encodeToMatrix(content)
        val matrixSize = matrix.size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val moduleSize = size / matrixSize
        val quietZone = (size - (moduleSize * matrixSize)) / 2

        for (y in 0 until size) {
            for (x in 0 until size) {
                val matX = (x - quietZone) / moduleSize
                val matY = (y - quietZone) / moduleSize

                val isBlack = if (matX in 0 until matrixSize && matY in 0 until matrixSize) {
                    matrix[matY][matX]
                } else {
                    false
                }
                bitmap.setPixel(x, y, if (isBlack) foregroundColor else backgroundColor)
            }
        }
        return bitmap
    }

    private fun encodeToMatrix(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val dataLen = bytes.size

        // Select QR Version based on byte length (using ECC Level M)
        val version = when {
            dataLen <= 14 -> 1  // 21x21 modules, 16 data capacity (ECC M)
            dataLen <= 26 -> 2  // 25x25 modules, 28 data capacity (ECC M)
            dataLen <= 42 -> 3  // 29x29 modules, 44 data capacity (ECC M)
            else -> 4          // 33x33 modules, 64 data capacity (ECC M)
        }

        val qrSize = 17 + 4 * version
        val matrix = Array(qrSize) { BooleanArray(qrSize) }
        val reserved = Array(qrSize) { BooleanArray(qrSize) }

        // 1. Draw Finder Patterns (7x7)
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, qrSize - 7, 0)
        drawFinderPattern(matrix, reserved, 0, qrSize - 7)

        // 2. Draw Alignment Pattern (for Version 2+)
        if (version >= 2) {
            val alignPos = qrSize - 7
            drawAlignmentPattern(matrix, reserved, alignPos, alignPos)
        }

        // 3. Draw Timing Patterns
        for (i in 8 until qrSize - 8) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // 4. Reserve Format Info Areas
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[8][qrSize - 1 - i] = true
            reserved[qrSize - 1 - i][8] = true
        }

        // Dark Module
        matrix[qrSize - 8][8] = true
        reserved[qrSize - 8][8] = true

        // 5. Build Data Stream (Byte Mode)
        val bitBuffer = mutableListOf<Boolean>()

        // Mode Indicator for Byte Mode: 0100
        addBits(bitBuffer, 0b0100, 4)

        // Character Count Indicator (8 bits for Version 1-9)
        addBits(bitBuffer, dataLen, 8)

        // Data Bytes
        for (b in bytes) {
            addBits(bitBuffer, b.toInt() and 0xFF, 8)
        }

        // Total data capacity in bits for ECC M
        val maxDataBits = when (version) {
            1 -> 128   // 16 bytes
            2 -> 224   // 28 bytes
            3 -> 352   // 44 bytes
            else -> 512 // 64 bytes
        }

        // Terminator bits
        val termLen = minOf(4, maxDataBits - bitBuffer.size)
        if (termLen > 0) {
            addBits(bitBuffer, 0, termLen)
        }

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(false)
        }

        // Pad bytes (0xEC, 0x11)
        var padToggle = false
        while (bitBuffer.size < maxDataBits) {
            val padByte = if (padToggle) 0x11 else 0xEC
            addBits(bitBuffer, padByte, 8)
            padToggle = !padToggle
        }

        // Convert data bits to data bytes
        val dataBytes = IntArray(bitBuffer.size / 8)
        for (i in dataBytes.indices) {
            var v = 0
            for (b in 0 until 8) {
                if (bitBuffer[i * 8 + b]) {
                    v = v or (1 shl (7 - b))
                }
            }
            dataBytes[i] = v
        }

        // 6. Generate Error Correction Codewords (Reed-Solomon)
        val eccCount = when (version) {
            1 -> 10
            2 -> 16
            3 -> 26
            else -> 36
        }

        val eccBytes = calculateReedSolomon(dataBytes, eccCount)

        // Combine Data + ECC
        val allBits = mutableListOf<Boolean>()
        for (d in dataBytes) addBits(allBits, d, 8)
        for (e in eccBytes) addBits(allBits, e, 8)

        // 7. Place Bits in Matrix
        var bitIndex = 0
        var right = qrSize - 1
        var upward = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column

            val columnPair = intArrayOf(right, right - 1)
            val rows = if (upward) (qrSize - 1 downTo 0) else (0 until qrSize)

            for (y in rows) {
                for (x in columnPair) {
                    if (!reserved[y][x]) {
                        val bit = if (bitIndex < allBits.size) allBits[bitIndex++] else false

                        // Apply Mask Pattern 0: (x + y) % 2 == 0
                        val maskCondition = (x + y) % 2 == 0
                        matrix[y][x] = bit xor maskCondition
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // 8. Place Format Information (Mask 0, ECC M)
        // Format string for ECC M, Mask 0 with BCH(15,5) error correction: 0x5412 -> 101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)

        // Draw Format Bits
        var fIdx = 0
        // Top-left
        for (x in 0..8) {
            if (x == 6) continue
            matrix[8][x] = formatBits[fIdx] == 1
            fIdx++
        }
        for (y in 7 downTo 0) {
            if (y == 6) continue
            matrix[y][8] = formatBits[fIdx] == 1
            fIdx++
        }

        // Split format bits for top-right and bottom-left
        fIdx = 0
        for (y in qrSize - 1 downTo qrSize - 7) {
            matrix[y][8] = formatBits[fIdx] == 1
            fIdx++
        }
        for (x in qrSize - 8 until qrSize) {
            matrix[8][x] = formatBits[fIdx] == 1
            fIdx++
        }

        return matrix
    }

    private fun drawFinderPattern(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        startX: Int,
        startY: Int
    ) {
        for (r in -1..7) {
            for (c in -1..7) {
                val x = startX + c
                val y = startY + r
                if (x in matrix.indices && y in matrix.indices) {
                    reserved[y][x] = true
                    if (r in 0..6 && c in 0..6) {
                        matrix[y][x] = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    } else {
                        matrix[y][x] = false // Separator
                    }
                }
            }
        }
    }

    private fun drawAlignmentPattern(
        matrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        centerX: Int,
        centerY: Int
    ) {
        for (r in -2..2) {
            for (c in -2..2) {
                val x = centerX + c
                val y = centerY + r
                if (x in matrix.indices && y in matrix.indices && !reserved[y][x]) {
                    reserved[y][x] = true
                    matrix[y][x] = (Math.abs(r) == 2 || Math.abs(c) == 2 || (r == 0 && c == 0))
                }
            }
        }
    }

    private fun addBits(list: MutableList<Boolean>, value: Int, numBits: Int) {
        for (i in numBits - 1 downTo 0) {
            list.add(((value shr i) and 1) == 1)
        }
    }

    /**
     * Reed-Solomon Error Correction Code Generator in Galois Field GF(256)
     */
    private fun calculateReedSolomon(data: IntArray, eccCount: Int): IntArray {
        // GF(256) log and exp tables with primitive polynomial 0x11D
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            exp[i + 255] = x
            log[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }

        // Generator polynomial for eccCount
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val newGen = IntArray(gen.size + 1)
            val rootExp = exp[i]
            for (j in gen.indices) {
                newGen[j] = newGen[j] xor gfMultiply(gen[j], rootExp, exp, log)
                newGen[j + 1] = newGen[j + 1] xor gen[j]
            }
            gen = newGen
        }

        // Divide data by generator polynomial
        val res = IntArray(eccCount)
        for (d in data) {
            val factor = d xor res[0]
            System.arraycopy(res, 1, res, 0, eccCount - 1)
            res[eccCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until eccCount) {
                    res[j] = res[j] xor gfMultiply(gen[j], factor, exp, log)
                }
            }
        }
        return res
    }

    private fun gfMultiply(a: Int, b: Int, exp: IntArray, log: IntArray): Int {
        if (a == 0 || b == 0) return 0
        return exp[log[a] + log[b]]
    }
}
