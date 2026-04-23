import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    @Binding var screen: AppScreen
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider().padding(.horizontal, 20)
            formSection
            footerSection
        }
    }

    private var headerSection: some View {
        VStack(spacing: 6) {
            Image(systemName: "heart.circle.fill")
                .symbolRenderingMode(.multicolor)
                .font(.system(size: 36))
                .padding(.bottom, 2)

            Text("SideSignal")
                .font(.title3.bold())

            Text("조용히 연결되다")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.top, 28)
        .padding(.bottom, 20)
    }

    private var formSection: some View {
        VStack(spacing: 10) {
            TextField("이메일", text: $email)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()

            SecureField("비밀번호", text: $password)
                .textFieldStyle(.roundedBorder)

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button(action: login) {
                Group {
                    if isLoading {
                        ProgressView().controlSize(.small)
                    } else {
                        Text("로그인").frame(maxWidth: .infinity)
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || email.isEmpty || password.isEmpty)
            .padding(.top, 4)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
    }

    private var footerSection: some View {
        HStack(spacing: 4) {
            Text("계정이 없으신가요?")
                .foregroundStyle(.secondary)
            Button("회원가입") {
                withAnimation(.easeInOut(duration: 0.15)) {
                    screen = .register
                }
            }
            .buttonStyle(.link)
        }
        .font(.caption)
        .padding(.bottom, 20)
    }

    private func login() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let response: AuthTokenResponse = try await NetworkManager.shared.request(
                    path: "/auth/login",
                    method: "POST",
                    body: ["email": email, "password": password]
                )
                await MainActor.run {
                    authManager.save(token: response.accessToken, user: response.user)
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = "이메일 또는 비밀번호를 확인해주세요."
                    isLoading = false
                }
            }
        }
    }
}
