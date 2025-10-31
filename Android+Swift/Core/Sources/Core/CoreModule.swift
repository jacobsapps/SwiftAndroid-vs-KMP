import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
#if canImport(CSwiftJavaJNI)
import CSwiftJavaJNI
#endif

public struct RollerCoasterCategory: Codable, Hashable, Sendable {
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

    public init(
        slug: String,
        name: String,
        sourceURL: URL,
        construction: String,
        prebuiltDesigns: [String],
        image: URL,
        imageSource: URL
    ) {
        self.slug = slug
        self.name = name
        self.sourceURL = sourceURL
        self.construction = construction
        self.prebuiltDesigns = prebuiltDesigns
        self.image = image
        self.imageSource = imageSource
    }

    public func sourceURLString() -> String { sourceURL.absoluteString }
    public func imageURLString() -> String { image.absoluteString }
    public func imageSourceString() -> String { imageSource.absoluteString }
    public func prebuiltDesignCount() -> Int32 { Int32(clamping: prebuiltDesigns.count) }
    public func prebuiltDesign(at index: Int32) -> String { prebuiltDesigns[Int(index)] }
}

public struct RollerCoasterCatalog: Codable, Hashable, Sendable {
    public let categories: [RollerCoasterCategory]

    public init(categories: [RollerCoasterCategory]) {
        self.categories = categories
    }

    public func categoriesCount() -> Int32 {
        Int32(clamping: categories.count)
    }

    public func category(at index: Int32) -> RollerCoasterCategory {
        categories[Int(index)]
    }
}

public final class RollerCoasterCategoryHandle {
    private let category: RollerCoasterCategory

    public init(category: RollerCoasterCategory) {
        self.category = category
    }

    public func slug() -> String { category.slug }
    public func name() -> String { category.name }
    public func construction() -> String { category.construction }
    public func sourceURLString() -> String { category.sourceURL.absoluteString }
    public func imageURLString() -> String { category.image.absoluteString }
    public func imageSourceString() -> String { category.imageSource.absoluteString }
    public func prebuiltDesignCount() -> Int32 { Int32(clamping: category.prebuiltDesigns.count) }
    public func prebuiltDesign(at index: Int32) -> String { category.prebuiltDesigns[Int(index)] }
}

public final class RollerCoasterCatalogHandle {
    private let catalog: RollerCoasterCatalog

    public init(catalog: RollerCoasterCatalog) {
        self.catalog = catalog
    }

    public func count() -> Int32 {
        Int32(clamping: catalog.categories.count)
    }

    public func category(at index: Int32) -> RollerCoasterCategoryHandle {
        let category = catalog.categories[Int(index)]
        return RollerCoasterCategoryHandle(category: category)
    }
}

public enum RollerCoasterServiceError: Error {
    case invalidURL
    case invalidResponse
}

public final class RollerCoasterService {
    private let baseURLs: [URL]
    private let decoder: JSONDecoder
    private let session: URLSession

    public init(baseURL: String? = nil, session: URLSession = .shared) {
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
        self.session = session
    }

    // Async API for Swift callers
    public func fetchAllAsync() async throws -> RollerCoasterCatalog {
        try await request(path: "roller-coasters")
    }

    public func searchAsync(name: String) async throws -> RollerCoasterCatalog {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return try await fetchAllAsync()
        }
        return try await request(
            path: "roller-coasters/search",
            queryItems: [URLQueryItem(name: "name", value: trimmed)]
        )
    }

    // Synchronous API for JNI bridging
    public func fetchAll() -> RollerCoasterCatalog {
        fetchRemoteCatalogSync(path: "roller-coasters") ?? RollerCoasterCatalog(categories: [])
    }

    public func search(name: String) -> RollerCoasterCatalog {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return fetchAll()
        }
        if let catalog = fetchRemoteCatalogSync(
            path: "roller-coasters/search",
            queryItems: [URLQueryItem(name: "name", value: trimmed)]
        ) {
            return catalog
        }
        return RollerCoasterCatalog(categories: [])
    }

    public func fetchAllHandle() -> RollerCoasterCatalogHandle {
        RollerCoasterCatalogHandle(catalog: fetchAll())
    }

    public func searchHandle(name: String) -> RollerCoasterCatalogHandle {
        RollerCoasterCatalogHandle(catalog: search(name: name))
    }

    private func request(path: String, queryItems: [URLQueryItem] = []) async throws -> RollerCoasterCatalog {
        for base in baseURLs {
            var components = URLComponents(url: base, resolvingAgainstBaseURL: false)
            components?.path = "/" + path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            components?.queryItems = queryItems.isEmpty ? nil : queryItems
            guard let url = components?.url else { continue }
            var request = URLRequest(url: url)
            request.timeoutInterval = 15
            do {
                let (data, response) = try await session.data(for: request)
                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    continue
                }
                if let catalog = try? decoder.decode(RollerCoasterCatalog.self, from: data) {
                    return catalog
                }
            } catch {
                continue
            }
        }
        throw RollerCoasterServiceError.invalidResponse
    }

    private func fetchRemoteCatalogSync(path: String, queryItems: [URLQueryItem] = []) -> RollerCoasterCatalog? {
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
}

#if canImport(CSwiftJavaJNI)
extension RollerCoasterService {
    static var jniPlaceholderValue: jlong { 0 }
}

extension RollerCoasterCatalog {
    static var jniPlaceholderValue: jlong { 0 }
}
#endif
