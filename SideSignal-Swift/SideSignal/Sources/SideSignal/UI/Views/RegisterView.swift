import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var authManager: AuthManager
    @Binding var screen: AppScreen
    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var passwordConfirm = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider().padding(.horizontal, 20)
            formSection
        }
    }

    private var headerSection: some View {
        ZStack {
            HStack {
                Button {
                    withAnimation(.easeInOut(duration: 0.15)) {
                        screen = .login
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("로그인")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.blue)
                }
                .buttonStyle(.plain)
                Spacer()
            }

            Text("회원가입")
                .font(.title3.bold())
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 16)
    }

    private var formSection: some View {
        VStack(spacing: 10) {
            LabeledField(label: "닉네임") {
                TextField("1~40자", text: $displayName)
                    .textFieldStyle(.roundedBorder)
            }

            LabeledField(label: "이메일") {
                TextField("example@company.com", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
            }

            LabeledField(label: "비밀번호") {
                SecureField("12자 이상", text: $password)
                    .textFieldStyle(.roundedBorder)
            }

            LabeledField(label: "비밀번호 확인") {
                SecureField("비밀번호 재입력", text: $passwordConfirm)
                    .textFieldStyle(.roundedBorder)
            }

            if !passwordConfirm.isEmpty && password != passwordConfirm {
                Text("비밀번호가 일치하지 않습니다.")
                    .font(.caption)
                    .foregroundStyle(.orange)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button(action: register) {
                Group {
                    if isLoading {
                        ProgressView().controlSize(.small)
                    } else {
                        Text("회원가입").frame(maxWidth: .infinity)
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || !isFormValid)
            .padding(.top, 4)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
    }

    private var isFormValid: Bool {
        !displayName.isEmpty &&
        !email.isEmpty &&
        password.count >= 12 &&
        password == passwordConfirm
    }

    private func register() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let response: AuthTokenResponse = try await NetworkManager.shared.request(
                    path: "/auth/register",
                    method: "POST",
                    body: ["displayName": displayName, "email": email, "password": password]
                )
                await MainActor.run {
                    authManager.save(token: response.accessToken, user: response.user)
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    if case NetworkError.serverError(let code) = error, code == 409 {
                        errorMessage = "이미 사용 중인 이메일입니다."
                    } else {
                        errorMessage = "회원가입에 실패했습니다. 다시 시도해주세요."
                    }
                    isLoading = false
                }
            }
        }
    }
}

// RegisterView 전용 label + field 래퍼
private struct LabeledField<Content: View>: View {
    let label: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            content
        }
    }
}
