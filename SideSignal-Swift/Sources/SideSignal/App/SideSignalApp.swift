import SwiftUI

enum AppScreen {
    case login
    case register
    case pairing
    case main
}

@main
struct SideSignalApp: App {
    @StateObject private var authManager = AuthManager.shared
    @ObservedObject private var sseManager = SSEManager.shared

    var body: some Scene {
        MenuBarExtra {
            AppMenuView()
                .environmentObject(authManager)
        } label: {
            Image(systemName: "heart.circle.fill")
                .symbolRenderingMode(.multicolor)
                .imageScale(.medium)
        }
        .menuBarExtraStyle(.window)
    }
}

struct AppMenuView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var screen: AppScreen = .login
    @State private var pairCheckDone = false

    var body: some View {
        Group {
            if authManager.isAuthenticated {
                switch screen {
                case .main:
                    MainStatusView()
                case .pairing:
                    PairingView(screen: $screen)
                default:
                    // 페어 상태 확인 완료 대기
                    ProgressView()
                        .frame(height: 80)
                }
            } else if screen == .register {
                RegisterView(screen: $screen)
            } else {
                LoginView(screen: $screen)
            }
        }
        .frame(width: 320)
        .background(.ultraThinMaterial)
        .fontDesign(.rounded)
        .task(id: authManager.isAuthenticated) {
            guard authManager.isAuthenticated, !pairCheckDone else { return }
            pairCheckDone = true
            await checkPairStatus()
        }
        .onChange(of: authManager.isAuthenticated) { _, isAuth in
            if !isAuth {
                pairCheckDone = false
                screen = .login
            }
        }
    }

    private func checkPairStatus() async {
        guard let token = authManager.token,
              let userId = authManager.currentUser?.id else {
            screen = .pairing
            return
        }
        do {
            let pair: PairResponse = try await NetworkManager.shared.request(
                path: "/pairs/current",
                token: token
            )
            SSEManager.shared.setPartner(from: pair, myUserId: userId)
            screen = .main
        } catch NetworkError.serverError(404) {
            screen = .pairing
        } catch {
            // 네트워크 오류 등 — pair 확인 불가 상태에서 SSE 연결 시도 방지
            screen = .pairing
        }
    }
}
