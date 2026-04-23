import SwiftUI

struct PairingView: View {
    @EnvironmentObject var authManager: AuthManager
    @Binding var screen: AppScreen

    @State private var tab: PairingTab = .create
    @State private var inviteCode: String? = nil
    @State private var inputCode = ""
    @State private var isLoading = false
    @State private var errorMessage: String? = nil

    enum PairingTab: String, CaseIterable {
        case create = "초대 코드 생성"
        case join   = "초대 코드 입력"
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            Picker("", selection: $tab) {
                ForEach(PairingTab.allCases, id: \.self) { t in
                    Text(t.rawValue).tag(t)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            if tab == .create {
                createTab
            } else {
                joinTab
            }

            if let msg = errorMessage {
                Text(msg)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }

            Divider()
            footer
        }
        .onChange(of: tab) {
            errorMessage = nil
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 10) {
            Image(systemName: "heart.circle.fill")
                .symbolRenderingMode(.multicolor)
                .font(.system(size: 28))
            VStack(alignment: .leading, spacing: 2) {
                Text("페어 연결")
                    .font(.headline)
                Text("함께 사용할 상대방과 연결하세요")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    // MARK: - Create Tab

    private var createTab: some View {
        VStack(spacing: 16) {
            if let code = inviteCode {
                VStack(spacing: 12) {
                    Text("초대 코드")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(code)
                        .font(.system(size: 28, weight: .bold, design: .monospaced))
                        .kerning(4)
                        .foregroundStyle(.primary)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(.quaternary)
                        )
                    Button {
                        NSPasteboard.general.clearContents()
                        NSPasteboard.general.setString(code, forType: .string)
                    } label: {
                        Label("클립보드에 복사", systemImage: "doc.on.doc")
                            .font(.subheadline)
                    }
                    .buttonStyle(.bordered)
                    Button("새 코드 생성") {
                        inviteCode = nil
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .buttonStyle(.plain)
                }
                .padding(.bottom, 8)
            } else {
                VStack(spacing: 8) {
                    Image(systemName: "qrcode")
                        .font(.system(size: 44))
                        .foregroundStyle(.tertiary)
                    Text("초대 코드를 생성해서\n상대방에게 공유하세요")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 8)

                actionButton(title: "초대 코드 생성", loading: isLoading) {
                    await generateCode()
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    // MARK: - Join Tab

    private var joinTab: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text("초대 코드")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                TextField("코드 입력", text: $inputCode)
                    .textFieldStyle(.roundedBorder)
                    .font(.system(.body, design: .monospaced))
                    .multilineTextAlignment(.center)
                    .onChange(of: inputCode) { _, v in
                        inputCode = v.uppercased()
                    }
            }
            actionButton(title: "수락", loading: isLoading, disabled: inputCode.isEmpty) {
                await acceptCode()
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    // MARK: - Footer

    private var footer: some View {
        HStack {
            Button("로그아웃") {
                authManager.logout()
                screen = .login
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .buttonStyle(.plain)
            Spacer()
            Text("SideSignal")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: - Shared

    @ViewBuilder
    private func actionButton(
        title: String,
        loading: Bool,
        disabled: Bool = false,
        action: @escaping () async -> Void
    ) -> some View {
        Button {
            Task { await action() }
        } label: {
            Group {
                if loading {
                    ProgressView().controlSize(.small)
                } else {
                    Text(title)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 32)
        }
        .buttonStyle(.borderedProminent)
        .disabled(loading || disabled)
    }

    // MARK: - API

    private func generateCode() async {
        guard let token = authManager.token else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let res: PairInviteResponse = try await NetworkManager.shared.request(
                path: "/pair-invites",
                method: "POST",
                token: token
            )
            inviteCode = res.inviteCode
        } catch {
            errorMessage = "코드 생성에 실패했습니다."
        }
    }

    private func acceptCode() async {
        guard let token = authManager.token, !inputCode.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let _: PairResponse = try await NetworkManager.shared.request(
                path: "/pair-invites/\(inputCode)/accept",
                method: "POST",
                token: token
            )
            screen = .main
        } catch NetworkError.serverError(404) {
            errorMessage = "유효하지 않거나 만료된 코드입니다."
        } catch NetworkError.serverError(409) {
            errorMessage = "이미 페어가 연결되어 있습니다."
        } catch {
            errorMessage = "코드 수락에 실패했습니다."
        }
    }
}
