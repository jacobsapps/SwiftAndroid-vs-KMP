import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public struct RollerCoasterCategory: Codable, Hashable {
    public let slug: String
    public let name: String
    public let sourceURL: URL
    public let construction: String
    public let prebuiltDesigns: [String]
    public let image: URL
    public let imageSource: URL

    enum CodingKeys: String, CodingKey {
        case slug
        case name
        case sourceURL = "source_url"
        case construction
        case prebuiltDesigns = "prebuilt_designs"
        case image
        case imageSource = "image_source"
    }
}

public struct RollerCoasterCatalog: Codable, Hashable {
    public let categories: [RollerCoasterCategory]
}

public enum RollerCoasterServiceError: Error {
    case invalidURL
    case invalidResponse
}

public final class RollerCoasterService {
    private let baseURL: URL
    private let decoder: JSONDecoder

    public init(baseURL: String = "http://127.0.0.1:3000") throws {
        guard let url = URL(string: baseURL) else {
            throw RollerCoasterServiceError.invalidURL
        }
        self.baseURL = url
        self.decoder = JSONDecoder()
    }

    public func fetchAll() throws -> RollerCoasterCatalog {
        try request(path: "roller-coasters")
    }

    public func search(name: String) throws -> RollerCoasterCatalog {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return try fetchAll()
        }
        return try request(path: "roller-coasters/search", queryItems: [URLQueryItem(name: "name", value: trimmed)])
    }

    private func request(path: String, queryItems: [URLQueryItem] = []) throws -> RollerCoasterCatalog {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)
        components?.path = "/" + path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        components?.queryItems = queryItems.isEmpty ? nil : queryItems
        guard let url = components?.url else {
            throw RollerCoasterServiceError.invalidURL
        }

        let request = URLRequest(url: url, timeoutInterval: 15)
        let data = try URLSession.shared.syncData(for: request)
        return try decoder.decode(RollerCoasterCatalog.self, from: data)
    }
}

private extension URLSession {
    func syncData(for request: URLRequest) throws -> Data {
        let semaphore = DispatchSemaphore(value: 0)
        let box = ResultBox<Data>()

        let task = dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }
            if let error {
                box.store(.failure(error))
                return
            }
            guard let response = response as? HTTPURLResponse, response.statusCode == 200, let data else {
                box.store(.failure(RollerCoasterServiceError.invalidResponse))
                return
            }
            box.store(.success(data))
        }
        task.resume()
        semaphore.wait()
        return try box.value.get()
    }
}

private final class ResultBox<Value> {
    private var result: Result<Value, Error>?
    private let lock = NSLock()

    func store(_ value: Result<Value, Error>) {
        lock.lock()
        result = value
        lock.unlock()
    }

    var value: Result<Value, Error> {
        lock.lock()
        defer { lock.unlock() }
        return result ?? .failure(RollerCoasterServiceError.invalidResponse)
    }
}
