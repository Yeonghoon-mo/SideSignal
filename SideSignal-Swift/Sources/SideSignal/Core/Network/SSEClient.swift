import Foundation

enum SSEEvent {
    case connect
    case heartbeat
    case signalUpdated(SignalUpdatedEventPayload)
    case unknown(String, String)
}

// 재연결하면 안 되는 HTTP 오류 (4xx)
enum SSEConnectionError: Error {
    case httpError(Int)
}

protocol SSEClientDelegate: AnyObject {
    func sseClient(_ client: SSEClient, didReceiveEvent event: SSEEvent)
    func sseClient(_ client: SSEClient, didDisconnectWithError error: Error?)
}

class SSEClient: NSObject, URLSessionDataDelegate {
    private var session: URLSession?
    private var task: URLSessionDataTask?
    private let url: URL
    private let token: String

    weak var delegate: SSEClientDelegate?

    init(url: URL, token: String) {
        self.url = url
        self.token = token
        super.init()
    }

    func connect() {
        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("text/event-stream, application/json;q=0.9, */*;q=0.8", forHTTPHeaderField: "Accept")
        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
        request.timeoutInterval = 3600

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 3600
        config.timeoutIntervalForResource = 3600

        session = URLSession(configuration: config, delegate: self, delegateQueue: .main)
        task = session?.dataTask(with: request)
        task?.resume()
    }

    func disconnect() {
        task?.cancel()
        session?.invalidateAndCancel()
    }

    // MARK: - URLSessionDataDelegate

    // HTTP 상태 코드 확인: 4xx/5xx면 즉시 취소
    func urlSession(
        _ session: URLSession,
        dataTask: URLSessionDataTask,
        didReceive response: URLResponse,
        completionHandler: @escaping (URLSession.ResponseDisposition) -> Void
    ) {
        guard let http = response as? HTTPURLResponse else {
            completionHandler(.allow)
            return
        }
        if (200...299).contains(http.statusCode) {
            completionHandler(.allow)
        } else {
            completionHandler(.cancel)
            delegate?.sseClient(self, didDisconnectWithError: SSEConnectionError.httpError(http.statusCode))
        }
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        guard let string = String(data: data, encoding: .utf8) else { return }

        let lines = string.components(separatedBy: .newlines)
        var currentEvent: String?
        var currentData: String?

        for line in lines {
            if line.isEmpty {
                if let eventName = currentEvent, let eventData = currentData {
                    parseEvent(name: eventName, data: eventData)
                }
                currentEvent = nil
                currentData = nil
            } else if line.hasPrefix("event:") {
                currentEvent = line.replacingOccurrences(of: "event:", with: "").trimmingCharacters(in: .whitespaces)
            } else if line.hasPrefix("data:") {
                let dataValue = line.replacingOccurrences(of: "data:", with: "").trimmingCharacters(in: .whitespaces)
                if currentData == nil {
                    currentData = dataValue
                } else {
                    currentData?.append(dataValue)
                }
            }
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        delegate?.sseClient(self, didDisconnectWithError: error)
    }

    private func parseEvent(name: String, data: String) {
        switch name {
        case "connect":
            delegate?.sseClient(self, didReceiveEvent: .connect)
        case "heartbeat":
            delegate?.sseClient(self, didReceiveEvent: .heartbeat)
        case "signal.updated":
            if let jsonData = data.data(using: .utf8) {
                let decoder = JSONDecoder()
                decoder.dateDecodingStrategy = .iso8601
                if let payload = try? decoder.decode(SignalUpdatedEventPayload.self, from: jsonData) {
                    delegate?.sseClient(self, didReceiveEvent: .signalUpdated(payload))
                }
            }
        default:
            delegate?.sseClient(self, didReceiveEvent: .unknown(name, data))
        }
    }
}
