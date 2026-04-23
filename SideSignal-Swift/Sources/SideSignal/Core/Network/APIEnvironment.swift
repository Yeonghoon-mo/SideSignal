import Foundation

// API 서버 주소 환경 설정
enum APIEnvironment {
    private static let environmentKey = "SIDESIGNAL_API_BASE_URL"
    private static let fallbackBaseURL = "http://localhost:8080/api/v1"

    static var baseURL: String {
        let configuredURL = ProcessInfo.processInfo.environment[environmentKey]?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let configuredURL, !configuredURL.isEmpty else {
            return fallbackBaseURL
        }

        guard configuredURL.hasSuffix("/") else {
            return configuredURL
        }

        return String(configuredURL.dropLast())
    }
}
