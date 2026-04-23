import Combine
import os
import UserNotifications

class NotificationManager: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationManager()

    private var center: UNUserNotificationCenter { .current() }
    private var cancellables = Set<AnyCancellable>()
    private let logger = Logger(subsystem: "com.sidesignal.SideSignal", category: "NotificationManager")

    // 퇴근 시간 알림 식별자
    private let idSoon = "pair.departure.soon"
    private let idArrived = "pair.departure.arrived"

    private override init() {
        super.init()
        center.delegate = self
    }

    func start() {
        center.delegate = self
        requestPermission()

        // SSEManager.pairSignal 변경 시 자동 재스케줄
        SSEManager.shared.$pairSignal
            .map { $0?.departureTime }
            .removeDuplicates()
            .sink { [weak self] departureTime in
                guard let self else { return }
                self.cancelAll()
                if let dt = departureTime {
                    self.scheduleNotifications(for: dt)
                }
            }
            .store(in: &cancellables)
    }

    func stop() {
        cancellables.removeAll()
        cancelAll()
    }

    // 콕 찌르기 즉시 알림
    func notifyPoke(from senderDisplayName: String) {
        guard senderDisplayName.isEmpty == false else {
            logger.error("poke_notification_skipped_empty_sender")
            return
        }

        let content = UNMutableNotificationContent()
        content.title = "콕 찌르기"
        content.body = "\(senderDisplayName)님이 콕 찌르셨어요 !"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "poke.received.\(UUID().uuidString)",
            content: content,
            trigger: nil
        )

        addNotificationRequest(request, logKey: "poke_notification")
    }

    // foreground 상태 알림 표시
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    // MARK: - Permission

    private func requestPermission() {
        center.requestAuthorization(options: [.alert, .sound]) { [weak self] granted, error in
            guard let self else { return }

            if let error {
                self.logger.error("notification_permission_failed error=\(error.localizedDescription, privacy: .public)")
                return
            }

            if granted {
                self.logger.info("notification_permission_granted")
            } else {
                self.logger.error("notification_permission_denied")
            }
        }
    }

    // MARK: - Schedule

    private func scheduleNotifications(for departureTime: Date) {
        let now = Date()
        let name = SSEManager.shared.partnerDisplayName ?? "파트너"

        // 10분 전 예고 알림
        let tenMinBefore = departureTime.addingTimeInterval(-10 * 60)
        if tenMinBefore > now {
            addNotification(
                id: idSoon,
                at: tenMinBefore,
                title: "\(name) 퇴근 예정",
                body: "10분 후 \(name)이(가) 퇴근할 예정이에요."
            )
        }

        // 퇴근 시간 도달 알림
        if departureTime > now {
            addNotification(
                id: idArrived,
                at: departureTime,
                title: "\(name) 퇴근 시간",
                body: "\(name)의 퇴근 예정 시간이 됐어요."
            )
        }
    }

    private func addNotification(id: String, at date: Date, title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let comps = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute],
            from: date
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)

        addNotificationRequest(request, logKey: id)
    }

    private func addNotificationRequest(_ request: UNNotificationRequest, logKey: String) {
        center.add(request) { [weak self] error in
            guard let self else { return }

            if let error {
                self.logger.error("notification_add_failed key=\(logKey, privacy: .public), error=\(error.localizedDescription, privacy: .public)")
                return
            }

            self.logger.info("notification_add_succeeded key=\(logKey, privacy: .public)")
        }
    }

    private func cancelAll() {
        center.removePendingNotificationRequests(withIdentifiers: [idSoon, idArrived])
    }
}
