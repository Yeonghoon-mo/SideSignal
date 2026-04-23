import AppKit
import Foundation
import Network

// @Published 변경은 전부 main thread에서 수행하므로 수동 thread-safety 보장
class SSEManager: ObservableObject, @unchecked Sendable {
    static let shared = SSEManager()

    @Published var pairSignal: SignalResponse? = nil
    @Published var isConnected = false
    private(set) var partnerDisplayName: String? = nil

    private var client: SSEClient?
    private var reconnectTask: Task<Void, Never>?
    private var pathMonitor: NWPathMonitor?
    private var wakeObserver: NSObjectProtocol?

    private var currentToken: String?
    private var currentUserId: UUID?
    private var reconnectDelay: TimeInterval = 2
    private let baseURL = APIEnvironment.baseURL

    private init() {}

    func setPartner(from pair: PairResponse, myUserId: UUID) {
        let partner = pair.firstUser.id == myUserId ? pair.secondUser : pair.firstUser
        partnerDisplayName = partner.displayName
    }

    func start(token: String, userId: UUID) {
        currentToken = token
        currentUserId = userId
        reconnectDelay = 2
        connect()
        startSystemObservers()
        NotificationManager.shared.start()
    }

    func stop() {
        client?.disconnect()
        client = nil
        reconnectTask?.cancel()
        reconnectTask = nil
        stopSystemObservers()
        NotificationManager.shared.stop()
        DispatchQueue.main.async { self.isConnected = false }
    }

    // MARK: - Connection

    private func connect() {
        guard let token = currentToken,
              let url = URL(string: "\(baseURL)/pairs/current/events") else { return }
        client?.disconnect()
        let c = SSEClient(url: url, token: token)
        c.delegate = self
        client = c
        c.connect()
    }

    private func scheduleReconnect() {
        reconnectTask?.cancel()
        let delay = reconnectDelay
        reconnectDelay = min(reconnectDelay * 2, 30)
        reconnectTask = Task {
            try? await Task.sleep(for: .seconds(delay))
            guard !Task.isCancelled else { return }
            DispatchQueue.main.async { self.connect() }
        }
    }

    // MARK: - System Observers

    private func startSystemObservers() {
        // 절전 해제 후 재연결
        wakeObserver = NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.didWakeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.resetAndReconnect()
        }

        // 네트워크 복구 후 재연결
        let monitor = NWPathMonitor()
        monitor.pathUpdateHandler = { [weak self] path in
            guard path.status == .satisfied else { return }
            DispatchQueue.main.async { self?.resetAndReconnect() }
        }
        monitor.start(queue: DispatchQueue.global(qos: .background))
        pathMonitor = monitor
    }

    private func stopSystemObservers() {
        if let obs = wakeObserver {
            NSWorkspace.shared.notificationCenter.removeObserver(obs)
            wakeObserver = nil
        }
        pathMonitor?.cancel()
        pathMonitor = nil
    }

    private func resetAndReconnect() {
        reconnectDelay = 2
        reconnectTask?.cancel()
        connect()
    }

    // MARK: - Helpers

    private func parseISO8601(_ string: String) -> Date? {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = f.date(from: string) { return d }
        f.formatOptions = [.withInternetDateTime]
        return f.date(from: string)
    }
}

// MARK: - SSEClientDelegate

extension SSEManager: SSEClientDelegate {
    func sseClient(_ client: SSEClient, didReceiveEvent event: SSEEvent) {
        switch event {
        case .connect:
            isConnected = true
            reconnectDelay = 2
        case .heartbeat:
            break
        case .signalUpdated(let payload):
            guard let myId = currentUserId, payload.senderId != myId else { return }
            guard let status = SignalStatus(rawValue: payload.status) else { return }
            let departureTime = payload.departureTime.flatMap { parseISO8601($0) }
            let updatedAt = parseISO8601(payload.occurredAt) ?? Date()
            pairSignal = SignalResponse(
                id: pairSignal?.id ?? UUID(),
                userId: payload.senderId,
                status: status,
                departureTime: departureTime,
                message: pairSignal?.message,
                updatedAt: updatedAt
            )
        case .unknown:
            break
        }
    }

    func sseClient(_ client: SSEClient, didDisconnectWithError error: Error?) {
        isConnected = false
        // 정상 취소 → 재연결 불필요
        if let urlErr = error as? URLError, urlErr.code == .cancelled { return }
        // 4xx HTTP 오류 → 인증/페어 문제이므로 재연결해도 동일하게 실패
        if case .httpError(let code)? = error as? SSEConnectionError, (400..<500).contains(code) { return }
        scheduleReconnect()
    }
}
