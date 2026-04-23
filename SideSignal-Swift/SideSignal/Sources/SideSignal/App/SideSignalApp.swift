import SwiftUI

@main
struct SideSignalApp: App {
    @StateObject private var authManager = AuthManager.shared
    
    var body: some Scene {
        MenuBarExtra {
            MenuView()
                .environmentObject(authManager)
        } label: {
            HStack {
                Image(systemName: "antenna.radiowaves.left.and.right")
                Text("SideSignal")
            }
        }
        .menuBarExtraStyle(.window)
    }
}

struct MenuView: View {
    @EnvironmentObject var authManager: AuthManager
    
    var body: some View {
        VStack(spacing: 0) {
            if authManager.isAuthenticated {
                MainStatusView()
            } else {
                LoginView()
            }
        }
        .frame(width: 300)
    }
}
