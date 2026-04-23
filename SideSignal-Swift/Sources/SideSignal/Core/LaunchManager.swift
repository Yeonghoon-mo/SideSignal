import Foundation
import ServiceManagement
import OSLog

class LaunchManager: ObservableObject {
    static let shared = LaunchManager()
    private let logger = Logger(subsystem: "com.sidesignal.SideSignal", category: "LaunchManager")

    @Published var isLaunchAtLoginEnabled: Bool = false {
        didSet {
            updateLaunchService()
        }
    }

    private let service = SMAppService.mainApp

    private init() {
        checkStatus()
    }

    func checkStatus() {
        // 현재 로그인 항목 등록 상태 확인
        isLaunchAtLoginEnabled = (service.status == .enabled)
    }

    private func updateLaunchService() {
        do {
            if isLaunchAtLoginEnabled {
                if service.status != .enabled {
                    try service.register()
                    logger.info("로그인 시 자동 실행 등록 성공")
                }
            } else {
                if service.status == .enabled {
                    try service.unregister()
                    logger.info("로그인 시 자동 실행 해제 성공")
                }
            }
        } catch {
            logger.error("자동 실행 설정 변경 실패: \(error.localizedDescription)")
            // 실패 후 UI 상태 재동기화
            checkStatus()
        }
    }
}
