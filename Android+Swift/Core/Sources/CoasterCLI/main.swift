import Core
import Foundation

@main
struct CoasterCLI {
    static func main() async {
        let service: RollerCoasterService
        do {
            service = try RollerCoasterService()
        } catch {
            printError("Failed to initialise service: \(error)")
            return
        }

        var lastMatches: [RollerCoasterCategory] = []

        print("Welcome to the RollerCoaster CLI.")
        print("Type a search term, press Return to list all, enter a number for details, or type 'quit' to exit.")

        while true {
            print("> ", terminator: "")
            guard let line = readLine() else {
                break
            }

            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.lowercased() == "quit" {
                break
            }

            if let index = Int(trimmed), index > 0, index <= lastMatches.count {
                printDetail(for: lastMatches[index - 1])
                continue
            }

            do {
                let catalog: RollerCoasterCatalog
                if trimmed.isEmpty {
                    catalog = try await service.fetchAll()
                } else {
                    catalog = try await service.search(name: trimmed)
                }
                lastMatches = catalog.categories
                displayMatches(lastMatches)
            } catch {
                printError("Error: \(error)")
            }
        }

        print("Goodbye.")
    }

    private static func displayMatches(_ matches: [RollerCoasterCategory]) {
        if matches.isEmpty {
            print("No matches.")
            return
        }

        for (index, category) in matches.enumerated() {
            print("\(index + 1). \(category.name)")
        }

        print("Enter a number for details or keep searching.")
    }

    private static func printDetail(for category: RollerCoasterCategory) {
        print(category.name)
        print("Slug: \(category.slug)")
        print("Construction: \(category.construction)")
        if !category.prebuiltDesigns.isEmpty {
            let joined = category.prebuiltDesigns.joined(separator: ", ")
            print("Roller Coasters: \(joined)")
        }
        print("Source: \(category.sourceURL.absoluteString)")
        print("Image Source: \(category.imageSource.absoluteString)")
    }

    private static func printError(_ message: String) {
        guard let data = (message + "\n").data(using: .utf8) else { return }
        try? FileHandle.standardError.write(contentsOf: data)
    }
}
