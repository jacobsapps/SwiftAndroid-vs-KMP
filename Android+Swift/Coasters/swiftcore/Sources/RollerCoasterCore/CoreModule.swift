import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import CSwiftJavaJNI

public struct RollerCoasterCategory: Codable, Hashable, Sendable {
    public let slug: String
    public let name: String
    public let sourceURL: URL
    public let construction: String
    public let prebuiltDesigns: [String]
    public let imageSource: URL

    enum CodingKeys: String, CodingKey {
        case slug
        case name
        case sourceURL = "source_url"
        case construction
        case prebuiltDesigns = "prebuilt_designs"
        case imageSource = "image_source"
    }
}

public struct RollerCoasterCatalog: Codable, Hashable, Sendable {
    public let categories: [RollerCoasterCategory]
}

public final class RollerCoasterService {
    private let baseURLs: [URL]
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    public init(baseURL: String? = nil) {
        let envURL = ProcessInfo.processInfo.environment["ROLLER_COASTER_BASE_URL"]
        var candidates = [String]()
        if let baseURL, !baseURL.isEmpty {
            candidates.append(baseURL)
        } else if let envURL, !envURL.isEmpty {
            candidates.append(envURL)
        }
        candidates.append(contentsOf: [
            "http://10.0.2.2:3000",
            "http://127.0.0.1:3000"
        ])
        let resolved = candidates.compactMap(URL.init(string:))
        self.baseURLs = resolved.isEmpty
            ? [URL(string: "http://10.0.2.2:3000")!, URL(string: "http://127.0.0.1:3000")!]
            : resolved
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
    }

    public func fetchAll() -> RollerCoasterCatalog {
        fetchRemoteCatalog(path: "roller-coasters") ?? RollerCoasterCatalog(categories: [])
    }

    public func search(name: String) -> RollerCoasterCatalog {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return fetchAll()
        }
        if let catalog = fetchRemoteCatalog(
            path: "roller-coasters/search",
            queryItems: [URLQueryItem(name: "name", value: trimmed)]
        ) {
            return catalog
        }
        return RollerCoasterCatalog(categories: [])
    }

    public func fetchAllJSON() -> String {
        encode(fetchAll())
    }

    public func searchJSON(name: String) -> String {
        encode(search(name: name))
    }

    private func encode(_ catalog: RollerCoasterCatalog) -> String {
        guard let data = try? encoder.encode(catalog) else {
            return "{\"categories\":[]}"
        }
        return String(data: data, encoding: .utf8) ?? "{\"categories\":[]}"
    }
}

private extension RollerCoasterService {
    func fetchRemoteCatalog(path: String, queryItems: [URLQueryItem] = []) -> RollerCoasterCatalog? {
        for base in baseURLs {
            var components = URLComponents(url: base, resolvingAgainstBaseURL: false)
            components?.path = "/" + path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            components?.queryItems = queryItems.isEmpty ? nil : queryItems
            guard let url = components?.url else { continue }
            do {
                let data = try Data(contentsOf: url)
                if let catalog = try? decoder.decode(RollerCoasterCatalog.self, from: data) {
                    return catalog
                }
            } catch {
                continue
            }
        }
        return nil
    }


// MARK: - JNI placeholders

extension RollerCoasterService {
    static var jniPlaceholderValue: jlong { 0 }
}

extension RollerCoasterCatalog {
    static var jniPlaceholderValue: jlong { 0 }
}
