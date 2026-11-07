package com.decompress.app.data

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * 鍩虹 ZIP 淇锛氶『搴忔壂鎻忔湰鍦版枃浠跺ご锛岃烦杩囨崯鍧忔锛岄噸寤轰腑澶洰褰曚笌 EOCD銆? * - 鏀寔 stored/deflate 绛夊師濮嬫暟鎹洿鎺ユ惉杩愶紱
 * - 鏀寔 data descriptor锛坆it 3锛夋潯鐩紝閫氳繃鎵弿涓嬩竴涓枃浠跺ご瀹氫綅鏁版嵁缁撳熬锛? * - 涓嶅鐞?ZIP64銆? */
private fun ByteBuffer.putShortLe(value: Int): ByteBuffer = putShort(value.toShort())
object ZipRepair {
    data class Result(val file: File, val entries: Int, val skipped: Int)

    private const val LOCAL = 0x04034b50
    private const val CENTRAL = 0x02014b50
    private const val EOCD = 0x06054b50
    private const val DESCRIPTOR = 0x08074b50

    fun repair(source: File, output: File): Result? {
        val input = RandomAccessFile(source, "r")
        val out = RandomAccessFile(output, "rw")
        out.setLength(0)
        var entries = 0
        var skipped = 0
        val central = ArrayList<Pair<ByteArray, Long>>()
        try {
            val size = input.length()
            var pos = 0L
            while (pos + 30 <= size) {
                input.seek(pos)
                if (input.readIntLe() != LOCAL) {
                    pos += 1
                    continue
                }
                val headerStart = pos
                val version = input.readShortLe()
                val flags = input.readShortLe()
                val method = input.readShortLe()
                val time = input.readShortLe()
                val date = input.readShortLe()
                var crc = input.readIntLe()
                var compressed = input.readIntLe()
                val uncompressed = input.readIntLe()
                val nameLength = input.readShortLe()
                val extraLength = input.readShortLe()
                if (nameLength < 0 || extraLength < 0 ||
                    headerStart + 30 + nameLength + extraLength > size
                ) {
                    skipped++
                    pos += 1
                    continue
                }
                val nameBytes = ByteArray(nameLength)
                input.readFully(nameBytes)
                val extra = ByteArray(extraLength)
                input.readFully(extra)
                val dataStart = headerStart + 30 + nameLength + extraLength
                if (compressed == -1 || uncompressed == -1) {
                    skipped++
                    pos = dataStart
                    continue
                }
                var dataEnd: Long
                var nextPosition: Long
                if (flags and 0x08 == 0) {
                    dataEnd = dataStart + compressed
                    if (dataEnd > size) {
                        skipped++
                        break
                    }
                    nextPosition = dataEnd
                } else {
                    val next = findNextHeader(input, dataStart, size)
                    if (next < 0) {
                        skipped++
                        break
                    }
                    var end = next
                    if (next - 16 >= dataStart) {
                        input.seek(next - 16)
                        if (input.readIntLe() == DESCRIPTOR) {
                            input.seek(next - 12)
                            crc = input.readIntLe()
                            compressed = input.readIntLe()
                            end = next - 16
                        }
                    }
                    dataEnd = end
                    compressed = (dataEnd - dataStart).toInt()
                    nextPosition = next
                }
                val localHeader = ByteBuffer.allocate(30 + nameLength + extraLength)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(LOCAL)
                    .putShortLe(version)
                    .putShortLe(flags)
                    .putShortLe(method)
                    .putShortLe(time)
                    .putShortLe(date)
                    .putInt(crc)
                    .putInt(compressed)
                    .putInt(uncompressed)
                    .putShortLe(nameLength)
                    .putShortLe(extraLength)
                    .put(nameBytes)
                    .put(extra)
                    .array()
                val localOffset = out.length()
                out.write(localHeader)
                copyRange(input, out, dataStart, dataEnd)
                val centralRecord = ByteBuffer.allocate(46 + nameLength + extraLength)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(CENTRAL)
                    .putShortLe(version)
                    .putShortLe(version)
                    .putShortLe(flags)
                    .putShortLe(method)
                    .putShortLe(time)
                    .putShortLe(date)
                    .putInt(crc)
                    .putInt(compressed)
                    .putInt(uncompressed)
                    .putShortLe(nameLength)
                    .putShortLe(extraLength)
                    .putShortLe(0)
                    .putShortLe(0)
                    .putShortLe(0)
                    .putInt(0)
                    .putInt(localOffset.toInt())
                    .put(nameBytes)
                    .array()
                central += centralRecord to localOffset
                entries++
                pos = nextPosition
            }
            if (entries == 0) return null
            val centralStart = out.length()
            central.forEach { (record, _) -> out.write(record) }
            val centralSize = out.length() - centralStart
            val eocd = ByteBuffer.allocate(22)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(EOCD)
                .putShortLe(0)
                .putShortLe(0)
                .putShortLe(entries)
                .putShortLe(entries)
                .putInt(centralSize.toInt())
                .putInt(centralStart.toInt())
                .putShortLe(0)
                .array()
            out.write(eocd)
        } finally {
            out.close()
            input.close()
        }
        return runCatching {
            ZipFile(output).use { zip ->
                val count = zip.entries().asSequence().count()
                check(count > 0)
            }
            Result(output, entries, skipped)
        }.getOrNull()
    }

    private fun findNextHeader(input: RandomAccessFile, start: Long, size: Long): Long {
        var pos = start
        val buffer = ByteArray(1 shl 16)
        while (pos + 4 <= size) {
            input.seek(pos)
            val read = input.read(buffer)
            if (read <= 0) return -1
            for (i in 0 until read - 3) {
                if (buffer[i] == 'P'.code.toByte() &&
                    buffer[i + 1] == 'K'.code.toByte() &&
                    buffer[i + 2] == 3.toByte() &&
                    buffer[i + 3] == 4.toByte()
                ) {
                    return pos + i
                }
                if (buffer[i] == 'P'.code.toByte() &&
                    buffer[i + 1] == 'K'.code.toByte() &&
                    buffer[i + 2] == 1.toByte() &&
                    buffer[i + 3] == 2.toByte()
                ) {
                    return pos + i
                }
            }
            pos += (read - 3).coerceAtLeast(1)
        }
        return -1
    }

    private fun copyRange(input: RandomAccessFile, out: RandomAccessFile, start: Long, end: Long) {
        input.seek(start)
        var remaining = end - start
        val buffer = ByteArray(1 shl 16)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) break
            out.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun RandomAccessFile.readIntLe(): Int =
        Integer.reverseBytes(readInt())

    private fun RandomAccessFile.readShortLe(): Int {
        val low = read()
        val high = read()
        return (low and 0xFF) or ((high and 0xFF) shl 8)
    }
}
