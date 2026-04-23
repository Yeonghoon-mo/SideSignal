import SwiftUI

enum AppScreen {
    case login
    case register
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
            Image(systemName: "dot.radiowaves.left.and.right")
                .imageScale(.medium)
        }
        .menuBarExtraStyle(.window)
    }
}

struct AppMenuView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var screen: AppScreen = .login

    var body: some View {
        Group {
            if authManager.isAuthenticated {
                MainStatusView()
            } else if screen == .register {
                RegisterView(screen: $screen)
            } else {
                LoginView(screen: $screen)
            }
        }
        .frame(width: 320)
        .background(.ultraThinMaterial)
    }
}
