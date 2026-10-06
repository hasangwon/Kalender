import Foundation

/// 빨간달력 내보내기 파일(.redcalendar) 읽기.
///
/// 파일은 Realm DB(파일 형식 24) 그대로라서, 라이브러리 없이 필요한 부분만 직접 해석한다.
/// `RC_Event` 테이블의 날짜(year/month/day)와 그날의 글자 칸(body/bodyS/bodySS/continuousBody)만
/// 읽어 한 줄 = 종일 일정 하나로 만든다. 색상·굵게·반복·음력·메모는 사용하지 않아 무시한다.
/// 기간 일정(continuousBody)은 원본에 하루마다 행이 따로 있어 그날의 한 줄로 취급하면 된다.
enum RedCalendarImporter {
    struct Entry: Hashable {
        let year: Int
        let month: Int
        let day: Int
        let title: String
    }

    enum ImportError: Error {
        case invalidFile
    }

    private static let tableName = "class_RC_Event"
    private static let lineColumns = ["body", "bodyS", "bodySS", "continuousBody"]

    static func entries(from data: Data) throws -> [Entry] {
        let reader = RealmFileReader(bytes: [UInt8](data))
        let topRef = try reader.topRef()

        let top = try reader.integers(at: topRef)
        guard top.count >= 2 else { throw ImportError.invalidFile }

        let tableNames = try reader.strings(at: top[0])
        let tableRefs = try reader.integers(at: top[1])
        guard let tableIndex = tableNames.firstIndex(of: tableName), tableIndex < tableRefs.count
        else { throw ImportError.invalidFile }

        // 테이블: [spec, -, 클러스터 트리, ...] / spec: [타입, 이름, 속성, -, -, 컬럼 키]
        let table = try reader.integers(at: tableRefs[tableIndex])
        guard table.count >= 3 else { throw ImportError.invalidFile }

        let spec = try reader.integers(at: table[0])
        guard spec.count >= 6 else { throw ImportError.invalidFile }

        let columnNames = try reader.strings(at: spec[1])
        let columnKeys = try reader.integers(at: spec[5])
        guard columnNames.count == columnKeys.count else { throw ImportError.invalidFile }

        // 컬럼 키 하위 16비트 = 클러스터 안에서의 위치
        var leafIndex: [String: Int] = [:]
        for (name, key) in zip(columnNames, columnKeys) {
            if let name { leafIndex[name] = Int(key & 0xFFFF) }
        }

        let required = ["year", "month", "day", "isDeleted"] + lineColumns
        let columns = try required.map { name -> Int in
            guard let index = leafIndex[name] else { throw ImportError.invalidFile }
            return index
        }
        let column = Dictionary(uniqueKeysWithValues: zip(required, columns))

        var entries: [Entry] = []
        for leaf in try reader.clusterLeaves(at: table[2]) {
            entries += try readLeaf(leaf, column: column, reader: reader)
        }
        return entries
    }

    private static func readLeaf(
        _ leaf: [Int64], column: [String: Int], reader: RealmFileReader
    ) throws -> [Entry] {
        // 클러스터 리프: [키, 컬럼0, 컬럼1, ...]
        func ref(_ name: String) throws -> Int64 {
            guard let index = column[name], index + 1 < leaf.count else { throw ImportError.invalidFile }
            return leaf[index + 1]
        }

        let years = try reader.integers(at: try ref("year"))
        let months = try reader.integers(at: try ref("month"))
        let days = try reader.integers(at: try ref("day"))
        let deleted = try reader.integers(at: try ref("isDeleted"))
        let lines = try lineColumns.map { try reader.strings(at: try ref($0)) }

        let rowCount = years.count
        guard months.count == rowCount, days.count == rowCount, deleted.count == rowCount,
              lines.allSatisfy({ $0.count == rowCount })
        else { throw ImportError.invalidFile }

        var entries: [Entry] = []
        for row in 0..<rowCount where deleted[row] == 0 {
            for column in lines {
                guard let text = column[row] else { continue }

                for line in text.split(whereSeparator: \.isNewline) {
                    let title = line.trimmingCharacters(in: .whitespaces)
                    guard !title.isEmpty else { continue }

                    entries.append(Entry(
                        year: Int(years[row]), month: Int(months[row]), day: Int(days[row]), title: title
                    ))
                }
            }
        }
        return entries
    }
}

// MARK: - Realm 파일 최소 해석기

/// Realm 코어 파일에서 정수 배열·문자열 배열·클러스터 트리만 읽는다.
/// 노드 헤더(8바이트): "AAAA" + 플래그 1바이트 + 원소 수 3바이트(빅엔디언).
private struct RealmFileReader {
    let bytes: [UInt8]

    private struct NodeHeader {
        let isInner: Bool
        let hasRefs: Bool
        let context: Bool
        /// 0 = 비트 단위 원소, 1 = 바이트 단위 원소, 2 = 바이트 덩어리
        let widthType: Int
        let width: Int
        let size: Int
        let start: Int
    }

