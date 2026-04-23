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
                if screen == .pairing {
                    PairingView(screen: $screen)
                } else {
                    MainStatusView()
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
              let userId = authManager.currentUser?.id else { return }
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
            screen = .main
        }
    }
}
