package com.hasangwon.planwidget.app.backup

/**
 * 내보내기 타입 2 파일(.redcalendar) 읽기 (iOS ExportType2Importer 와 같은 바이트 규칙).
 *
 * 파일은 Realm DB(파일 형식 24) 그대로라서, 라이브러리 없이 필요한 부분만 직접 해석한다.
 * `RC_Event` 테이블의 날짜(year/month/day)와 그날의 글자 칸(body/bodyS/bodySS/continuousBody)만
 * 읽어 한 줄 = 종일 일정 하나로 만든다. 색상·굵게·반복·음력·메모는 사용하지 않아 무시한다.
 * 기간 일정(continuousBody)은 원본에 하루마다 행이 따로 있어 그날의 한 줄로 취급하면 된다.
 */
object ExportType2Importer {
    data class Entry(val year: Int, val month: Int, val day: Int, val title: String)

    class InvalidFile : Exception("invalid redcalendar file")

    private const val TABLE_NAME = "class_RC_Event"
    private val LINE_COLUMNS = listOf("body", "bodyS", "bodySS", "continuousBody")

    fun entries(data: ByteArray): List<Entry> {
        val reader = RealmFileReader(data)
        val topRef = reader.topRef()

        val top = reader.integers(topRef)
        if (top.size < 2) throw InvalidFile()

        val tableNames = reader.strings(top[0])
        val tableRefs = reader.integers(top[1])
        val tableIndex = tableNames.indexOf(TABLE_NAME)
        if (tableIndex < 0 || tableIndex >= tableRefs.size) throw InvalidFile()

        // 테이블: [spec, -, 클러스터 트리, ...] / spec: [타입, 이름, 속성, -, -, 컬럼 키]
        val table = reader.integers(tableRefs[tableIndex])
        if (table.size < 3) throw InvalidFile()

        val spec = reader.integers(table[0])
        if (spec.size < 6) throw InvalidFile()

        val columnNames = reader.strings(spec[1])
        val columnKeys = reader.integers(spec[5])
        if (columnNames.size != columnKeys.size) throw InvalidFile()

        // 컬럼 키 하위 16비트 = 클러스터 안에서의 위치
        val leafIndex = HashMap<String, Int>()
        for ((name, key) in columnNames.zip(columnKeys.toList())) {
            if (name != null) leafIndex[name] = (key and 0xFFFF).toInt()
        }

        val required = listOf("year", "month", "day", "isDeleted") + LINE_COLUMNS
        val column = required.associateWith { leafIndex[it] ?: throw InvalidFile() }

        val entries = mutableListOf<Entry>()
        for (leaf in reader.clusterLeaves(table[2])) {
            entries += readLeaf(leaf, column, reader)
        }
        return entries
    }

    private fun readLeaf(leaf: LongArray, column: Map<String, Int>, reader: RealmFileReader): List<Entry> {
        // 클러스터 리프: [키, 컬럼0, 컬럼1, ...]
        fun ref(name: String): Long {
            val index = column.getValue(name)
            if (index + 1 >= leaf.size) throw InvalidFile()
            return leaf[index + 1]
        }

        val years = reader.integers(ref("year"))
        val months = reader.integers(ref("month"))
        val days = reader.integers(ref("day"))
        val deleted = reader.integers(ref("isDeleted"))
        val lines = LINE_COLUMNS.map { reader.strings(ref(it)) }

        val rowCount = years.size
        if (months.size != rowCount || days.size != rowCount || deleted.size != rowCount || lines.any { it.size != rowCount }) {
            throw InvalidFile()
        }

        val entries = mutableListOf<Entry>()
        for (row in 0 until rowCount) {
            if (deleted[row] != 0L) continue
            for (columnText in lines) {
                val text = columnText[row] ?: continue
                for (line in text.lineSequence()) {
                    val title = line.trim()
                    if (title.isEmpty()) continue
                    entries += Entry(years[row].toInt(), months[row].toInt(), days[row].toInt(), title)
                }
            }
        }
        return entries
    }
}

/**
 * Realm 코어 파일에서 정수 배열·문자열 배열·클러스터 트리만 읽는다.
 * 노드 헤더(8바이트): "AAAA" + 플래그 1바이트 + 원소 수 3바이트(빅엔디언).
 */
private class RealmFileReader(private val bytes: ByteArray) {
    private class NodeHeader(
        val isInner: Boolean,
        val hasRefs: Boolean,
        val context: Boolean,
        /** 0 = 비트 단위 원소, 1 = 바이트 단위 원소, 2 = 바이트 덩어리 */
        val widthType: Int,
        val width: Int,
        val size: Int,
        val start: Int,
    )

    private fun ubyte(index: Int): Int = bytes[index].toInt() and 0xFF