    func topRef() throws -> Int64 {
        guard bytes.count >= 24, Array(bytes[16..<20]) == Array("T-DB".utf8)
        else { throw RedCalendarImporter.ImportError.invalidFile }

        let slot = Int(bytes[23] & 1)
        let ref = try readUInt64(at: slot * 8)
        guard ref == -1 else { return ref }

        // 스트리밍 형식(내보내기 복사본 등): 위치가 파일 끝 16바이트 [top ref, 매직 쿠키]에 있다
        guard bytes.count >= 40, try readUInt64(at: bytes.count - 8) == Int64(bitPattern: 0x3034_1252_37E5_26C8)
        else { throw RedCalendarImporter.ImportError.invalidFile }

        return try readUInt64(at: bytes.count - 16)
    }

    func integers(at ref: Int64) throws -> [Int64] {
        let header = try header(at: ref)
        guard header.widthType == 0 else { throw RedCalendarImporter.ImportError.invalidFile }

        let width = header.width
        let byteCount = (header.size * width + 7) / 8
        guard header.start + byteCount <= bytes.count else { throw RedCalendarImporter.ImportError.invalidFile }

        return (0..<header.size).map { index in
            switch width {
            case 0:
                return 0
            case 1, 2, 4:
                let bit = index * width
                let byte = bytes[header.start + bit / 8]
                return Int64((byte >> (bit % 8)) & UInt8((1 << width) - 1))
            default:
                let size = width / 8
                let offset = header.start + index * size
                var value: UInt64 = 0
                for i in 0..<size {
                    value |= UInt64(bytes[offset + i]) << (8 * i)
                }
                // 부호 확장
                let shift = UInt64(64 - width)
                return Int64(bitPattern: value << shift) >> Int64(shift)
            }
        }
    }

    /// 문자열 리프 — 짧은 문자열 / 중간(오프셋+덩어리) / 긴 문자열(덩어리 참조 목록) 세 형태
    func strings(at ref: Int64) throws -> [String?] {
        let header = try header(at: ref)

        if !header.hasRefs && header.widthType == 1 {
            // 고정 폭 칸, 마지막 바이트 = 남은 패딩 수 (폭과 같으면 null)
            let width = header.width
            guard header.start + header.size * width <= bytes.count
            else { throw RedCalendarImporter.ImportError.invalidFile }

            return (0..<header.size).map { index in
                guard width > 0 else { return "" }

                let offset = header.start + index * width
                let padding = Int(bytes[offset + width - 1])
                guard padding < width else { return nil }

                return String(decoding: bytes[offset..<(offset + width - 1 - padding)], as: UTF8.self)
            }
        }

        if header.hasRefs && !header.context {
            let children = try integers(at: ref)
            guard children.count >= 2 else { throw RedCalendarImporter.ImportError.invalidFile }

            // 오프셋은 각 문자열의 끝(종료 0 바이트 포함) 위치
            let offsets = try integers(at: children[0])
            let blob = try blobBytes(at: children[1])
            var previous = 0
            return try offsets.map { end in
                let end = Int(end)
                guard end >= previous + 1, end <= blob.count
                else { throw RedCalendarImporter.ImportError.invalidFile }

                defer { previous = end }
                return String(decoding: blob[previous..<(end - 1)], as: UTF8.self)
            }
        }

        if header.hasRefs && header.context {
            return try integers(at: ref).map { child in
                guard child != 0 else { return nil }

                let blob = try blobBytes(at: child)
                let trimmed = blob.prefix { $0 != 0 }
                return String(decoding: trimmed, as: UTF8.self)
            }
        }

        throw RedCalendarImporter.ImportError.invalidFile
    }

    /// 클러스터 트리의 모든 리프. 행이 많으면 내부 노드 아래에 여러 리프로 나뉜다.
    func clusterLeaves(at ref: Int64) throws -> [[Int64]] {
        let header = try header(at: ref)
        let values = try integers(at: ref)

        guard header.isInner else { return [values] }

        // 내부 노드: [키, 깊이, 자식...]. 태그 값(홀수)은 참조가 아니므로 건너뛴다.
        var leaves: [[Int64]] = []
        for child in values.dropFirst(2) where child != 0 && child % 2 == 0 {
            leaves += try clusterLeaves(at: child)
        }
        return leaves
    }

    private func blobBytes(at ref: Int64) throws -> [UInt8] {
        let header = try header(at: ref)
        guard header.start + header.size <= bytes.count
        else { throw RedCalendarImporter.ImportError.invalidFile }

        return Array(bytes[header.start..<(header.start + header.size)])
    }

    private func header(at ref: Int64) throws -> NodeHeader {
        let start = Int(ref)
        guard ref > 0, ref % 8 == 0, start + 8 <= bytes.count,
              bytes[start..<(start + 4)].elementsEqual("AAAA".utf8)
        else { throw RedCalendarImporter.ImportError.invalidFile }

        let flags = bytes[start + 4]
        let size = Int(bytes[start + 5]) << 16 | Int(bytes[start + 6]) << 8 | Int(bytes[start + 7])

        return NodeHeader(
            isInner: flags & 0x80 != 0,
            hasRefs: flags & 0x40 != 0,
            context: flags & 0x20 != 0,
            widthType: Int((flags >> 3) & 0x3),
            width: (1 << Int(flags & 0x7)) >> 1,
            size: size,
            start: start + 8
        )
    }

    private func readUInt64(at offset: Int) throws -> Int64 {
        guard offset + 8 <= bytes.count else { throw RedCalendarImporter.ImportError.invalidFile }

        var value: UInt64 = 0
        for i in 0..<8 {
            value |= UInt64(bytes[offset + i]) << (8 * i)
        }
        return Int64(bitPattern: value)
    }
}
