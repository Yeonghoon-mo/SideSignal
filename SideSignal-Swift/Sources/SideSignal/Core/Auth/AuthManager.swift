import Foundation

class AuthManager: ObservableObject {
    static let shared = AuthManager()
    
    @Published var currentUser: AuthUserResponse?
    @Published var token: String?
    
    private let tokenKey = "com.sidesignal.token"
    private let userKey = "com.sidesignal.user"
    
    var isAuthenticated: Bool {
        return token != nil
    }
    
    private init() {
        load()
    }
    
    func save(token: String, user: AuthUserResponse) {
        self.token = token
        self.currentUser = user
        
        UserDefaults.standard.set(token, forKey: tokenKey)
        if let encodedUser = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(encodedUser, forKey: userKey)
        }
    }
    
    func logout() {
        token = nil
        currentUser = nil
        UserDefaults.standard.removeObject(forKey: tokenKey)
        UserDefaults.standard.removeObject(forKey: userKey)
    }
    
    private func load() {
        token = UserDefaults.standard.string(forKey: tokenKey)
        if let userData = UserDefaults.standard.data(forKey: userKey) {
            currentUser = try? JSONDecoder().decode(AuthUserResponse.self, from: userData)
        }
    }
}
