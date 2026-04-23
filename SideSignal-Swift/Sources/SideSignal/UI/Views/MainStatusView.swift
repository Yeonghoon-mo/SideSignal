import SwiftUI

struct MainStatusView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var myStatus: SignalStatus = .offline
    @State private var pairStatus: SignalStatus = .offline

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider()
            statusSection(
                label: "내 상태",
                status: myStatus,
                trailing: statusChangeMenu
            )
            Divider().padding(.horizontal, 12)
            statusSection(
                label: "상대방 상태",
                status: pairStatus,
                trailing: Text("방금 전")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            )
            Divider()
            footerSection
        }
    }

    // MARK: - Sections

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

            Button {
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

    private func statusSection<T: View>(label: String, status: SignalStatus, trailing: T) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption.bold())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            HStack(spacing: 12) {
                statusBadge(status: status, size: 36)

                Text(status.displayName)
                    .font(.subheadline.weight(.medium))

                Spacer()

                trailing
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(status.color.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 12)
        }
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    private var statusChangeMenu: some View {
        Menu {
            ForEach(SignalStatus.allCases, id: \.self) { status in
                Button {
                    myStatus = status
                    // TODO: Update API
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
    }

    private var footerSection: some View {
        HStack {
            Image(systemName: "dot.radiowaves.left.and.right")
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Text("SideSignal")
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Spacer()
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
}
