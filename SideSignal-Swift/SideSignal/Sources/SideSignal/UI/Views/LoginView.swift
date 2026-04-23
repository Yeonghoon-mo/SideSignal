import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .font(.system(size: 40))
                .foregroundColor(.blue)
            
            Text("SideSignal 시작하기")
                .font(.headline)
            
            VStack(spacing: 12) {
                TextField("이메일", text: $email)
                    .textFieldStyle(.roundedBorder)
                
                SecureField("비밀번호", text: $password)
                    .textFieldStyle(.roundedBorder)
            }
            
            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }
            
            Button {
                login()
            } label: {
                if isLoading {
                    ProgressView()
                        .controlSize(.small)
                } else {
                    Text("로그인")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading || email.isEmpty || password.isEmpty)
        }
        .padding(30)
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
                    errorMessage = "로그인 실패: \(error.localizedDescription)"
                    isLoading = false
                }
            }
        }
    }
}
