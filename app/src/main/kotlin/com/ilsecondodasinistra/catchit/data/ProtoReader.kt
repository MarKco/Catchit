package com.ilsecondodasinistra.catchit.data

class ProtoException(message: String) : Exception(message)

/**
 * Just enough of the protobuf wire format to read GTFS-Realtime: the feed is ~50 KB and we use a handful of
 * fields, so a library (and its R8 rules) isn't worth it.
 */
class ProtoReader(private val buf: ByteArray, private var pos: Int = 0, private val end: Int = buf.size) {
    fun hasMore(): Boolean = pos < end

    /** Returns `fieldNumber shl 3 or wireType`. */
    fun readTag(): Int = readVarint().toInt()

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= end || shift > 63) throw ProtoException("Truncated varint")
            val b = buf[pos++].toInt()
            result = result or ((b and 0x7f).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
    }

    fun readLengthDelimited(): ProtoReader {
        val length = readVarint().toInt()
        if (length < 0 || pos + length > end) throw ProtoException("Truncated field")
        return ProtoReader(buf, pos, pos + length).also { pos += length }
    }

    fun readString(): String {
        val sub = readLengthDelimited()
        return String(buf, sub.pos, sub.end - sub.pos, Charsets.UTF_8)
    }

    fun skip(wireType: Int) {
        when (wireType) {
            WIRE_VARINT -> readVarint()
            WIRE_FIXED64 -> advance(8)
            WIRE_LENGTH -> readLengthDelimited()
            WIRE_FIXED32 -> advance(4)
            else -> throw ProtoException("Unsupported wire type $wireType")
        }
    }

    private fun advance(n: Int) {
        if (pos + n > end) throw ProtoException("Truncated field")
        pos += n
    }

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH = 2
        const val WIRE_FIXED32 = 5
    }
}

inline fun ProtoReader.forEachField(block: (field: Int, wireType: Int) -> Unit) {
    while (hasMore()) {
        val tag = readTag()
        block(tag ushr 3, tag and 7)
    }
}
