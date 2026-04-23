import SwiftUI

struct MainStatusView: View {
    @EnvironmentObject var authManager: AuthManager
    @ObservedObject private var sseManager = SSEManager.shared

    @State private var mySignal: SignalResponse? = nil
    @State private var isUpdating = false
    @State private var errorMessage: String? = nil

    // 퇴근 예정 입력 상태
    @State private var showDeparturePicker = false
    @State private var departureDateInput = Date()

    private var myStatus: SignalStatus { mySignal?.status ?? .offline }
    private var pairStatus: SignalStatus { sseManager.pairSignal?.status ?? .offline }

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider()
            myStatusSection
            departureSection
            Divider().padding(.horizontal, 12)
            pairStatusSection

            if let msg = errorMessage {
                Text(msg)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 6)
            }

            Divider()
            footerSection
        }
        .task {
            guard let token = authManager.token,
                  let userId = authManager.currentUser?.id else { return }
            await loadSignals()
            SSEManager.shared.start(token: token, userId: userId)
        }
        .onDisappear {
            SSEManager.shared.stop()
        }
    }

    // MARK: - Header

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
                SSEManager.shared.stop()
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

    // MARK: - My Status

    private var myStatusSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("내 상태")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            HStack(spacing: 12) {
                statusBadge(status: myStatus, size: 36)
                Text(myStatus.displayName)
                    .font(.subheadline.weight(.medium))
                Spacer()
                statusChangeMenu
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(myStatus.color.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 12)
        }
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    // MARK: - Departure Time

    private var departureSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("퇴근 예정")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            HStack(spacing: 10) {
                Image(systemName: "clock")
                    .font(.system(size: 14))
                    .foregroundStyle(.purple)

                if let dt = mySignal?.departureTime {
                    Text(dt, style: .time)
                        .font(.subheadline.weight(.medium))
                    Button {
                        Task { await clearDepartureTime() }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.tertiary)
                            .font(.system(size: 14))
                    }
                    .buttonStyle(.plain)
                } else {
                    Text("미설정")
                        .font(.subheadline)
                        .foregroundStyle(.tertiary)
                }

                Spacer()

                Button(showDeparturePicker ? "취소" : "변경") {
                    if !showDeparturePicker {
                        departureDateInput = mySignal?.departureTime ?? Date()
                    }
                    showDeparturePicker.toggle()
                    errorMessage = nil
                }
                .font(.caption.bold())
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(Color.purple.opacity(0.1))
                .foregroundStyle(.purple)
                .clipShape(Capsule())
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.purple.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 12)

            if showDeparturePicker {
                HStack(spacing: 10) {
                    DatePicker("", selection: $departureDateInput, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                        .datePickerStyle(.automatic)
                    Spacer()
                    Button("설정") {
                        showDeparturePicker = false
                        Task { await updateDepartureTime(departureDateInput) }
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 4)
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
    }

    // MARK: - Pair Status

    private var pairStatusSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("상대방 상태")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            HStack(spacing: 12) {
                statusBadge(status: pairStatus, size: 36)
                VStack(alignment: .leading, spacing: 2) {
                    Text(pairStatus.displayName)
                        .font(.subheadline.weight(.medium))
                    if let dt = sseManager.pairSignal?.departureTime {
                        HStack(spacing: 4) {
                            Image(systemName: "clock")
                                .font(.caption2)
                            Text(dt, style: .time)
                                .font(.caption)
                        }
                        .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                if let updatedAt = sseManager.pairSignal?.updatedAt {
                    Text(updatedAt, formatter: relativeDateFormatter)
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(pairStatus.color.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 12)
        }
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    // MARK: - Status Change Menu

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

    // MARK: - Footer

    private var footerSection: some View {
        HStack {
            Image(systemName: "heart.circle.fill")
                .symbolRenderingMode(.multicolor)
                .font(.caption2)
            Text("SideSignal")
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Spacer()
            Circle()
                .fill(sseManager.isConnected ? Color.green : Color.red)
                .frame(width: 6, height: 6)
            Text(sseManager.isConnected ? "연결됨" : "연결 끊김")
                .font(.caption2)
                .foregroundStyle(.tertiary)
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
            mySignal = res.signals.first { $0.userId == userId }
            SSEManager.shared.pairSignal = res.signals.first { $0.userId != userId }
        } catch {
            // pair 없는 경우 등 무시
        }
    }

    private func updateStatus(_ status: SignalStatus) async {
        guard let token = authManager.token else { return }
        isUpdating = true
        errorMessage = nil
        defer { isUpdating = false }
        do {
            let body = SignalUpdateRequest(status: status.rawValue, departureTime: nil, message: nil)
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

    private func updateDepartureTime(_ date: Date) async {
        guard let token = authManager.token else { return }
        isUpdating = true
        errorMessage = nil
        defer { isUpdating = false }
        do {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime]
            formatter.timeZone = TimeZone.current
            let timeString = formatter.string(from: date)
            let body = SignalUpdateRequest(status: nil, departureTime: timeString, message: nil)
            let updated: SignalResponse = try await NetworkManager.shared.request(
                path: "/me/signal",
                method: "PATCH",
                body: body,
                token: token
            )
            mySignal = updated
        } catch {
            errorMessage = "퇴근 시간 설정에 실패했습니다."
        }
    }

    private func clearDepartureTime() async {
        guard let token = authManager.token else { return }
        isUpdating = true
        errorMessage = nil
        defer { isUpdating = false }
        do {
            try await NetworkManager.shared.requestPlain(
                path: "/me/signal/departure-time",
                method: "DELETE",
                token: token
            )
            await loadSignals()
        } catch {
            errorMessage = "퇴근 시간 초기화에 실패했습니다."
        }
    }
}