    fun topRef(): Long {
        if (bytes.size < 24 || !bytes.copyOfRange(16, 20).contentEquals("T-DB".toByteArray())) {
            throw ExportType2Importer.InvalidFile()
        }

        val slot = ubyte(23) and 1
        val ref = readInt64(slot * 8)
        if (ref != -1L) return ref

        // 스트리밍 형식(내보내기 복사본 등): 위치가 파일 끝 16바이트 [top ref, 매직 쿠키]에 있다
        if (bytes.size < 40 || readInt64(bytes.size - 8) != 0x3034125237E526C8L) throw ExportType2Importer.InvalidFile()
        return readInt64(bytes.size - 16)
    }

    fun integers(ref: Long): LongArray {
        val header = header(ref)
        if (header.widthType != 0) throw ExportType2Importer.InvalidFile()

        val width = header.width
        val byteCount = (header.size * width + 7) / 8
        if (header.start + byteCount > bytes.size) throw ExportType2Importer.InvalidFile()

        return LongArray(header.size) { index ->
            when (width) {
                0 -> 0L
                1, 2, 4 -> {
                    val bit = index * width
                    val byte = ubyte(header.start + bit / 8)
                    ((byte shr (bit % 8)) and ((1 shl width) - 1)).toLong()
                }
                else -> {
                    val size = width / 8
                    val offset = header.start + index * size
                    var value = 0L
                    for (i in 0 until size) value = value or (ubyte(offset + i).toLong() shl (8 * i))
                    // 부호 확장
                    val shift = 64 - width
                    (value shl shift) shr shift
                }
            }
        }
    }

    /** 문자열 리프 — 짧은 문자열 / 중간(오프셋+덩어리) / 긴 문자열(덩어리 참조 목록) 세 형태 */
    fun strings(ref: Long): List<String?> {
        val header = header(ref)

        if (!header.hasRefs && header.widthType == 1) {
            // 고정 폭 칸, 마지막 바이트 = 남은 패딩 수 (폭과 같으면 null)
            val width = header.width
            if (header.start + header.size * width > bytes.size) throw ExportType2Importer.InvalidFile()

            return List(header.size) { index ->
                if (width == 0) return@List ""
                val offset = header.start + index * width
                val padding = ubyte(offset + width - 1)
                if (padding >= width) null
                else String(bytes, offset, width - 1 - padding, Charsets.UTF_8)
            }
        }

        if (header.hasRefs && !header.context) {
            val children = integers(ref)
            if (children.size < 2) throw ExportType2Importer.InvalidFile()

            // 오프셋은 각 문자열의 끝(종료 0 바이트 포함) 위치
            val offsets = integers(children[0])
            val blob = blobBytes(children[1])
            var previous = 0
            return offsets.map { endLong ->
                val end = endLong.toInt()
                if (end < previous + 1 || end > blob.size) throw ExportType2Importer.InvalidFile()
                val text = String(blob, previous, end - 1 - previous, Charsets.UTF_8)
                previous = end
                text
            }
        }

        if (header.hasRefs && header.context) {
            return integers(ref).map { child ->
                if (child == 0L) return@map null
                val blob = blobBytes(child)
                val length = blob.indexOfFirst { it == 0.toByte() }.let { if (it < 0) blob.size else it }
                String(blob, 0, length, Charsets.UTF_8)
            }
        }

        throw ExportType2Importer.InvalidFile()
    }

    /** 클러스터 트리의 모든 리프. 행이 많으면 내부 노드 아래에 여러 리프로 나뉜다 */
    fun clusterLeaves(ref: Long): List<LongArray> {
        val header = header(ref)
        val values = integers(ref)

        if (!header.isInner) return listOf(values)

        // 내부 노드: [키, 깊이, 자식...]. 태그 값(홀수)은 참조가 아니므로 건너뛴다
        val leaves = mutableListOf<LongArray>()
        for (child in values.drop(2)) {
            if (child != 0L && child % 2 == 0L) leaves += clusterLeaves(child)
        }
        return leaves
    }

    private fun blobBytes(ref: Long): ByteArray {
        val header = header(ref)
        if (header.start + header.size > bytes.size) throw ExportType2Importer.InvalidFile()
        return bytes.copyOfRange(header.start, header.start + header.size)
    }

    private fun header(ref: Long): NodeHeader {
        val start = ref.toInt()
        if (ref <= 0 || ref % 8 != 0L || start + 8 > bytes.size ||
            !bytes.copyOfRange(start, start + 4).contentEquals("AAAA".toByteArray())
        ) {
            throw ExportType2Importer.InvalidFile()
        }

        val flags = ubyte(start + 4)
        val size = (ubyte(start + 5) shl 16) or (ubyte(start + 6) shl 8) or ubyte(start + 7)

        return NodeHeader(
            isInner = flags and 0x80 != 0,
            hasRefs = flags and 0x40 != 0,
            context = flags and 0x20 != 0,
            widthType = (flags shr 3) and 0x3,
            width = (1 shl (flags and 0x7)) shr 1,
            size = size,
            start = start + 8,
        )
    }

    private fun readInt64(offset: Int): Long {
        if (offset + 8 > bytes.size) throw ExportType2Importer.InvalidFile()
        var value = 0L
        for (i in 0 until 8) value = value or (ubyte(offset + i).toLong() shl (8 * i))
        return value
    }
}
