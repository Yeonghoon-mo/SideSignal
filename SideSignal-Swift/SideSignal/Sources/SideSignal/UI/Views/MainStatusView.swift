import SwiftUI

struct MainStatusView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var myStatus: SignalStatus = .offline
    @State private var pairStatus: SignalStatus = .offline
    
    var body: some View {
        VStack(spacing: 16) {
            // My Status Section
            VStack(alignment: .leading, spacing: 8) {
                Text("내 상태")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    Image(systemName: myStatus.icon)
                        .foregroundColor(.blue)
                    Text(myStatus.displayName)
                        .font(.headline)
                    Spacer()
                    
                    Menu {
                        ForEach(SignalStatus.allCases, id: \.self) { status in
                            Button(status.displayName) {
                                myStatus = status
                                // TODO: Update API
                            }
                        }
                    } label: {
                        Text("변경")
                    }
                    .fixedSize()
                }
                .padding()
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(8)
            }
            
            Divider()
            
            // Pair Status Section
            VStack(alignment: .leading, spacing: 8) {
                Text("상대방 상태")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    Image(systemName: pairStatus.icon)
                        .foregroundColor(.green)
                    Text(pairStatus.displayName)
                        .font(.headline)
                    Spacer()
                    Text("1분 전")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(8)
            }
            
            Button("로그아웃") {
                authManager.logout()
            }
            .buttonStyle(.link)
            .padding(.top, 8)
        }
        .padding()
    }
}
