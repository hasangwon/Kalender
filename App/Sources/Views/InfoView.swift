import StoreKit
import SwiftUI

/// 정보 화면 — 광고 제거(위) + 후원(아래) + 앱 정보.
struct InfoView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var donation: DonationManager

    @State private var showThanks = false
    @State private var thanksAmount = 0

    private let fallbackTips = [1000, 5000, 10000, 100000]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    removeAdsCard
                    donationCard
                    if !donation.history.isEmpty {
                        historyCard
                    }
                    appInfoCard
                }
                .padding(16)
            }
            .background(AppTheme.background)
            .fontDesign(.rounded)
            .navigationTitle("정보")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.footnote.weight(.bold))
                            .foregroundStyle(.secondary)
                            .frame(width: 30, height: 30)
                            .background(Color.primary.opacity(0.05), in: Circle())
                    }
                }
            }
            .toolbarBackground(AppTheme.background, for: .navigationBar)
            .task { await donation.loadProducts() }
            .onChange(of: donation.lastTipAmount) { _, newValue in
                if let amount = newValue {
                    thanksAmount = amount
                    showThanks = true
                    donation.lastTipAmount = nil
                }
            }
            .overlay {
                if showThanks {
                    ThankYouView(amount: thanksAmount) { showThanks = false }
                }
            }
        }
    }

    // MARK: - 광고 제거

    private var removeAdsCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Text(donation.adsRemoved ? "✅" : "🚫").font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text("광고 제거")
                        .font(.subheadline.weight(.bold))
                    Text(donation.adsRemoved
                        ? "광고가 제거되었습니다. 감사합니다!"
                        : "하단 배너 광고를 영구히 제거해요")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }

            if donation.adsRemoved {
                Text("구매 완료")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.primary.opacity(0.06), in: RoundedRectangle(cornerRadius: 14))
            } else {
                Button {
                    Task { await donation.purchaseRemoveAds() }
                } label: {
                    Text(removeAdsPriceText)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                }
                .disabled(donation.isPurchasing)
            }

            Button {
                Task { await donation.restore() }
            } label: {
                Text("구매 복원")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private var removeAdsPriceText: String {
        if let product = donation.removeAdsProduct {
            return "\(product.displayPrice)에 광고 제거"
        }
        return "900원에 광고 제거"
    }

    // MARK: - 후원

    private var donationCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Text("☕️").font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text("개발자 후원하기")
                        .font(.subheadline.weight(.bold))
                    Text("하상원을 응원해 주세요")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }

            if donation.tipProducts.isEmpty {
                ForEach(fallbackTips, id: \.self) { amount in
                    tipButton(title: "\(amount.formatted())원", amount: amount, product: nil)
                }
            } else {
                ForEach(donation.tipProducts, id: \.id) { product in
                    tipButton(
                        title: product.displayPrice,
                        amount: DonationManager.amounts[product.id] ?? 0,
                        product: product
                    )
                }
            }

            if donation.totalDonated > 0 {
                Text("지금까지 \(donation.totalDonated.formatted())원 후원해 주셨어요 💛")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Text("모든 기능은 후원과 상관없이 무료로 사용할 수 있어요.")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private func tipButton(title: String, amount: Int, product: Product?) -> some View {
        Button {
            Task {
                if let product {
                    await donation.purchaseTip(product)
                } else {
                    #if DEBUG
                    // 시뮬레이터 데모 (릴리스 빌드 제외)
                    thanksAmount = amount
                    showThanks = true
                    #endif
                }
            }
        } label: {
            HStack {
                Text(tipLabel(for: amount))
                    .font(.subheadline.weight(.bold))
                Spacer()
                Text(title)
                    .font(.subheadline.weight(.heavy))
            }
            .foregroundStyle(.white)
            .padding(.vertical, 14)
            .padding(.horizontal, 16)
            .background(AppTheme.primary.opacity(0.92), in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(donation.isPurchasing)
    }

    private func tipLabel(for amount: Int) -> String {
        switch amount {
        case ..<5000: "🥉 커피 한 모금"
        case ..<10000: "🥈 커피 한 잔"
        case ..<100000: "🥇 든든한 한 끼"
        default: "👑 전설의 후원자"
        }
    }

    // MARK: - 결제 내역 (로컬 기록, 서버 없음)

    private var historyCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("결제 내역")
                .font(.system(.headline, design: .rounded).weight(.bold))

            ForEach(donation.history) { record in
                HStack {
                    Image(systemName: record.kind == .removeAds ? "nosign" : "cup.and.saucer.fill")
                        .font(.caption)
                        .foregroundStyle(AppTheme.primary)
                    Text(record.kind.label)
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    VStack(alignment: .trailing, spacing: 1) {
                        Text("\(record.amount.formatted())원")
                            .font(.subheadline.weight(.bold))
                        Text(record.date.formatted(.dateTime.year().month().day().locale(Locale(identifier: "ko_KR"))))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Text("이 앱에서의 결제 기록입니다. 전체 구입 내역은 아이폰 설정 → 미디어 및 구입에서 확인할 수 있어요.")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    // MARK: - 앱 정보

    private var appInfoCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("앱 정보")
                .font(.system(.headline, design: .rounded).weight(.bold))

            infoRow("앱 이름", value: "Kalender")
            infoRow("버전", value: "1.0.0")
        }
        .padding(18)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 10, y: 3)
    }

    private func infoRow(_ title: String, value: String) -> some View {
        HStack {
            Text(title).font(.subheadline).foregroundStyle(.secondary)
            Spacer()
            Text(value).font(.subheadline.weight(.semibold))
        }
    }
}

/// 후원 감사 모달 — 금액 클수록 더 감사하고 비굴하게
struct ThankYouView: View {
    let amount: Int
    let onDismiss: () -> Void

    @State private var appeared = false

    private var emoji: String {
        switch amount {
        case ..<5000: "🙏"
        case ..<10000: "🥹"
        case ..<100000: "😭"
        default: "🙇‍♂️"
        }
    }

    private var title: String {
        switch amount {
        case ..<5000: "감사히 잘 쓰겠습니다"
        case ..<10000: "정말 감사합니다!"
        case ..<100000: "이 은혜 잊지 않겠습니다"
        default: "충성을 다하겠습니다"
        }
    }

    private var message: String {
        switch amount {
        case ..<5000:
            return "따뜻한 응원 감사합니다.\n더 좋은 앱으로 보답할게요 🙏"
        case ..<10000:
            return "덕분에 커피 한 잔 하겠습니다.\n힘내서 개발하겠습니다! ☕️"
        case ..<100000:
            return "이렇게까지 응원해 주시다니…\n평생 이 앱 책임지고 관리하겠습니다.\n진심으로 감사드립니다 🥹"
        default:
            return "가문의 영광입니다, 후원자님.\n남은 개발 인생을 바쳐\n최고의 앱으로 모시겠습니다.\n부디 만수무강하시옵소서 🙇‍♂️👑"
        }
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea().onTapGesture { onDismiss() }

            VStack(spacing: 16) {
                Text(emoji).font(.system(size: 60)).scaleEffect(appeared ? 1 : 0.4)
                Text(title)
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundStyle(AppTheme.primary)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.primary)

                Button {
                    onDismiss()
                } label: {
                    Text("닫기")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(.top, 4)
            }
            .padding(28)
            .frame(maxWidth: 330)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 24))
            .scaleEffect(appeared ? 1 : 0.85)
            .opacity(appeared ? 1 : 0)
            .padding(32)
        }
        .fontDesign(.rounded)
        .onAppear {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.65)) { appeared = true }
        }
    }
}
