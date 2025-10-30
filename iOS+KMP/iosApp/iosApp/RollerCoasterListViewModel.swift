import Combine
import Foundation
import Shared

@MainActor
final class RollerCoasterListViewModel: ObservableObject {
    @Published var query: String = "" {
        didSet {
            scheduleSearch()
        }
    }
    @Published private(set) var items: [RollerCoasterItem] = []
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    private let service: RollerCoasterService
    private var searchTask: Task<Void, Never>?

    init(service: RollerCoasterService = RollerCoasterService.companion.createDefault()) {
        self.service = service
        load()
    }

    func load() {
        searchTask?.cancel()
        searchTask = Task { await fetchAll() }
    }

    private func scheduleSearch() {
        searchTask?.cancel()
        let currentQuery = query
        searchTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(0.3))
            await self?.runSearch(currentQuery)
        }
    }

    private func fetchAll() async {
        await perform { [weak self] in
            try await self?.service.fetchAll() ?? []
        }
    }

    private func runSearch(_ value: String) async {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            await fetchAll()
            return
        }
        await perform { [weak self] in
            try await self?.service.search(name: trimmed) ?? []
        }
    }

    private func perform(_ action: @escaping () async throws -> [RollerCoasterCategory]) async {
        isLoading = true
        errorMessage = nil
        do {
            let categories = try await action()
            items = categories.map(RollerCoasterItem.init)
        } catch {
            errorMessage = error.localizedDescription
            items = []
        }
        isLoading = false
    }
}

struct RollerCoasterItem: Identifiable, Hashable {
    let id: String
    let name: String
    let construction: String
    let prebuiltDesigns: [String]
    let sourceUrl: String
    let imageSource: String
    var imageURL: URL? { URL(string: imageSource) }

    init(category: RollerCoasterCategory) {
        id = category.slug
        name = category.name
        construction = category.construction
        prebuiltDesigns = category.prebuiltDesigns
        sourceUrl = category.sourceUrl
        imageSource = category.imageSource
    }
}
