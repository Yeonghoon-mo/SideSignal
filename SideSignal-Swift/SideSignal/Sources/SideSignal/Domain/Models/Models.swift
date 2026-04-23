import Foundation

// MARK: - Auth
struct AuthTokenResponse: Codable {
    let tokenType: String
    let accessToken: String
    let expiresIn: Int
    let user: AuthUserResponse
}

struct AuthUserResponse: Codable {
    let id: UUID
    let email: String
    let displayName: String
}

// MARK: - Pair
struct PairResponse: Codable {
    let id: UUID
    let firstUser: AuthUserResponse
    let secondUser: AuthUserResponse
    let createdAt: Date
}

struct PairInviteResponse: Codable {
    let inviteCode: String
    let expiresAt: Date
}

// MARK: - Signal
enum SignalStatus: String, Codable, CaseIterable {
    case focusing = "FOCUSING"
    case inMeeting = "IN_MEETING"
    case coffeeAvailable = "COFFEE_AVAILABLE"
    case lunchAvailable = "LUNCH_AVAILABLE"
    case leavingSoon = "LEAVING_SOON"
    case offline = "OFFLINE"
    
    var displayName: String {
        switch self {
        case .focusing: return "집중 중"
        case .inMeeting: return "회의 중"
        case .coffeeAvailable: return "커피 가능"
        case .lunchAvailable: return "점심 가능"
        case .leavingSoon: return "퇴근 준비"
        case .offline: return "오프라인"
        }
    }
    
    var icon: String {
        switch self {
        case .focusing: return "laptopcomputer"
        case .inMeeting: return "person.2.fill"
        case .coffeeAvailable: return "cup.and.saucer.fill"
        case .lunchAvailable: return "fork.knife"
        case .leavingSoon: return "figure.walk.arrival"
        case .offline: return "power"
        }
    }
}

struct SignalResponse: Codable {
    let id: UUID
    let userId: UUID
    let status: SignalStatus
    let departureTime: Date?
    let message: String?
    let updatedAt: Date
}

struct PairSignalsResponse: Codable {
    let signals: [SignalResponse]
}

// MARK: - SSE Events
struct SignalUpdatedEventPayload: Codable {
    let pairId: UUID
    let senderId: UUID
    let status: String
    let departureTime: String?
    let occurredAt: String
}
