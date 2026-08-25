import Foundation
import StoreKit

/// 이 앱에서의 결제 기록 (로컬 저장 — 서버 없음)
struct PurchaseRecord: Codable, Identifiable {
    enum Kind: String, Codable {
        case tip
        case removeAds

        var label: String {
            switch self {
            case .tip: "후원"
            case .removeAds: "광고 제거"
            }
        }
    }

    var id = UUID()
    let kind: Kind
    let amount: Int
    let date: Date
}

/// 인앱 결제 관리 — 후원(소모성) + 광고 제거(비소모성).
/// ⚠️ 앱 핵심 기능은 결제와 무관하게 무료. 후원은 응원, 광고 제거만 실제 효과.
@MainActor
final class DonationManager: ObservableObject {
    // 후원 (소모성)
    static let tipIDs = [
        "com.hasangwon.planwidget.tip.1000",
        "com.hasangwon.planwidget.tip.5000",
        "com.hasangwon.planwidget.tip.10000",
        "com.hasangwon.planwidget.tip.100000",
    ]

    // 광고 제거 (비소모성)
    static let removeAdsID = "com.hasangwon.planwidget.removeads"

    static let amounts: [String: Int] = [
        "com.hasangwon.planwidget.tip.1000": 1000,
        "com.hasangwon.planwidget.tip.5000": 5000,
        "com.hasangwon.planwidget.tip.10000": 10000,
        "com.hasangwon.planwidget.tip.100000": 100000,
        removeAdsID: 900,
    ]

    @Published private(set) var tipProducts: [Product] = []
    @Published private(set) var removeAdsProduct: Product?
    @Published private(set) var isPurchasing = false
    /// 방금 후원 성공한 금액 (감사 모달 트리거)
    @Published var lastTipAmount: Int?
    /// 광고 제거 구매 여부
    @Published private(set) var adsRemoved: Bool
    @Published private(set) var totalDonated: Int
    /// 이 앱에서의 결제 내역 (로컬 기록, 서버 없음)
    @Published private(set) var history: [PurchaseRecord] = []

    private let totalKey = "donation.total"
    private let historyKey = "purchase.history"
    private let adsRemovedKey = "purchase.adsRemoved"
    private var updatesTask: Task<Void, Never>?

    init() {
        totalDonated = UserDefaults.standard.integer(forKey: totalKey)
        adsRemoved = UserDefaults.standard.bool(forKey: adsRemovedKey)
        history = Self.loadHistory(key: historyKey)
        // 트랜잭션 갱신 감시 (다른 기기 구매·환불 반영)
        updatesTask = Task { [weak self] in
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    await self?.apply(transaction)
                    await transaction.finish()
                }
            }
        }
    }

    deinit { updatesTask?.cancel() }

    func loadProducts() async {
        do {
            let all = try await Product.products(for: Self.tipIDs + [Self.removeAdsID])
            tipProducts = all.filter { Self.tipIDs.contains($0.id) }.sorted { $0.price < $1.price }
            removeAdsProduct = all.first { $0.id == Self.removeAdsID }
        } catch {
            tipProducts = []
            removeAdsProduct = nil
        }
        await refreshEntitlements()
    }

    /// 비소모성(광고 제거) 소유 여부를 실제 App Store 결제 기준으로 확정.
    /// 잘못 저장된 값도 여기서 교정된다 (실제 결제 없으면 false).
    func refreshEntitlements() async {
        var owned = false
        for await entitlement in Transaction.currentEntitlements {
            if case .verified(let transaction) = entitlement,
               transaction.productID == Self.removeAdsID {
                owned = true
            }
        }
        setAdsRemoved(owned)
    }

    func purchaseTip(_ product: Product) async {
        await purchase(product)
    }

    func purchaseRemoveAds() async {
        guard let product = removeAdsProduct else {
            // 상품 미로드(시뮬레이터/미등록) 시에는 아무 것도 하지 않음.
            // 광고 제거는 실제 결제로만 활성화된다.
            return
        }
        await purchase(product)
    }

    /// 구매 복원 (기기 변경 시)
    func restore() async {
        try? await AppStore.sync()
        await refreshEntitlements()
    }

    private func purchase(_ product: Product) async {
        isPurchasing = true
        defer { isPurchasing = false }

        do {
            let result = try await product.purchase()
            if case .success(let verification) = result,
               case .verified(let transaction) = verification {
                await apply(transaction)
                await transaction.finish()
            }
        } catch {
            // 실패 — 조용히 무시
        }
    }

    private func apply(_ transaction: Transaction) async {
        let purchaseDate = transaction.purchaseDate

        if transaction.productID == Self.removeAdsID {
            setAdsRemoved(true)
            addHistory(PurchaseRecord(kind: .removeAds, amount: Self.amounts[Self.removeAdsID] ?? 900, date: purchaseDate))
        } else if let amount = Self.amounts[transaction.productID] {
            totalDonated += amount
            UserDefaults.standard.set(totalDonated, forKey: totalKey)
            lastTipAmount = amount
            addHistory(PurchaseRecord(kind: .tip, amount: amount, date: purchaseDate))
        }
    }

    private func addHistory(_ record: PurchaseRecord) {
        history.insert(record, at: 0)
        Self.saveHistory(history, key: historyKey)
    }

    private static func loadHistory(key: String) -> [PurchaseRecord] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let records = try? JSONDecoder().decode([PurchaseRecord].self, from: data)
        else { return [] }
        return records
    }

    private static func saveHistory(_ records: [PurchaseRecord], key: String) {
        if let data = try? JSONEncoder().encode(records) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    private func setAdsRemoved(_ value: Bool) {
        adsRemoved = value
        UserDefaults.standard.set(value, forKey: adsRemovedKey)
    }
}
