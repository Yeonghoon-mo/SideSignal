import SwiftUI

struct MainStatusView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var mySignal: SignalResponse? = nil
    @State private var pairSignal: SignalResponse? = nil
    @State private var isUpdating = false
    @State private var errorMessage: String? = nil

    private var myStatus: SignalStatus { mySignal?.status ?? .offline }
    private var pairStatus: SignalStatus { pairSignal?.status ?? .offline }

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider()
            statusSection(
                label: "내 상태",
                status: myStatus,
                trailing: statusChangeMenu
            )
            Divider().padding(.horizontal, 12)
            statusSection(
                label: "상대방 상태",
                status: pairStatus,
                trailing: pairUpdatedLabel
            )

            if let msg = errorMessage {
                Text(msg)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 4)
            }

            Divider()
            footerSection
        }
        .task {
            await loadSignals()
        }
    }

    // MARK: - Sections

    private var headerSection: some View {
        HStack(spacing: 10) {
            statusBadge(status: myStatus, size: 30)

            VStack(alignment: .leading, spacing: 1) {
                Text(authManager.currentUser?.displayName ?? "나")
                    .font(.subheadline.bold())
                    .lineLimit(1)
                Text(authManager.currentUser?.email ?? "")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }

            Spacer()

            if isUpdating {
                ProgressView().controlSize(.mini)
            }

            Button {
                authManager.logout()
            } label: {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.plain)
            .help("로그아웃")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private func statusSection<T: View>(label: String, status: SignalStatus, trailing: T) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption.bold())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            HStack(spacing: 12) {
                statusBadge(status: status, size: 36)

                Text(status.displayName)
                    .font(.subheadline.weight(.medium))

                Spacer()

                trailing
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(status.color.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 12)
        }
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    private var statusChangeMenu: some View {
        Menu {
            ForEach(SignalStatus.allCases, id: \.self) { status in
                Button {
                    Task { await updateStatus(status) }
                } label: {
                    Label(status.displayName, systemImage: status.icon)
                }
            }
        } label: {
            Text("변경")
                .font(.caption.bold())
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(.blue.opacity(0.1))
                .foregroundStyle(.blue)
                .clipShape(Capsule())
        }
        .menuStyle(.borderlessButton)
        .fixedSize()
        .disabled(isUpdating)
    }

    @ViewBuilder
    private var pairUpdatedLabel: some View {
        if let updatedAt = pairSignal?.updatedAt {
            Text(updatedAt, formatter: relativeDateFormatter)
                .font(.caption)
                .foregroundStyle(.tertiary)
        } else {
            Text("정보 없음")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
    }

    private var footerSection: some View {
        HStack {
            Image(systemName: "heart.circle.fill")
                .symbolRenderingMode(.multicolor)
                .font(.caption2)
            Text("SideSignal")
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: - Helpers

    private func statusBadge(status: SignalStatus, size: CGFloat) -> some View {
        Circle()
            .fill(status.color.opacity(0.15))
            .frame(width: size, height: size)
            .overlay(
                Image(systemName: status.icon)
                    .font(.system(size: size * 0.42))
                    .foregroundStyle(status.color)
            )
    }

    private var relativeDateFormatter: RelativeDateTimeFormatter {
        let f = RelativeDateTimeFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.unitsStyle = .short
        return f
    }

    // MARK: - API

    private func loadSignals() async {
        guard let token = authManager.token,
              let userId = authManager.currentUser?.id else { return }
        do {
            let res: PairSignalsResponse = try await NetworkManager.shared.request(
                path: "/pairs/current/signals",
                token: token
            )
            mySignal   = res.signals.first { $0.userId == userId }
            pairSignal = res.signals.first { $0.userId != userId }
        } catch {
            // pair 없는 경우 등 무시 — PairingView에서 이미 체크함
        }
    }

    private func updateStatus(_ status: SignalStatus) async {
        guard let token = authManager.token else { return }
        isUpdating = true
        errorMessage = nil
        defer { isUpdating = false }
        do {
            let body = SignalUpdateRequest(
                status: status.rawValue,
                departureTime: nil,
                message: nil
            )
            let updated: SignalResponse = try await NetworkManager.shared.request(
                path: "/me/signal",
                method: "PATCH",
                body: body,
                token: token
            )
            mySignal = updated
        } catch {
            errorMessage = "상태 변경에 실패했습니다."
        }
    }
}
